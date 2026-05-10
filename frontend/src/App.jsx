import React, { useState } from 'react'
import Dashboard from './components/Dashboard.jsx'
import CrqTable from './components/CrqTable.jsx'
import AdHocApproval from './components/AdHocApproval.jsx'
import ProcessingLogs from './components/ProcessingLogs.jsx'
import './App.css'

const NAV_ITEMS = [
  { id: 'dashboard', label: 'Dashboard', icon: '📊' },
  { id: 'crqs', label: 'CRQ List', icon: '📋' },
  { id: 'adhoc', label: 'Ad-Hoc Approval', icon: '⚡' },
  { id: 'logs', label: 'Processing Logs', icon: '📜' },
]

export default function App() {
  const [active, setActive] = useState('dashboard')

  return (
    <div className="app-layout">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <span className="brand-icon">🔐</span>
          <span className="brand-name">CRQ Approval</span>
        </div>
        <nav className="sidebar-nav">
          {NAV_ITEMS.map(item => (
            <button
              key={item.id}
              className={`nav-item ${active === item.id ? 'active' : ''}`}
              onClick={() => setActive(item.id)}
            >
              <span className="nav-icon">{item.icon}</span>
              <span>{item.label}</span>
            </button>
          ))}
        </nav>
        <div className="sidebar-footer">
          <small>CRQ Automation System</small>
          <small>Scheduler: daily 5:30 PM</small>
        </div>
      </aside>

      <main className="main-content">
        <header className="top-bar">
          <h1 className="page-title">
            {NAV_ITEMS.find(n => n.id === active)?.icon}{' '}
            {NAV_ITEMS.find(n => n.id === active)?.label}
          </h1>
          <span className="top-bar-time">{new Date().toLocaleString()}</span>
        </header>

        <div className="content-area">
          {active === 'dashboard' && <Dashboard />}
          {active === 'crqs' && <CrqTable />}
          {active === 'adhoc' && <AdHocApproval />}
          {active === 'logs' && <ProcessingLogs />}
        </div>
      </main>
    </div>
  )
}
