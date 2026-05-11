import React, { useEffect, useState } from 'react'
import { getAllCrqs } from '../services/api.js'

const fmt = dt => dt ? new Date(dt).toLocaleString() : '—'

export default function CrqTable() {
  const [crqs, setCrqs] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [filter, setFilter] = useState('ALL')
  const [search, setSearch] = useState('')

  const load = async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await getAllCrqs()
      setCrqs(data)
    } catch {
      setError('Failed to load CRQ list.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const filtered = crqs.filter(c => {
    const matchFilter =
      filter === 'ALL' ? true
      : filter === 'APPROVED' ? c.approved
      : filter === 'PENDING' ? !c.approved
      : filter === 'EMAIL_SENT' ? c.emailSent
      : true
    const matchSearch = search === '' ||
      c.crqNumber?.toLowerCase().includes(search.toLowerCase()) ||
      c.title?.toLowerCase().includes(search.toLowerCase()) ||
      c.application?.toLowerCase().includes(search.toLowerCase())
    return matchFilter && matchSearch
  })

  return (
    <div className="card">
      <div className="card-title">All CRQ Records</div>

      {/* Controls */}
      <div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap', alignItems: 'center' }}>
        <input
          type="text"
          placeholder="Search CRQ number, title, application..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          style={{
            padding: '8px 14px', borderRadius: 7, border: '1px solid var(--gray-200)',
            fontSize: '0.9rem', width: 280,
          }}
        />
        {['ALL', 'APPROVED', 'PENDING', 'EMAIL_SENT'].map(f => (
          <button
            key={f}
            className={`btn ${filter === f ? 'btn-primary' : 'btn-outline'}`}
            style={{ padding: '7px 14px', fontSize: '0.82rem' }}
            onClick={() => setFilter(f)}
          >
            {f.replace('_', ' ')}
          </button>
        ))}
        <button className="btn btn-outline" style={{ marginLeft: 'auto' }} onClick={load}>
          🔄 Refresh
        </button>
      </div>

      {loading && <div className="spinner-wrap"><div className="spinner" /></div>}
      {error && <div className="alert alert-error">{error}</div>}
      {!loading && filtered.length === 0 && <div className="empty-state">No CRQs match the filter.</div>}

      {!loading && filtered.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>CRQ Number</th>
                <th>Type</th>
                <th>Application</th>
                <th>Country</th>
                <th>Remedy Status</th>
                <th>Approved</th>
                <th>Email Sent</th>
                <th>Email Sent At</th>
                <th>Batch Type</th>
                <th>Date Group</th>
                <th>Processed At</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(crq => (
                <tr key={crq.id}>
                  <td><strong>{crq.crqNumber}</strong></td>
                  <td>{crq.title || '—'}</td>
                  <td>{crq.application || '—'}</td>
                  <td>{crq.country || '—'}</td>
                  <td>
                    <span className={`badge ${crq.approved ? 'badge-green' : 'badge-orange'}`}>
                      {crq.remedyStatus || 'Unknown'}
                    </span>
                  </td>
                  <td>{crq.approved ? <span className="badge badge-green">Yes</span> : <span className="badge badge-red">No</span>}</td>
                  <td>{crq.emailSent ? <span className="badge badge-green">Sent ✓</span> : <span className="badge badge-gray">No</span>}</td>
                  <td style={{ fontSize: '0.82rem' }}>{fmt(crq.emailSentAt)}</td>
                  <td>
                    <span className={`badge ${crq.batchType === 'SCHEDULED' ? 'badge-blue' : 'badge-orange'}`}>
                      {crq.batchType}
                    </span>
                  </td>
                  <td style={{ fontSize: '0.82rem' }}>{fmt(crq.lastUpdatedInExcel)}</td>
                  <td style={{ fontSize: '0.82rem' }}>{fmt(crq.processedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div style={{ marginTop: 10, color: 'var(--gray-600)', fontSize: '0.82rem' }}>
            Showing {filtered.length} of {crqs.length} records
          </div>
        </div>
      )}
    </div>
  )
}
