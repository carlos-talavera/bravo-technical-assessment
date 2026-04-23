const BASE = '/api/v1'

async function request(method, path, body) {
  const token = localStorage.getItem('token')
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body != null ? JSON.stringify(body) : undefined,
  })

  if (res.status === 401) {
    localStorage.removeItem('token')
    window.location.reload()
    return
  }

  if (!res.ok) {
    const data = await res.json().catch(() => ({}))
    throw new Error(data.message || `HTTP ${res.status}`)
  }

  if (res.status === 204) return null
  return res.json()
}

export const api = {
  login: (username, password) =>
    request('POST', '/auth/login', { username, password }),

  listApplications: (country, status) =>
    request('GET', `/applications?country=${country}&status=${status}`),

  createApplication: (data) =>
    request('POST', '/applications', data),

  updateStatus: (id, newStatus) =>
    request('PATCH', `/applications/${id}/status`, { newStatus }),
}

export function createEventSource(token) {
  return new EventSource(`${BASE}/applications/events?token=${encodeURIComponent(token)}`)
}
