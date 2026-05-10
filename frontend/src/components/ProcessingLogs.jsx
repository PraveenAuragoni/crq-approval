import React, { useEffect, useState } from 'react'
import { getLogs } from '../services/api.js'

const fmt = dt => dt ? new Date(dt).toLocaleString() : '—'

export default function ProcessingLogs() {
  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await getLogs()
      setLogs(data)
    } catch {
      setError('Failed to load processing logs.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  return (
    <div className="card">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
        <div className="card-title" style={{ marginBottom: 0, borderBottom: 'none', paddingBottom: 0 }}>
          All Processing Logs
        </div>
        <button className="btn btn-outline" onClick={load}>🔄 Refresh</button>
      </div>

      {loading && <div className="spinner-wrap"><div className="spinner" /></div>}
      {error && <div className="alert alert-error">{error}</div>}
      {!loading && logs.length === 0 && <div className="empty-state">No processing logs yet.</div>}

      {!loading && logs.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>#</th>
                <th>Run At</th>
                <th>Type</th>
                <th>Total Read</th>
                <th>Approved</th>
                <th>Emails Sent</th>
                <th>Status</th>
                <th>Triggered By</th>
                <th>Error</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log, idx) => (
                <tr key={log.id}>
                  <td style={{ color: 'var(--gray-400)', fontSize: '0.82rem' }}>{idx + 1}</td>
                  <td style={{ fontSize: '0.82rem', whiteSpace: 'nowrap' }}>{fmt(log.runAt)}</td>
                  <td>
                    <span className={`badge ${log.batchType === 'SCHEDULED' ? 'badge-blue' : 'badge-orange'}`}>
                      {log.batchType}
                    </span>
                  </td>
                  <td>{log.totalCrqsRead}</td>
                  <td>{log.approvedCount}</td>
                  <td>{log.emailsSent}</td>
                  <td>
                    <span className={`badge ${
                      log.status === 'SUCCESS' ? 'badge-green'
                      : log.status === 'PARTIAL' ? 'badge-orange'
                      : 'badge-red'}`}>
                      {log.status}
                    </span>
                  </td>
                  <td>{log.triggeredBy || '—'}</td>
                  <td style={{ maxWidth: 200, fontSize: '0.8rem', color: 'var(--danger)' }}>
                    {log.errorMessage
                      ? <span title={log.errorMessage}>{log.errorMessage.substring(0, 60)}{log.errorMessage.length > 60 ? '...' : ''}</span>
                      : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div style={{ marginTop: 10, color: 'var(--gray-600)', fontSize: '0.82rem' }}>
            Total: {logs.length} runs
          </div>
        </div>
      )}
    </div>
  )
}
