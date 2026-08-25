# 学习资源预览功能 · 任务清单

> 详见 `plan.md`。`[后端]`=后端同事仓库，`[前端]`=本仓库可直接执行。

## Phase 0：契约与数据模型
- [ ] Task 0 [后端] 定稿契约 + resource_file 加 preview_path 字段

## Phase 1：PDF 预览（端到端切片）
- [ ] Task 1 [后端] PDF 30%+水印预览生成 + GET /resource/resources/{id}/preview
- [ ] Task 2 [前端] PreviewModal.vue + ResourcesView「预览」按钮

## Phase 2：图片预览
- [ ] Task 3 [后端] 图片水印预览（不截）
- [ ] Task 4 [前端] PreviewModal 支持 image

## Phase 3：压缩包预览入口选择（仅 zip）
- [ ] Task 5 [后端] POST /resource/archive/inspect 解压清单（rar 拒绝）
- [ ] Task 6 [后端] 上传接收 previewPath + 压缩包预览生成
- [ ] Task 7 [前端] 上传面板「选压缩包预览文件」+ zip 提示

## Phase 4：Office 转 PDF
- [ ] Task 8 [后端] Office 转 PDF（LibreOffice，复用 PDF 预览链路）

## 决策已定（无待确认项）
- 30% = ceil，写死；图片只加水印不截
- 仅 zip，rar 拒绝并文字提示
- Office 直接转 PDF，不做手动预览图兜底
- 预览对所有登录用户可用，复用现有登录守卫
