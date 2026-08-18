# AI 流式输出 —— 前端对接指引

后端三个流式端点已就绪，本文档说明请求格式、响应协议和前端接线方式。

---

## 一、端点

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/ai/chat/stream` | 通用 AI 聊天（流式） |
| POST | `/api/v1/ai/wrong-analysis/stream` | 错题分析（流式，DeepSeek） |
| POST | `/api/v1/ai/exam-analysis/stream` | 试卷分析（流式，Doubao 视觉） |

- **请求体**：与对应非流式端点完全一致（`ChatRequest`）。
- **模型路由**：由后端按模块（`wrong_analysis` / `exam_analysis`）自动选择模型，**前端无需传 `model` / `apiUrl` / `apiKey`**。
- **鉴权**：需要登录，请求头 `Authorization: Bearer <token>`。
- **扣费**：流式与非流式一样扣积分（`pointService.consume` 在 controller 层执行）。
- **限流**：同一用户 1 分钟最多 10 次 AI 调用（`@RateLimit(limit=10, windowSec=60)`）。

---

## 二、请求体（ChatRequest）

```json
{
  "messages": [
    { "role": "user", "content": "请分析这张试卷" }
  ],
  "imageUrls": ["/uploads/ai/2026-08-17/xxx.png"],
  "systemPrompt": "选填，留空则由后端注入场景预设提示词"
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `messages` | array | 是 | 对话历史，`{ role, content }`，role 为 `user` / `assistant` |
| `imageUrl` | string | 否 | 单图 URL（兼容旧前端） |
| `imageUrls` | array | 否 | 多图 URL 列表（试卷分析传多张） |
| `systemPrompt` | string | 否 | 留空由后端注入场景预设提示词 |

图片建议先走 `/api/v1/ai/upload` 上传拿到 URL 再传入；也支持 base64 data URL（`data:image/png;base64,...`）。

---

## 三、响应协议（text/event-stream）

每个事件一行 `data: <payload>\n\n`，`payload` 是下面三种之一：

| payload | 含义 |
|---------|------|
| `"<JSON 编码的 token>"` | 内容块。**双引号包裹的 JSON 字符串**，前端 `JSON.parse` 还原（可含换行、特殊字符） |
| `[DONE]` | 正常结束 |
| `[ERROR] <中文错误信息>` | 出错 |

**示例原始字节流：**

```
data: "1. **扣分"
data: "分布**：\n函数题"
data: "扣 12 分\n\n2. ..."

data: [DONE]

```

### 关键点

1. **token 是 JSON 编码的**：后端用 `ObjectMapper.writeValueAsString(token)` 编码，把 markdown 里的换行 `\n`、引号等转义掉，保证一个 token 只占一行。前端必须 `JSON.parse(payload)` 还原，否则会丢失换行、变成一坨文本。
2. **用 `\n\n` 切分事件**，不是单行 `\n`。
3. **不能用 `EventSource`**：`EventSource` 只支持 GET、无法带请求体和 `Authorization` 头，而这三个端点是 POST。必须用 `fetch` + `ReadableStream`。
4. **不能用 axios**：axios 不暴露 `ReadableStream`。

---

## 四、前端接线（复制即用）

1. 复制 `frontend-integration/api/streamAI.js` 到前端项目 `src/api/streamAI.js`。
2. 在 Vue 组件里调用：

```vue
<script setup>
import { ref } from 'vue'
import { streamExamAnalysis } from '@/api/streamAI'

const output = ref('')     // 累计全文
const loading = ref(false)
const errorMsg = ref('')
const controller = ref(null)

async function runExamAnalysis() {
  output.value = ''
  errorMsg.value = ''
  loading.value = true

  // 取消控制（可选）：用户点「停止」时中断
  controller.value = new AbortController()

  await streamExamAnalysis(
    {
      messages: [{ role: 'user', content: '请分析这张试卷' }],
      imageUrls: ['/uploads/ai/2026-08-17/xxx.png'],
    },
    {
      signal: controller.value.signal,
      onChunk: (token, full) => { output.value = full },      // 每块增量刷新
      onDone: (full) => { output.value = full; loading.value = false },
      onError: (msg) => { errorMsg.value = msg; loading.value = false },
    }
  )
}

function stop() {
  controller.value?.abort()
  loading.value = false
}
</script>
```

### 渲染注意事项

后端返回的是 **markdown**（含 `**加粗**`、`1. 列表`、换行）。前端展示时：

- 用 markdown 渲染库（`marked` / `markdown-it`），或
- 至少给容器加 `white-space: pre-wrap`，让换行生效，否则会挤成一行。

---

## 五、curl 测试

`-N`（`--no-buffer`）关键，否则 curl 会等全部结束才显示：

```bash
TOKEN="<登录拿到的 token>"

curl -N -X POST http://localhost:8080/api/v1/ai/exam-analysis/stream \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"messages":[{"role":"user","content":"请分析这张试卷"}],"imageUrls":["/uploads/ai/2026-08-17/xxx.png"]}'
```

---

## 六、本地开发 & 上线

### 本地（Vite 代理）

现有 `vite.config.js` 的 `/api` 代理通常能直接透传 SSE。若发现流式在本地「卡住不输出」，检查代理是否缓冲 —— 一般无需额外配置即可用。

### 上线（nginx 反代）

nginx 默认会**缓冲响应 + 60 秒超时**，会掐断流式。需给 `/api` 加：

```nginx
location /api/ {
    proxy_pass http://backend:8080;
    proxy_http_version 1.1;
    proxy_set_header Connection "";

    proxy_buffering off;   # 关键①：关闭缓冲，让 SSE 逐块下发
    proxy_cache off;
    proxy_read_timeout 1200s;  # 关键②：20 分钟长连接
    proxy_send_timeout 1200s;
    gzip off;              # 关键③：SSE 不压缩
}
```

---

## 七、错误处理约定

| 场景 | 前端收到 |
|------|---------|
| 未登录 | HTTP 401（`resp.ok === false`，`onError` 收到「请先登录」） |
| 限流 | HTTP 429 或业务错误，`onError` 收到「AI 调用过于频繁…」 |
| AI 调用失败（如模型不支持图片） | `data: [ERROR] 题目图片识别失败…` |
| 网络中断 / 后端异常 | `data: [ERROR] …` 或 `onError` 收到「连接已中断」 |
| 正常结束 | `data: [DONE]` → `onDone(fullText)` |
