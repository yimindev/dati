#!/usr/bin/env python3
"""OpenAPI 按需查询工具 — 供 dati-ops 技能使用。

用法:
  openapi.py <path> [method]      # 查询端点定义,自动展开 $ref
  openapi.py --list               # 列出全部 path + method
  openapi.py --search <keyword>   # 按关键字搜索 path / operationId / tags

环境变量:
  OPENAPI_FILE   覆盖 openapi.json 路径(默认: 仓库根 docs/api/openapi.json)

设计原则:
  - 只输出请求所需的最小信息(参数 / 请求体 / 响应 schema),避免整文件读取浪费 token
  - $ref 递归展开,循环引用以 "$ref" 占位截断
"""
import argparse
import json
import os
import sys
from pathlib import Path

MAX_DEPTH = 8  # 展开深度上限,防止 schema 爆炸


def locate_spec() -> Path:
    env = os.environ.get("OPENAPI_FILE")
    if env:
        return Path(env).resolve()
    # 技能自包含: 只认技能目录下的 openapi.json
    skill_dir = Path(__file__).resolve().parents[1]  # scripts/ -> 技能目录 dati-ops/
    p = skill_dir / "openapi.json"
    if not p.exists():
        sys.exit(f"技能内置 openapi.json 不存在: {p}(由项目脚本 scripts/fetch-openapi.sh 同步)")
    return p


def load_spec(path: Path) -> dict:
    if not path.exists():
        sys.exit(f"openapi.json 不存在: {path}(可用 OPENAPI_FILE 环境变量覆盖)")
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def expand_schema(schema, schemas, depth=0):
    """递归展开 $ref;循环引用在 depth 超限或已访问时以 {"$ref": ...} 占位。"""
    if not isinstance(schema, dict):
        return schema
    ref = schema.get("$ref")
    if ref:
        name = ref.rsplit("/", 1)[-1]
        if depth >= MAX_DEPTH or name not in schemas:
            return {"$ref": ref}
        return expand_schema(schemas[name], schemas, depth + 1)
    out = {}
    for key, val in schema.items():
        if key == "properties" and isinstance(val, dict):
            out[key] = {
                k: expand_schema(v, schemas, depth + 1) for k, v in val.items()
            }
        elif key == "items" and isinstance(val, dict):
            out[key] = expand_schema(val, schemas, depth + 1)
        elif key in ("allOf", "anyOf", "oneOf") and isinstance(val, list):
            out[key] = [expand_schema(v, schemas, depth + 1) for v in val]
        elif isinstance(val, list):
            out[key] = [expand_schema(v, schemas, depth + 1) for v in val]
        else:
            out[key] = val
    return out


def fmt(obj) -> str:
    return json.dumps(obj, indent=2, ensure_ascii=False)


def list_paths(spec: dict):
    lines = []
    for path, item in spec.get("paths", {}).items():
        methods = ",".join(m.upper() for m in item if m in ("get", "post", "put", "delete", "patch"))
        op_ids = " | ".join(
            item[m].get("operationId", "") for m in ("get", "post", "put", "delete", "patch") if m in item
        )
        lines.append(f"{methods:<12} {path:<60} {op_ids}")
    print("\n".join(lines))


def show_operation(spec: dict, path: str, method: str):
    item = spec["paths"].get(path)
    if not item:
        sys.exit(f"path 不存在: {path}(用 --list 查看全部)")
    method = method.lower()
    op = item.get(method)
    if not op:
        sys.exit(f"{path} 不支持 {method.upper()}(支持: {', '.join(m.upper() for m in item if m in ('get','post','put','delete','patch'))})")

    schemas = spec.get("components", {}).get("schemas", {})
    out = {"path": path, "method": method.upper()}
    if op.get("operationId"):
        out["operationId"] = op["operationId"]
    if op.get("tags"):
        out["tags"] = op["tags"]
    if op.get("summary"):
        out["summary"] = op["summary"]

    params = op.get("parameters")
    if params:
        out["parameters"] = [
            {
                "name": p.get("name"),
                "in": p.get("in"),
                "required": p.get("required", False),
                "schema": expand_schema(p.get("schema", {}), schemas),
            }
            for p in params
        ]

    rb = op.get("requestBody", {}).get("content", {}).get("application/json")
    if rb:
        out["requestBody"] = {
            "required": op["requestBody"].get("required", False),
            "schema": expand_schema(rb.get("schema", {}), schemas),
        }

    responses = {}
    for code, resp in op.get("responses", {}).items():
        content = resp.get("content", {})
        schema = None
        # 优先 application/json,兜底 */*
        for ct in ("application/json", "*/*"):
            if ct in content and content[ct].get("schema"):
                schema = expand_schema(content[ct]["schema"], schemas)
                break
        responses[code] = {"description": resp.get("description", "")}
        if schema:
            responses[code]["schema"] = schema
    out["responses"] = responses

    print(fmt(out))


def collect_terms(op: dict, schemas: dict) -> str:
    """收集用于搜索的词汇: path + operationId + tags + 参数名 + 请求体字段名($ref 也解析)。"""
    parts = [op.get("operationId", ""), " ".join(op.get("tags", []))]
    for p in op.get("parameters", []):
        parts.append(p.get("name", ""))
    rb = op.get("requestBody", {}).get("content", {}).get("application/json", {})
    schema = rb.get("schema", {})
    if "$ref" in schema:
        name = schema["$ref"].rsplit("/", 1)[-1]
        schema = schemas.get(name, {})
    parts.extend(schema.get("properties", {}).keys())
    return " ".join(parts).lower()


def search(spec: dict, keyword: str):
    kw = keyword.lower()
    schemas = spec.get("components", {}).get("schemas", {})
    hits = []
    for path, item in spec.get("paths", {}).items():
        for m in ("get", "post", "put", "delete", "patch"):
            op = item.get(m)
            if not op:
                continue
            haystack = " ".join([path, m, collect_terms(op, schemas)])
            if kw in haystack:
                hits.append((path, m.upper(), op.get("operationId", "")))
    if not hits:
        print(f"未找到包含 '{keyword}' 的接口(提示: 搜索词使用英文, 如 password / tables / publish)")
        return
    for path, method, op_id in hits:
        print(f"{method:<7} {path:<60} {op_id}")


def main():
    parser = argparse.ArgumentParser(description="OpenAPI 按需查询工具(dati-ops)")
    parser.add_argument("path", nargs="?", help="接口路径,如 /v1/data-sources")
    parser.add_argument("method", nargs="?", help="HTTP 方法,如 POST")
    parser.add_argument("--list", action="store_true", help="列出全部 path + method")
    parser.add_argument("--search", metavar="KEYWORD", help="按关键字搜索接口")
    parser.add_argument("--file", help="指定 openapi.json 路径(默认取仓库 docs/api/openapi.json)")
    args = parser.parse_args()

    path = Path(args.file) if args.file else locate_spec()
    spec = load_spec(path)

    if args.list:
        list_paths(spec)
    elif args.search:
        search(spec, args.search)
    elif args.path:
        method = args.method or "GET"
        show_operation(spec, args.path, method)
    else:
        parser.print_help()


if __name__ == "__main__":
    main()
