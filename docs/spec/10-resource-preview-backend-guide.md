# 学习资源预览功能 · 后端工作指引

> 目标：在学习资源模块上增加「30% 试看预览」。前端已完成，本指引与前端约定**完全对齐**，照此实现即可，前端无需再改。
> 前置：需先完成 `09-resource-backend-guide.md`（`resource_file` 表 + 上传/下载接口）。

## 1. 需求定稿（已与前端确认，无遗留项）

- **30% 口径**：`ceil(总页数 × 30%)`，常量写死、不可配置；单张图片**只加水印、不截断**。
- **压缩包仅 zip**：rar 不支持，后端直接拒绝。
- **Office 预览直接做**：LibreOffice 转 PDF，**不做**手动传预览图兜底。
- **预览权限**：所有登录用户可用（复用 `@SaCheckLogin`），下载完整版仍按现有扣费/上传者逻辑。
- **预览由服务端强制**：预览文件独立生成（截断+水印），与原始文件隔离，前端拿不到原文件。

## 2. 数据模型变更

`resource_file` 表新增一列：

```sql
ALTER TABLE resource_file
  ADD COLUMN preview_path VARCHAR(500) DEFAULT NULL
  COMMENT '压缩包内预览入口文件路径（非压缩包为 NULL，由文件自身类型实时判定预览能力）';
```

说明：**不需要** `preview_type` 列。预览能力（pdf/image/none）由文件类型 + 是否 zip 实时判定。

## 3. API 契约（对齐 `web/src/api/common/resources.js`）

统一前缀 `/api/v1`，响应 `{ code, message, data }`。

### 3.1 上传（扩展既有 upload）

`POST /resource/resources/upload`（multipart），在现有字段基础上新增：

| 字段 | 说明 |
|---|---|
| `previewPaths` | 可选，**JSON 字符串**，与 `files[]` 按索引对齐；每项为压缩包内预览入口文件路径（如 `课件/第1章.pdf`），非压缩包项为 `""` |

后端处理：
- 若存在 `previewPaths`，`JSON.parse` 成数组，逐文件写入 `resource_file.preview_path`（非 zip 的文件即便给了路径也存 NULL）。
- 若无该字段（非压缩包上传），`preview_path` 一律 NULL。

### 3.2 压缩包内部清单（新增）

`POST /resource/archive/inspect`（multipart，单文件字段 `file`）：

- 仅接受 `.zip`；`.rar` 等其他格式返回业务错误（`code != 200`，`message` 提示「仅支持 zip 格式」）。
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

`previewable` 判断：内部文件扩展名 ∈ 可预览集合（`.pdf` + 图片 `.png/.jpg/.jpeg/.gif` + Office `.doc/.docx/.ppt/.pptx/.xls/.xlsx`）时为 `true`。

### 3.3 预览（新增，双端点）

`GET /resource/resources/{id}/preview` — 返回预览元信息（`@SaCheckLogin`）：

```json
// 可预览：
{ "code": 200, "data": { "type": "pdf",   "url": "/api/v1/resource/resources/{id}/preview/file" } }
{ "code": 200, "data": { "type": "image", "url": "/api/v1/resource/resources/{id}/preview/file" } }
// 无可预览内容：
{ "code": 200, "data": { "type": "none" } }
```

`GET /resource/resources/{id}/preview/file` — 返回预览文件流（`@SaCheckLogin`）：

- `type=pdf` → `Content-Type: application/pdf`
- `type=image` → 对应 `image/png` / `image/jpeg`
- 未生成时 404 / 业务错误

前端 `PreviewModal` 会带 `Authorization: Bearer` 头 fetch `url`，故该文件端点必须走登录校验、且能被带鉴权的请求访问到（不要走匿名静态目录）。

## 4. 预览生成核心逻辑

统一流程 `generatePreview(resource)`，按文件类型分派；**首次生成后落盘缓存**，重复请求直接返回缓存。

### 4.1 PDF（截 30% + 水印）

- 库：Apache PDFBox（推荐，Java 侧成熟）。
- 截页数：`int n = Math.max(1, (int) Math.ceil(totalPages * 0.3))`；取前 `n` 页。
- 水印：每页盖半透明「预览」字样（斜向、居中或平铺均可，保证醒目且遮挡阅读）。
- 输出：新 PDF，落盘 `uploads/previews/{resourceId}.pdf`。

### 4.2 图片（只加水印，不截）

- 库：Java2D（`BufferedImage` + `Graphics2D`）。
- 在图片上叠加半透明「预览」水印（可平铺或居中大字），**不裁剪、不改尺寸**。
- 输出：`uploads/previews/{resourceId}.png`（或保留原格式）。

### 4.3 Office（转 PDF 后复用 4.1）

- 工具：LibreOffice headless：`soffice --headless --convert-to pdf --outdir <dir> <file>`。
- 转出 PDF 后走 4.1 的「30% + 水印」。
- 部署依赖：服务器需装 LibreOffice，并确认 `soffice` 在 PATH。

### 4.4 压缩包 zip（抽预览入口文件）

- 库：Apache Commons Compress（推荐，正确处理 UTF-8 中文文件名；`java.util.zip` 有中文乱码风险）。
- 逻辑：解压（**内存/临时目录**）→ 按 `preview_path` 定位内部文件 → 按该文件类型走 4.1 / 4.2 / 4.3。
- 安全：
    - 解压前校验总大小上限（如 ≤ 50MB），防止 zip 炸弹。
    - 校验条目路径，**防路径穿越**（`../`、绝对路径）。
    - `preview_path` 未命中 / 指向不可预览文件时，`preview` 返回 `{ type: "none" }`。

## 5. 安全要点

- **preview 与 download 隔离**：`download` 仍返回完整原文件（扣费/上传者校验）；`preview/file` 只返回截断+水印的预览文件，二者文件路径不重叠，杜绝「改 URL 拿原文件」。
- **预览文件不可外链**：`preview/file` 需登录；预览文件不放公开静态目录。
- **路径穿越 / zip 炸弹**：见 4.4，解压必须做大小与路径校验。

## 6. 依赖清单

| 用途 | 库 / 工具 |
|---|---|
| PDF 截页 + 水印 | Apache PDFBox |
| 图片水印 | JDK Java2D（无需额外依赖） |
| Office 转 PDF | LibreOffice（`soffice`，部署依赖） |
| zip 解压 | Apache Commons Compress |

## 7. 验收清单

- [ ] `resource_file` 已加 `preview_path` 列。
- [ ] 上传 PDF → `GET .../preview` 返回 `type=pdf`，`preview/file` 返回**页数 = ceil(总页×30%)、每页带「预览」水印**的 PDF。
- [ ] 上传图片 → `type=image`，带水印、**未被截断**。
- [ ] 上传 `.docx` → 经 LibreOffice 转 PDF 后返回 30% 水印预览。
- [ ] 上传 zip（含多文件）→ `archive/inspect` 返回内部清单与 `previewable`；`preview/file` 返回 `preview_path` 指向文件的预览。
- [ ] 上传 `.rar` → 上传或 inspect 明确拒绝（提示「仅支持 zip」）。
- [ ] `preview/file` 未登录返回 401；`download` 仍返回完整文件且正常扣费。
- [ ] 改 URL 无法拿到原文件（预览与下载文件路径不重叠）。
