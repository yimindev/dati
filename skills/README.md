# skills/

Repo-level canonical home for DatI agent skills. Skills are versioned with the repository and referenced (symlinked) from `.agents/skills/` so pi discovers them.

## Conventions

- One directory per skill, named `dati-<purpose>` (lowercase, hyphens)
- Each skill: `SKILL.md` (frontmatter + principles) + optional `references/` (heavy API docs, loaded on demand) + `scripts/` (helpers) + `examples/` (full flows)
- To make a skill usable in this repo: `ln -s ../../skills/<skill-name> .agents/skills/<skill-name>`
- To make a skill usable across projects: symlink it into `~/.agents/skills/`

## Skills

| Skill | Description |
|-------|-------------|
| `dati-api-ops` | Configure and operate DatI via its HTTP API (in progress) |
