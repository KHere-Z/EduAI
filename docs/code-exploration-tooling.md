# 后端代码探索工具分工（配置总结）

> 面向团队交接的说明文档。本仓库接入了两套代码导航工具，负责让 AI 助手在探索后端 Java 代码时更快、更准。前端同事如无需探索 Java 调用链，可不关注细节，只需了解「后端有两个索引工具、已配好、无需手动维护」。

---

## 一、结论先行

- 两套工具**互补、不冲突**，已按能力定死分工。
- **均随 Claude Code 自动启动**，重启无需手动操作。
- **索引持久化**，日常无需重建；只有换新仓库才需要一次性建索引。

---

## 二、两套工具是什么

| 维度 | jCodemunch | Codebase Memory (cbm) |
|---|---|---|
| 工具前缀 | `mcp__jcodemunch__*` | `mcp__codebase-memory-mcp__*` |
| 定位 | **「这个符号在哪」** — 符号搜索、定位、定义、文件结构 | **「这个符号串起谁」** — 调用链、多跳分析、影响面 |
| 索引 | 1324 符号 / 274 文件（名为 `KHere-Z/EduAI`） | 3649 节点 / 12803 边（名为 `EduAI-server`） |
| 强项 | 搜索快、带 signature/summary、91 个操作、runtime 热点分析 | 类型感知解析，能跨接口落到实现类（Java 链路更强） |
| 接入方式 | 项目级 `.mcp.json` + `enabledMcpjsonServers` | 用户级 `~/.claude.json` 的 `mcpServers` |

---

## 三、分工（写进全局 CLAUDE.md）

**jCodemunch → 定位**：符号搜索、定义、装饰器查询、文本/配置搜索、文件结构、runtime 热点。

**cbm → 连线**：`trace_path`（完整调用链）、`search_graph`（按关系找符号）、`get_code_snippet`（精确源码）、`query_graph`（Cypher 多跳）、`get_architecture`（架构总览）、`detect_changes`（改动影响面）、`index_status` / `check_index_coverage`（索引健康度）。

**经验法则**：符号的「身份与位置」是 jcodemunch 的问题；符号的「调用图与影响」是 cbm 的问题。追一条链（如 Controller → Service → Mapper）时，先用 jcodemunch `search_symbols` 定位入口，再用 cbm `trace_path` 走边。

---

## 四、为什么这样分工（实测依据）

同一条链 `TeacherController.listStudentWrongQuestions` 的对照结果：

| 维度 | cbm `trace_path` | jcodemunch `get_call_hierarchy` |
|---|---|---|
| 追踪深度 | 5 层 | 仅 1 层 |
| 识别到调用 Service | ✅ 落到具体接口方法 | ❌ 漏掉真实业务调用 |
| 继续往下追 | ✅ 接口 → 实现类 → 47 个 callee → 3 个 Mapper 方法 | ✗ 断在返回值类型 |

**原因**：Controller 那行真正调用的是 `questionBankService.listTeacherStudentWrongQuestions(...)`。cbm 用类型感知（type-aware）解析，认出了 `questionBankService` 是 `QuestionBankService` 并落到具体方法；jcodemunch 那条边是 `ast_inferred`，只抓到了返回值类型 `Result`，整条业务调用漏了。因此「连线」类任务归 cbm。

---

## 五、本次配置改动

1. **权限放行** — `dontAsk` 模式下，凡不在 allow 列表的工具会被静默拒绝。已在 `.claude/settings.local.json` 的 `permissions.allow` 增加 `mcp__codebase-memory-mcp`（顶层 + projectSpecific 各一处）。
2. **建 cbm 索引** — 本仓库首次索引完成：3649 节点 / 12803 边，0 文件跳过，22 个 parse_partial 均为 `.md` / `.sql`（不影响 Java 调用图）。
3. **更新全局策略** — `~/.claude/CLAUDE.md` 的「Code Exploration Policy」改写为双工具分工，并修正 negative-evidence 处理（先跨两个索引核对一次再下结论）。

---

## 六、Hook 布局（排查用）

| 事件 | matcher | 归属 |
|---|---|---|
| PreToolUse | Bash | rtk |
| PreToolUse | Read\|Grep | jcodemunch |
| PostToolUse | Edit\|Write | jcodemunch |
| PostToolUse | Read | cbm |
| SessionStart / SubagentStart | — | jcodemunch + cbm |

两套 hook 事件已错开，无共享 matcher 做互斥操作，运行时互不干扰。

---

## 七、日常维护

- **重启 Claude Code**：两个工具进程自动启动，无需手动操作。
- **换新仓库**：各建一次索引（一次性动作，之后持久）。
- **日常**：两个索引各自后台增量刷新，无需人工干预。
