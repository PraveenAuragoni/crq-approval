import React, { useState } from 'react'
import { triggerAdhoc } from '../services/api.js'

const fmt = dt => dt ? new Date(dt).toLocaleString() : '—'

export default function AdHocApproval() {
  const [triggeredBy, setTriggeredBy] = useState('')
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)

  const handleRun = async () => {
    if (!triggeredBy.trim()) {
      setError('Please enter your name / user ID before triggering.')
      return
    }
    setLoading(true)
    setError(null)
    setResult(null)
    try {
      const data = await triggerAdhoc(triggeredBy.trim())
      setResult(data)
    } catch (e) {
      setError(e.response?.data?.message || 'Ad-hoc run failed. Check backend logs.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      {/* Explanation card */}
      <div className="card">
        <div className="card-title">Ad-Hoc CRQ Approval</div>
        <div className="alert alert-info" style={{ marginBottom: 0 }}>
          <strong>How it works:</strong> This run picks up CRQs that were added or updated in the OneDrive
          Excel <strong>after 5:30 PM today</strong> (post the scheduled run cutoff). It re-checks their
          status in Remedy and sends approval emails for any newly approved CRQs.
        </div>
      </div>

      {/* Trigger form */}
      <div className="card">
        <div className="card-title">Trigger Ad-Hoc Run</div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 14, maxWidth: 420 }}>
          <label style={{ fontWeight: 600, fontSize: '0.9rem' }}>
            Your Name / User ID
            <input
              type="text"
              placeholder="e.g. john.doe"
              value={triggeredBy}
              onChange={e => setTriggeredBy(e.target.value)}
              disabled={loading}
              style={{
                display: 'block',
                marginTop: 6,
                width: '100%',
                padding: '9px 14px',
                borderRadius: 7,
                border: '1px solid var(--gray-200)',
                fontSize: '0.92rem',
              }}
            />
          </label>

          <button
            className="btn btn-warning"
            onClick={handleRun}
            disabled={loading}
            style={{ width: 'fit-content' }}
          >
            {loading ? '⏳ Running...' : '⚡ Run Ad-Hoc Approval Now'}
          </button>

          <div style={{ fontSize: '0.82rem', color: 'var(--gray-600)' }}>
            Note: This reads the latest OneDrive Excel, filters CRQs updated after 5:30 PM,
            checks Remedy, and sends email for approved ones.
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
