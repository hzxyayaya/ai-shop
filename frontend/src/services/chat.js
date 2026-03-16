import request from '../utils/request'

export function postChat(data) {
  return request({
    url: '/chat',
    method: 'post',
    data
  })
}

export async function postChatStream(data, handlers = {}) {
  const token = localStorage.getItem('token')
  const response = await fetch('/api/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(data)
  })

  if (response.status === 401) {
    localStorage.removeItem('token')
    if (window.location.pathname !== '/login') {
      window.location.href = '/login'
    }
    throw new Error('未登录或登录状态已失效')
  }

  if (!response.ok || !response.body) {
    throw new Error('流式请求失败')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  const emitBlock = block => {
    const lines = block
      .split('\n')
      .map(line => line.trim())

    const dataLines = lines
      .filter(line => line.startsWith('data:'))
      .map(line => line.slice(5).trim())
      .filter(Boolean)

    if (dataLines.length === 0) return

    try {
      const payload = JSON.parse(dataLines.join(''))
      handlers.onEvent?.(payload)
    } catch (error) {
      handlers.onError?.(error)
    }
  }

  while (true) {
    const { value, done } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const blocks = buffer.split('\n\n')
    buffer = blocks.pop() || ''
    blocks.forEach(emitBlock)
  }

  if (buffer.trim()) {
    emitBlock(buffer)
  }
}

export function deleteChatSession(sessionId) {
  return request({
    url: `/chat/${sessionId}`,
    method: 'delete'
  })
}
