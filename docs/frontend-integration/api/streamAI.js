/**
 * AI 流式调用封装（fetch + ReadableStream）
 *
 * 后端流式端点（均为 POST，响应 text/event-stream）：
 *   - /ai/chat/stream           通用聊天
 *   - /ai/wrong-analysis/stream 错题分析
 *   - /ai/exam-analysis/stream  试卷分析
 *
 * 请求体与对应非流式端点完全一致（ChatRequest），模型由后端按模块路由，
 * 前端无需传 model / apiUrl / apiKey。
 *
 * 响应协议（详见 ai-streaming.md）：
 *   data: "<JSON编码的token>"\n\n   ← 内容块，JSON.parse 还原（可含换行/特殊字符）
 *   data: [DONE]\n\n               ← 正常结束
 *   data: [ERROR] <中文错误>\n\n    ← 出错
 *
 * 注意：不能用 axios（不支持 ReadableStream），也不能用 EventSource（端点需 POST + 请求体 + 鉴权头）。
 */

const BASE_URL = '/api/v1'

function getToken() {
  return localStorage.getItem('token')
}

/**
 * 通用流式调用
 *
 * @param {string} path    端点路径，如 '/ai/exam-analysis/stream'
 * @param {object} body    请求体（ChatRequest）
 * @param {object} handlers
 *   onChunk(token, fullText)  每收到一块内容回调（token=本次增量，fullText=累计全文）
 *   onDone(fullText)          正常结束
 *   onError(message)          出错（HTTP 错误 / 网络中断 / 后端 [ERROR]）
 *   signal                    AbortSignal，用于取消
 */
export async function streamAI(path, body, { onChunk, onDone, onError, signal } = {}) {
  const token = getToken()

  let resp
  try {
    resp = await fetch(`${BASE_URL}${path}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(body),
      signal,
    })
  } catch (e) {
    if (e.name === 'AbortError') throw e
    onError?.('网络异常，请检查网络连接')
    return
  }

  // 非 200：读完整响应体，尝试取后端 Result.message
  if (!resp.ok) {
    let msg = `请求失败 (HTTP ${resp.status})`
    try {
      const text = await resp.text()
      const json = JSON.parse(text)
      if (json && json.message) msg = json.message
    } catch (_) {
      /* ignore */
    }
    onError?.(msg)
    return
  }

  if (!resp.body) {
    onError?.('当前浏览器不支持流式读取')
    return
  }

  const reader = resp.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let fullText = ''

  const handleEvent = (rawEvent) => {
    // 去掉 \r，按 \n 切行，取所有 data: 行（SSE 规范：多行 data 用 \n 拼接）
    const dataLines = rawEvent
      .replace(/\r/g, '')
      .split('\n')
      .filter((l) => l.startsWith('data:'))
      .map((l) => l.slice(5).replace(/^ /, ''))
    if (dataLines.length === 0) return
    const payload = dataLines.join('\n')

    if (payload === '[DONE]') {
      onDone?.(fullText)
      return
    }
    if (payload.startsWith('[ERROR]')) {
      onError?.(payload.slice('[ERROR]'.length).trim() || 'AI 调用失败')
      return
    }
    try {
      const tokenText = JSON.parse(payload) // 还原换行/特殊字符
      fullText += tokenText
      onChunk?.(tokenText, fullText)
    } catch (_) {
      // 忽略无法解析的行（心跳/注释等）
    }
  }

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      let idx
      while ((idx = buffer.indexOf('\n\n')) !== -1) {
        const rawEvent = buffer.slice(0, idx)
        buffer = buffer.slice(idx + 2)
        handleEvent(rawEvent)
      }
    }
    // 处理末尾无 \n\n 结尾的残留
    if (buffer.trim()) handleEvent(buffer)
  } catch (e) {
    if (e.name === 'AbortError') throw e
    onError?.(fullText ? '连接已中断，已展示部分内容' : '连接已中断')
  }
}

// ---- 三个便捷封装 ----
export const streamChat = (body, handlers) => streamAI('/ai/chat/stream', body, handlers)
export const streamWrongAnalysis = (body, handlers) =>
  streamAI('/ai/wrong-analysis/stream', body, handlers)
export const streamExamAnalysis = (body, handlers) =>
  streamAI('/ai/exam-analysis/stream', body, handlers)
