# 测试指南（Testing）

## 后端测试（JUnit 5）

- 运行全部测试：
```bash
cd backend && mvn test
```
- 运行指定测试类：
```bash
mvn -Dtest=com.dati.BackendApplicationTests test
```
- 运行指定测试方法：
```bash
mvn -Dtest=com.dati.BackendApplicationTests#contextLoads test
```

### 新增单元测试（示例）
- 位置：`backend/src/test/java/<your package>/YourTest.java`
- 最小示例：
```java
package com.dati;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class YourTest { @Test void works() { assertTrue(true); } }
```
- 运行：
```bash
mvn -Dtest=com.dati.YourTest test
```

## 前端测试（建议）

- 当前未配置测试框架。若需前端单元测试，建议引入 Vitest：
```bash
npm i -D vitest @vitest/ui jsdom
```
- 在 `package.json` 增加脚本：
```json
{
  "scripts": {
    "test": "vitest",
    "test:ui": "vitest --ui"
  }
}
```
- 在 `src` 下添加 `*.test.ts`，运行：
```bash
npm run test
```

## 持续集成（建议）
- CI 可分两阶段：
  1) 后端构建与测试：`mvn -B test`
  2) 前端类型检查与构建：`npm ci && npm run build`
- 构建-only 步骤可使用：`mvn -B -DskipTests package`
