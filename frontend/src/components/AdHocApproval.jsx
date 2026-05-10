import React, { useState } from 'react'
import { triggerAdhoc } from '../services/api.js'

const fmt = dt => dt ? new Date(dt).toLocaleString() : '—'

// Returns "YYYY-MM-DDTHH:mm:ss" string for datetime-local input default values
function toInputValue(date) {
  const pad = n => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:00`
}

function defaultFrom() {
  const d = new Date()
  d.setHours(17, 30, 0, 0)
  return toInputValue(d)
}

function defaultTo() {
  const d = new Date()
  d.setHours(23, 59, 0, 0)
  return toInputValue(d)
}

export default function AdHocApproval() {
  const [triggeredBy, setTriggeredBy] = useState('')
  const [fromDateTime, setFromDateTime] = useState(defaultFrom)
  const [toDateTime, setToDateTime] = useState(defaultTo)
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)

  const handleRun = async () => {
    if (!triggeredBy.trim()) {
      setError('Please enter your name / user ID before triggering.')
      return
    }
    if (!fromDateTime || !toDateTime) {
      setError('Please select both From and To date-time.')
      return
    }
    if (new Date(fromDateTime) >= new Date(toDateTime)) {
      setError('"From" date-time must be before "To" date-time.')
      return
    }
    setLoading(true)
    setError(null)
    setResult(null)
    try {
      const data = await triggerAdhoc(triggeredBy.trim(), fromDateTime, toDateTime)
      setResult(data)
    } catch (e) {
      setError(e.response?.data?.message || 'Ad-hoc run failed. Check backend logs.')
    } finally {
      setLoading(false)
    }
  }

  const inputStyle = {
    display: 'block',
    marginTop: 6,
    width: '100%',
    padding: '9px 14px',
    borderRadius: 7,
    border: '1px solid var(--gray-200)',
    fontSize: '0.92rem',
  }

  return (
    <div>
      {/* Explanation card */}
      <div className="card">
        <div className="card-title">Ad-Hoc CRQ Approval</div>
        <div className="alert alert-info" style={{ marginBottom: 0 }}>
          <strong>How it works:</strong> Select a date-time range. The run picks up CRQs whose
          <em> Last Updated</em> timestamp in the OneDrive Excel falls within that range, checks
          their status in Remedy, and sends approval emails for any approved CRQs.
        </div>
      </div>

      {/* Trigger form */}
      <div className="card">
        <div className="card-title">Trigger Ad-Hoc Run</div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16, maxWidth: 480 }}>

          <label style={{ fontWeight: 600, fontSize: '0.9rem' }}>
            Your Name / User ID
            <input
              type="text"
              placeholder="e.g. john.doe"
              value={triggeredBy}
              onChange={e => setTriggeredBy(e.target.value)}
              disabled={loading}
              style={inputStyle}
            />
          </label>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <label style={{ fontWeight: 600, fontSize: '0.9rem' }}>
              From Date &amp; Time
              <input
                type="datetime-local"
                value={fromDateTime}
                onChange={e => setFromDateTime(e.target.value)}
                disabled={loading}
                style={inputStyle}
              />
            </label>

            <label style={{ fontWeight: 600, fontSize: '0.9rem' }}>
              To Date &amp; Time
              <input
                type="datetime-local"
                value={toDateTime}
                onChange={e => setToDateTime(e.target.value)}
                disabled={loading}
                style={inputStyle}
              />
            </label>
          </div>

          <button
            className="btn btn-warning"
            onClick={handleRun}
            disabled={loading}
            style={{ width: 'fit-content' }}
          >
            {loading ? '⏳ Running...' : '⚡ Run Ad-Hoc Approval'}
          </button>

          <div style={{ fontSize: '0.82rem', color: 'var(--gray-600)' }}>
            CRQs whose "Last Updated" in Excel falls between the selected range will be
            processed. Remedy status is checked and approval emails are sent for matching CRQs.
          </div>
        </div>
      </div>

      {/* Error */}
      {error && <div className="alert alert-error">{error}</div>}

      {/* Result */}
      {result && (
        <div className="card">
          <div className="card-title">Ad-Hoc Run Result</div>
          <div className="alert alert-success" style={{ marginBottom: 18 }}>
            Ad-hoc run completed successfully!
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: 16 }}>
            {[
              { label: 'Run At', value: fmt(result.runAt) },
              { label: 'Type', value: result.batchType },
              { label: 'Total CRQs Read', value: result.totalCrqsRead },
              { label: 'Approved', value: result.approvedCount },
              { label: 'Emails Sent', value: result.emailsSent },
              { label: 'Status', value: result.status },
              { label: 'Triggered By', value: result.triggeredBy },
            ].map(item => (
              <div key={item.label}
                style={{
                  background: 'var(--gray-100)',
                  borderRadius: 8,
                  padding: '12px 16px',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 4,
                }}>
                <div style={{ fontSize: '0.75rem', color: 'var(--gray-600)', fontWeight: 600, textTransform: 'uppercase' }}>
                  {item.label}
                </div>
                <div style={{ fontWeight: 700, fontSize: '1.05rem' }}>
                  {item.label === 'Status'
                    ? <span className={`badge ${item.value === 'SUCCESS' ? 'badge-green' : item.value === 'PARTIAL' ? 'badge-orange' : 'badge-red'}`}>
                        {item.value}
                      </span>
                    : item.value}
                </div>
              </div>
            ))}
          </div>

          {result.errorMessage && (
            <div className="alert alert-error" style={{ marginTop: 16 }}>
              <strong>Error:</strong> {result.errorMessage}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
