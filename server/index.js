import express from 'express'
import cors from 'cors'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'
import { v4 as uuidv4 } from 'uuid'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const app = express()
const PORT = process.env.PORT ? Number(process.env.PORT) : 8081

app.use(cors())
app.use(express.json())

const dataDir = path.join(__dirname, 'data')
const applicationsFile = path.join(dataDir, 'applications.json')
if (!fs.existsSync(dataDir)) fs.mkdirSync(dataDir, { recursive: true })
if (!fs.existsSync(applicationsFile)) fs.writeFileSync(applicationsFile, JSON.stringify({ applications: [] }, null, 2))

function load() {
  try { return JSON.parse(fs.readFileSync(applicationsFile, 'utf-8')) } catch { return { applications: [] } }
}
function save(store) {
  fs.writeFileSync(applicationsFile, JSON.stringify(store, null, 2))
}
function usernameFromReq(req) {
  const auth = req.headers['authorization'] || ''
  if (auth.startsWith('Bearer ')) return auth.substring(7).trim()
  return req.body?.user || req.query?.user || 'guest'
}

app.post('/api/applications', (req, res) => {
  const store = load()
  const user = usernameFromReq(req)
  // 一个用户只有一个最新申请条目
  const existing = store.applications.find(a => a.user === user && a.status !== 'rejected')
  if (existing && existing.status === 'submitted') {
    return res.json({ code: 200, data: { id: existing.id, status: existing.status }, message: 'ok' })
  }
  const id = uuidv4()
  const now = new Date().toISOString()
  const item = {
    id,
    user,
    payload: req.body || {},
    status: 'submitted',
    createdAt: now,
    updatedAt: now
  }
  store.applications.push(item)
  save(store)
  res.json({ code: 200, data: { id, status: 'submitted' }, message: 'ok' })
})

app.get('/api/applications', (req, res) => {
  const store = load()
  const { status, q } = req.query
  let list = store.applications
  if (status) list = list.filter(a => a.status === status)
  if (q) list = list.filter(a => a.user.includes(q) || (a.payload?.realName || '').includes(q))
  res.json({ code: 200, data: { list }, message: 'ok' })
})

app.get('/api/applications/me', (req, res) => {
  const store = load()
  const user = usernameFromReq(req)
  const item = store.applications.slice().reverse().find(a => a.user === user)
  res.json({ code: 200, data: { status: item?.status || 'none', application: item || null }, message: 'ok' })
})

app.put('/api/applications/:id/approve', (req, res) => {
  const store = load()
  const id = req.params.id
  const item = store.applications.find(a => a.id === id)
  if (!item) return res.status(404).json({ code: 404, message: 'not found' })
  item.status = 'approved'
  item.updatedAt = new Date().toISOString()
  save(store)
  res.json({ code: 200, data: true, message: 'ok' })
})

app.put('/api/applications/:id/reject', (req, res) => {
  const store = load()
  const id = req.params.id
  const item = store.applications.find(a => a.id === id)
  if (!item) return res.status(404).json({ code: 404, message: 'not found' })
  item.status = 'rejected'
  item.reason = (req.body && req.body.reason) || ''
  item.updatedAt = new Date().toISOString()
  save(store)
  res.json({ code: 200, data: true, message: 'ok' })
})

app.get('/api/counselor/schedule', (req, res) => {
  const store = load()
  const user = usernameFromReq(req)
  const item = store.applications.slice().reverse().find(a => a.user === user)
  if (item && item.status === 'approved') return res.json({ code: 200, data: { schedule: [] }, message: 'ok' })
  return res.status(403).json({ code: 403, message: 'not counselor' })
})

app.listen(PORT, () => {
  console.log(`Xinqiao API listening on http://localhost:${PORT}`)
})
