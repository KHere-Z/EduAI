# 学习资源预览 · 前端对接指引

> **状态**：后端已实现（分支 `dev/20260823`），接口契约与 `10-resource-preview-backend-guide.md` **完全一致**，前端按既有实现直接联调，无需改代码。
> **前置**：后端上线需执行 `docs/sql/migrate-resource-preview.sql`，并安装 LibreOffice（Office 预览依赖）。

---

## 接口清单

统一前缀 `/api/v1`，响应 `{ code, message, data }`。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/resource/resources/upload` | 上传（**扩展**：新增 `previewPaths` 字段） |
| POST | `/resource/archive/inspect` | 压缩包内部清单（**新增**） |
| GET | `/resource/resources/{id}/preview` | 预览元信息（**新增**） |
| GET | `/resource/resources/{id}/preview/file` | 预览文件流（**新增**） |

---

## 1. 上传（扩展）

`POST /resource/resources/upload`（multipart，原有字段不变，新增一个）：

| 字段 | 类型 | 说明 |
|---|---|---|
| `sectionId` | Long | 必填，所属小节 |
| `subject` | String | 学科（默认 math） |
| `tag` | String | 类型：课件/学案/作业/试卷 |
| `year` / `price` / `shared` | — | 原有字段 |
| `previewPaths` | String | **新增**，JSON 字符串数组，与 `files[]` 按索引对齐；每项为压缩包内预览入口文件路径（如 `课件/第1章.pdf`），非压缩包项为空串 `""` |
| `files[]` | File[] | 多文件 |

> 注意：`previewPaths` 只在 zip 文件上生效，非 zip 文件即便给了路径，后端也存 `NULL`。

---

## 2. 压缩包内部清单

`POST /resource/archive/inspect`（multipart，单文件字段 `file`）：

- **仅接受 `.zip`**，`.rar` 等其他格式返回业务错误（`code != 200`，`message` 提示「仅支持 zip 格式」）。
- 成功返回：

```json
{
  "code": 200,
  "data": {
    "files": [
      { "path": "课件/第1章.pdf", "name": "第1章.pdf", "size": 123456, "previewable": true },
      { "path": "readme.txt",       "name": "readme.txt",  "size": 120,     "previewable": false }
    ]
  }
}
```

`previewable` 表示内部文件是否可生成预览（`.pdf` / 图片 / Office 为 `true`）。

---

## 3. 预览元信息

`GET /resource/resources/{id}/preview`（需登录）：

```json
// 可预览 → 前端据 type 用 pdf.js 或 <img> 渲染
{ "code": 200, "data": { "type": "pdf",   "url": "/api/v1/resource/resources/{id}/preview/file" } }
{ "code": 200, "data": { "type": "image", "url": "/api/v1/resource/resources/{id}/preview/file" } }
// 无可预览内容
{ "code": 200, "data": { "type": "none" } }
```

`type` 取值：`pdf` | `image` | `none`（Office 转 PDF 后也是 `pdf`）。

---

## 4. 预览文件流

`GET /resource/resources/{id}/preview/file`（**需登录**）：

- `type=pdf` → `Content-Type: application/pdf`
- `type=image` → `Content-Type: image/png`
- 未生成/无预览内容时返回 404 或业务错误

---

## 联调注意点

1. **`preview/file` 必须带 `Authorization: Bearer <token>`**。前端 `PreviewModal` 用 `fetch` 拉取 `url` 时需手动带 token（该端点走登录校验，不放匿名静态目录）。
2. **`type` 分派**：`pdf` → pdf.js 渲染；`image` → `<img>`；`none` → 提示「暂无预览」。
3. **rar 拒绝**：`archive/inspect` 对非 zip 返回业务错误，前端已有 zip 提示文案即可。
4. **预览是「截断 + 水印」**：PDF 按「≤25 页取前 `ceil(总页×20%)` 页（最少 1 页）、>25 页取前 5 页」截页，每页带「智学AI」水印；图片只加水印不截断。原文件**无法**通过 preview 端点拿到。
5. **首次预览可能较慢**：预览文件首次请求时后端同步生成（PDF 截页+水印、Office 需 LibreOffice 转码），之后命中磁盘缓存。前端可对「预览加载中」给个 loading 态，不必特殊处理。
6. **下载不受影响**：`download` 仍返回完整原文件，走原有扣费/上传者逻辑，与预览完全隔离。

---

## 后端部署依赖（联调前需知会）

- 执行 `docs/sql/migrate-resource-preview.sql`（给 `resource_file` 加 `preview_path` 列）。
- 安装 LibreOffice：`sudo apt install libreoffice`，并确认 `soffice` 在 PATH（否则 Office 预览返回 500）。
