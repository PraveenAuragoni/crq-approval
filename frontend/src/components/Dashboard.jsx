import React, { useEffect, useState, useCallback } from 'react'
import { getDashboard } from '../services/api.js'

const fmt = dt => dt ? new Date(dt).toLocaleString() : '—'

export default function Dashboard() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const d = await getDashboard()
      setData(d)
    } catch (e) {
      setError('Failed to load dashboard. Is the backend running?')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  if (loading) return <div className="spinner-wrap"><div className="spinner" /></div>
  if (error) return <div className="alert alert-error">{error}</div>

  const { totalToday, approvedToday, emailsSentToday, pendingToday,
          lastScheduledRun, lastAdhocRun, recentCrqs, recentLogs } = data

  return (
    <div>
      {/* Stat cards */}
      <div className="stat-grid">
        <div className="stat-card stat-blue">
          <span className="stat-icon">📋</span>
          <span className="stat-label">Total CRQs Today</span>
          <span className="stat-value">{totalToday}</span>
        </div>
        <div className="stat-card stat-green">
          <span className="stat-icon">✅</span>
          <span className="stat-label">Approved Today</span>
          <span className="stat-value">{approvedToday}</span>
        </div>
        <div className="stat-card stat-orange">
          <span className="stat-icon">📧</span>
          <span className="stat-label">Emails Sent</span>
          <span className="stat-value">{emailsSentToday}</span>
        </div>
        <div className="stat-card stat-red">
          <span className="stat-icon">⏳</span>
          <span className="stat-label">Pending / Not Approved</span>
          <span className="stat-value">{pendingToday}</span>
        </div>
      </div>

      {/* Last run info */}
      <div className="card">
        <div className="card-title">Last Run Info</div>
        <div style={{ display: 'flex', gap: 32, flexWrap: 'wrap' }}>
          <div>
            <div style={{ fontSize: '0.8rem', color: 'var(--gray-600)', marginBottom: 4 }}>SCHEDULED RUN</div>
            <div style={{ fontWeight: 600 }}>{fmt(lastScheduledRun)}</div>
          </div>
          <div>
            <div style={{ fontSize: '0.8rem', color: 'var(--gray-600)', marginBottom: 4 }}>AD-HOC RUN</div>
            <div style={{ fontWeight: 600 }}>{fmt(lastAdhocRun)}</div>
          </div>
          <button className="btn btn-outline" style={{ marginLeft: 'auto' }} onClick={load}>
            🔄 Refresh
          </button>
        </div>
      </div>

      {/* Today's CRQs table */}
      <div className="card">
        <div className="card-title">Today's CRQs</div>
        {recentCrqs?.length === 0
          ? <div className="empty-state">No CRQs processed today yet.</div>
          : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>CRQ Number</th>
                    <th>Title</th>
                    <th>Assignee</th>
                    <th>Remedy Status</th>
                    <th>Approved</th>
                    <th>Email Sent</th>
                    <th>Batch Type</th>
                    <th>Processed At</th>
                  </tr>
                </thead>
                <tbody>
                  {recentCrqs?.map(crq => (
                    <tr key={crq.id}>
                      <td><strong>{crq.crqNumber}</strong></td>
                      <td>{crq.title || '—'}</td>
                      <td>{crq.assignee || '—'}</td>
                      <td>
                        <span className={`badge ${crq.approved ? 'badge-green' : 'badge-orange'}`}>
                          {crq.remedyStatus || 'Unknown'}
                        </span>
                      </td>
                      <td>
                        {crq.approved
                          ? <span className="badge badge-green">Yes</span>
                          : <span className="badge badge-red">No</span>}
                      </td>
                      <td>
                        {crq.emailSent
                          ? <span className="badge badge-green">Sent ✓</span>
                          : <span className="badge badge-gray">Not Sent</span>}
                      </td>
                      <td>
                        <span className={`badge ${crq.batchType === 'SCHEDULED' ? 'badge-blue' : 'badge-orange'}`}>
                          {crq.batchType}
                        </span>
                      </td>
                      <td style={{ fontSize: '0.82rem' }}>{fmt(crq.processedAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
      </div>

      {/* Recent processing logs */}
      <div className="card">
        <div className="card-title">Recent Processing Logs</div>
        {recentLogs?.length === 0
          ? <div className="empty-state">No processing logs yet.</div>
          : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Run At</th>
                    <th>Type</th>
                    <th>Total Read</th>
                    <th>Approved</th>
                    <th>Emails Sent</th>
                    <th>Status</th>
                    <th>Triggered By</th>
                  </tr>
                </thead>
                <tbody>
                  {recentLogs?.map(log => (
                    <tr key={log.id}>
                      <td style={{ fontSize: '0.82rem' }}>{fmt(log.runAt)}</td>
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
                      <td>{log.triggeredBy}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
      </div>
    </div>
  )
}
