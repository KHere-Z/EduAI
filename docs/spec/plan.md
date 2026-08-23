# 学习资源预览功能 · 实施计划

> 状态：已评审，契约冻结。前端（本仓库）任务可直接执行，后端（同事仓库）任务作为协作指引。
> 前端利用 `resourceService.js` 的「API 优先 + localStorage 兜底」机制先行开发，后端就绪后自动切换，不阻塞。

## 概述

为学习资源增加「30% 试看预览」：
- PDF → 截前 30% 页（向上取整）+ 每页「预览」水印
- 图片 → 只加水印，不截
- Office（Word/PPT/Excel）→ LibreOffice 转 PDF，再走 PDF 预览链路
- 压缩包（仅 zip）→ 上传人指定内部「预览入口文件」
- 30% 与水印由**服务端强制**，前端只渲染，原文件不通过 preview 泄露

## 架构决策（已定稿）

1. **30% 口径**：`ceil(总页数 × 30%)`，写死常量，不可配置。单张图片只加水印不截。
2. **压缩包仅支持 zip**：rar 不支持，前端加文字提示「压缩包请上传 zip 格式」。
3. **Office 预览直接做**：LibreOffice headless 转 PDF，**不做**手动传预览图兜底（用户截图不可信）。
4. **预览权限**：所有登录用户可用，复用项目既有的「登录可见」守卫，不另做权限。
5. **预览生成服务端强制**：`preview` 与 `download` 双端点隔离，预览走「截断+水印」独立文件。
6. **schema 仅加一个字段** `preview_path`：压缩包内预览入口文件路径；非压缩包由文件自身类型实时判定预览能力，无需存储 `preview_type`。
7. **前端渲染**：PDF 用 `pdfjs-dist`（[package.json](web/package.json) 已装），图片用 `<img>`。

## API 契约（前后端共同，已冻结）

统一前缀 `/api/v1`，响应 `{ code, message, data }`。

### 上传（扩展）
`POST /resource/resources/upload`（multipart，现有 `sectionId/subject/tag/year/price/files[]` 基础上扩展）：
- `previewPaths`（可选，JSON 字符串，与 `files[]` 按索引对齐；每项为压缩包内预览入口文件路径如 `课件/第1章.pdf`，非压缩包项为空串 `""`）

### 压缩包内部清单（新增）
`POST /resource/archive/inspect`（multipart，单文件 `file`，仅 zip）：
- 返回 `data: { files: [{ path, name, size, previewable }] }`（`previewable` 表示内部文件是否可生成预览）

### 预览（新增）
`GET /resource/resources/{id}/preview`：
- 返回 `data: { type: 'pdf'|'image', url }`；无可预览内容时 `data: { type: 'none' }`
- 前端据 `type` 用 pdf.js 或 `<img>` 渲染

### 下载（保持不变）
`GET /resource/resources/{id}/download` 仍返回完整文件流，只对已付费/上传者开放。

## 任务清单

### Phase 0：契约与数据模型（协调项）

**Task 0（后端）：`resource_file` 加 `preview_path` 字段**
- 描述：`resource_file` 表加 `preview_path VARCHAR(500) DEFAULT NULL`，出 migration。
- 验收：字段落地，契约文档为唯一依据。
- 验证：`DESC resource_file` 可见该字段。
- 依赖：无
- 文件：后端 migration + `docs/spec/09-resource-backend-guide.md`（补 preview 接口）
- 规模：S

### Checkpoint 0：契约冻结
- [ ] 前后端确认字段名与 `preview` 返回结构

---

### Phase 1：PDF 预览（端到端切片，风险最高优先）

**Task 1（后端）：PDF 30% 水印预览生成 + 预览端点**
- 描述：对 PDF 截前 `ceil(页数×30%)` 页、每页盖「预览」水印，落盘独立 previews/ 路径；实现 `GET /resource/resources/{id}/preview` 返回 `{ type:'pdf', url }`。
- 验收：预览页数 = ceil(总页数×30%) 且每页带水印；原文件不可经 preview 拿到。
- 验证：`curl` preview 返回带水印 PDF；核对页数与水印。
- 依赖：Task 0
- 文件：后端 Controller/Service（preview 端点 + PDF 处理）
- 规模：M

**Task 2（前端）：PDF 预览 UI**
- 描述：`resources.js` 加 `getResourcePreviewUrl(id)`；新建 `PreviewModal.vue`（pdf.js 逐页 canvas）；`ResourcesView.vue` 列表加「预览」按钮。
- 验收：资源列表出现「预览」按钮；PDF 逐页预览且带水印；`type:'none'` 时提示「暂无预览」。
- 验证：`npm run build` 通过；手动点 PDF 预览正常。
- 依赖：Task 1（或后端未就绪走兜底不报错）
- 文件：`web/src/api/common/resources.js`、`web/src/components/PreviewModal.vue`、`web/src/views/student/subject/ResourcesView.vue`
- 规模：M

### Checkpoint 1：PDF 端到端
- [ ] PDF 上传 → 预览（30% 水印）→ 下载完整版，全链路可用

---

### Phase 2：图片预览

**Task 3（后端）：图片水印预览**
- 描述：图片（png/jpg 等）只加水印，`preview` 返回 `{ type:'image', url }`。
- 验收：图片预览返回 `type=image`，带水印，不截。
- 验证：curl 核对 content-type 与水印。
- 依赖：Task 0
- 文件：后端 Service（图片水印）
- 规模：S

**Task 4（前端）：PreviewModal 支持 image**
- 描述：`PreviewModal.vue` 根据 `type==='image'` 渲染 `<img>`。
- 验收：图片资源预览显示图片。
- 验证：手动点图片资源预览。
- 依赖：Task 2
- 文件：`web/src/components/PreviewModal.vue`
- 规模：XS

### Checkpoint 2：PDF + 图片
- [ ] 两种类型预览均正常

---

### Phase 3：压缩包预览入口选择（仅 zip）

**Task 5（后端）：解压清单接口**
- 描述：`POST /resource/archive/inspect` 解压 zip，返回内部文件清单（含 `previewable` 标记）；非 zip 返回错误。
- 验收：zip 返回 `{ files:[...] }`；rar 被拒绝。
- 验证：curl 上传 zip 返回清单、rar 报错。
- 依赖：Task 0
- 文件：后端 Service（zip 解压 + 类型识别）
- 规模：M

**Task 6（后端）：上传接收 previewPath + 压缩包预览生成**
- 描述：上传时存 `preview_path`；`preview` 对压缩包按 `preview_path` 抽内部文件，再走 PDF/图片预览生成。
- 验收：压缩包带 previewPath 上传后，preview 返回指定内部文件的 30% 水印预览。
- 验证：上传多文件 zip，指定某 PDF 为预览，preview 返回该 PDF 30% 水印预览。
- 依赖：Task 1、Task 3、Task 5
- 文件：后端 Service
- 规模：M

**Task 7（前端）：压缩包预览文件选择 UI + zip 提示**
- 描述：`ResourceUploadPanel.vue`：① 压缩包 accept 改为仅 `.zip`，提示文案加「压缩包请上传 zip 格式」；② 选 zip 后调用 `archive/inspect` 弹「选预览文件」清单（单选），选定后随上传带 `previewPath`。
- 验收：rar 不再可选；选 zip 后出现内部文件选择弹窗；payload 含 previewPath。
- 验证：手动上传 zip 选预览文件；rar 被拒并提示。
- 依赖：Task 2、Task 5（inspect）
- 文件：`web/src/api/common/resources.js`（加 `inspectArchive`）、`web/src/components/ResourceUploadPanel.vue`
- 规模：M

### Checkpoint 3：压缩包
- [ ] 上传 zip → 选内部预览文件 → 预览指定文件；rar 被拒

---

### Phase 4：Office 转 PDF

**Task 8（后端）：Office 转 PDF 预览**
- 描述：doc/docx/ppt/pptx/xls/xlsx 用 LibreOffice headless 转 PDF，再复用 Task 1 的 30% 水印预览。
- 验收：Office 文件能生成 30% 水印 PDF 预览。
- 验证：上传 .docx，preview 返回 30% 水印 PDF。
- 依赖：Task 1
- 文件：后端 Service（LibreOffice 转换）+ 部署依赖（soffice 环境）
- 规模：M（含环境依赖）

> 前端无额外改动：Office 预览返回 `type:'pdf'`，复用 Phase 1 的 PreviewModal。

### Checkpoint 4：完成
- [ ] PDF / 图片 / Office / zip 四类资源均能预览
- [ ] 30%（ceil）+ 水印服务端强制生效
- [ ] 下载完整版与预览互不串扰
- [ ] 预览对所有登录用户可用

---

## 风险与缓解

| 风险 | 影响 | 缓解 |
|---|---|---|
| 30% 仅前端隐藏可被绕过 | 高 | 预览独立文件由后端生成，preview/download 双端点隔离 |
| LibreOffice 部署环境缺失 | 中 | Task 8 隔离，部署时确认 soffice 可用 |
| 大文件解压/转码耗时 | 中 | 预览生成异步化 + 首次缓存；限制 zip 大小 |
| 后端未就绪导致前端空转 | 低 | `resourceService.js` localStorage 兜底 |

## 已确认决策（无遗留问题）
- 30% = ceil，写死，图片只加水印不截
- 仅 zip，rar 拒绝并文字提示
- Office 直接转 PDF，不做手动预览图
- 预览对所有登录用户可用，复用现有登录守卫
