# 学习资源模块 · 后端工作指引

> 目标：补齐「学习资源」后端表 + 接口，让管理员/老师端上传、学生/老师端浏览下载走真实后端，替换当前 localStorage 兜底。
> 前端已完成，本指引与前端约定**完全对齐**，照此实现即可，前端无需再改。

## 1. 现状与目标

- 前端已封装 API 契约：`web/src/api/common/resources.js`
- 前端运行时 `web/src/utils/resourceService.js`：**优先走 API，后端未就绪（404/500）时回退 localStorage**。
- 后端实现后，前端自动切到后端，localStorage 兜底自动失效，无需改前端。
- 目录种子数据已备好：`docs/spec/08-resource-catalog.sql`（教材 42 · 章节 251 · 小节 528，全数学）。

## 2. 数据表（4 张）

前 3 张（教材/章节/小节）DDL 见 `docs/spec/08-resource-catalog.sql`，这里给出第 4 张资源文件表：

```sql
CREATE TABLE IF NOT EXISTS resource_file (
  id BIGINT NOT NULL AUTO_INCREMENT,
  section_id BIGINT NOT NULL COMMENT '所属小节，关联 resource_section.id',
  subject VARCHAR(20) NOT NULL DEFAULT 'math',
  type VARCHAR(20) NOT NULL COMMENT '课件/学案/作业/试卷',
  year VARCHAR(10) DEFAULT NULL COMMENT '年份，如 2026',
  price INT NOT NULL DEFAULT 0 COMMENT '资源点：管理员端统一 200；老师端 100/300/500',
  title VARCHAR(200) DEFAULT NULL COMMENT '标题，缺省取文件名',
  file_name VARCHAR(255) NOT NULL,
  file_path VARCHAR(500) DEFAULT NULL COMMENT '服务端存储路径（相对 uploads/）',
  file_size BIGINT DEFAULT 0,
  author VARCHAR(50) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_rf_section (section_id),
  KEY idx_rf_subject (subject)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

表名说明：与知识点资源 `kp_resources`（按知识点挂资源）**不同**，本模块 `resource_*` 是「教材目录 + 按小节挂资源」。

## 3. 接口清单（对齐 resources.js）

统一前缀 `/api/v1`，响应 `{ code, message, data }`。**本模块所有 GET 返回 `data: [ ... ]` 扁平数组（暂不分页）**，不要返回 `{list,total}`，否则前端会拿到对象而非数组。

### 3.1 目录：教材 textbook

| 方法 | 路径 | 入参 | 返回 data |
|---|---|---|---|
| GET | `/resource/textbooks` | query `subject`（默认 math） | 教材数组 |
| POST | `/resource/textbooks` | `{ subject, name }` | 新建教材 `{ id, ... }` |
| DELETE | `/resource/textbooks/{id}` | — | 删除（级联删章节/小节） |

### 3.2 目录：章节 chapter

| 方法 | 路径 | 入参 | 返回 data |
|---|---|---|---|
| GET | `/resource/textbooks/{textbookId}/chapters` | — | 章节数组 |
| POST | `/resource/textbooks/{textbookId}/chapters` | `{ name }` | 新建章节 |
| DELETE | `/resource/chapters/{id}` | — | 删除（级联删小节） |

### 3.3 目录：小节 section

| 方法 | 路径 | 入参 | 返回 data |
|---|---|---|---|
| GET | `/resource/chapters/{chapterId}/sections` | — | 小节数组 |
| POST | `/resource/chapters/{chapterId}/sections` | `{ name }` | 新建小节 |
| DELETE | `/resource/sections/{id}` | — | 删除 |

### 3.4 资源文件 resource

| 方法 | 路径 | 入参 | 返回 data |
|---|---|---|---|
| GET | `/resource/resources` | query `sectionId`（必填）、`subject` | 该小节资源数组 |
| POST | `/resource/resources/upload` | multipart：`sectionId`、`subject`、`tag`(类型)、`year`、`price`、`files[]`(多文件) | 新资源数组 |
| DELETE | `/resource/resources/{id}` | — | 删除（含磁盘文件） |
| GET | `/resource/resources/{id}/download` | — | 文件流（`Content-Disposition` 附件） |

说明：`type` 与 `tag` 同义（前端上传时两者一致，值域 `课件/学案/作业/试卷`）。

## 4. 关键实现要点

- **目录节点排序**：返回时按 `sort_order` 升序。新建目录节点 `sort_order` 取「同父下最大 + 1」。
- **级联删除**：删教材 → 删其章节 → 删其小节 → 删关联资源文件；删章节/小节同理（注意物理删文件）。
- **价格**：`price` 由前端直接传入，后端**只存不校验/不改价**。规则在前端：管理员端固定 200，老师端三档（100/300/500）。
- **文件存储**：复用现有文件上传链路（`uploads/` 目录，参考 `kp_resources` 上传/下载的实现），`file_path` 存相对路径。
- **多文件批量**：`files[]` 一次可能多个，循环落盘 + 逐条入库，返回本次新建的资源数组（前端据此刷新列表）。

## 5. 权限（Sa-Token）

- 上传/删除目录与资源：`admin`(roleType 1) 与 `teacher`(3)。
- 浏览/下载：`admin`/`teacher`/`student`(4) 均可（登录即可）。
- 建议：写接口加 `@SaCheckLogin`，上传/删除额外校验角色 ∈ {1,3}。

## 6. 后端模块放置

放在 `eduai-system` 模块（现有学生/老师/题库/知识点/资源都在此），新增 controller/service/entity/repository 即可，无需新建模块。

## 7. 前端对接现状（后端无需关心，仅确认）

- `resourceService.js` 已「API 优先 + localStorage 兜底」；后端就绪后前端自动走后端。
- 资源列表 student/teacher 端（ResourcesView）读取 `eduai_learning_resources`，后端接管后由资源接口返回，前端列表字段兼容（`type/tag/year/price/fileName/title/fileSize`）。

## 8. 前端待补（后端完成后的一处小改动，非阻塞）

- `web/src/api/common/resources.js` 尚未封装下载地址 helper；`ResourcesView.vue` 的 `downloadRes` 目前兜底指向 `knowledge-points/resources/{id}/download`（知识点资源的下载），学习资源应改为 `/resource/resources/{id}/download`。
- 后端 `3.4` 的 download 接口上线后，需前端补一行 `getResourceDownloadUrl(id)` 并让 ResourcesView 走它。

## 9. 验收清单

- [ ] 执行 `08-resource-catalog.sql` 后，`resource_textbook/chapter/section` 三级数据可查。
- [ ] `GET /resource/textbooks?subject=math` 返回 42 条。
- [ ] 管理员 `/admin/resources` 选择学科→教材→章节→小节，批量上传后刷新列表可见，价格统一 200。
- [ ] 老师端「数学学科管理 → 学习资源」上传，价格可选 100/300/500，无学科选择。
- [ ] 学生端 `/student/subject/math/resources` 能列出后端资源并下载。
