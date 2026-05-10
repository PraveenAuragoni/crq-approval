import axios from 'axios'

const api = axios.create({
  baseURL: '/api/crq',
  headers: { 'Content-Type': 'application/json' },
})

export const getDashboard = () => api.get('/dashboard').then(r => r.data)

export const getAllCrqs = () => api.get('/list').then(r => r.data)

export const getLogs = () => api.get('/logs').then(r => r.data)

export const triggerAdhoc = (triggeredBy = 'UI_USER', fromDateTime = null, toDateTime = null) =>
  api.post('/adhoc', { triggeredBy, fromDateTime, toDateTime }).then(r => r.data)

export const runNow = () => api.post('/run-now').then(r => r.data)
