import { useState, useEffect } from 'react';
import { Users, Calendar, Activity } from 'lucide-react';
import { analyticsService } from '../services/analyticsService';
import './AnalyticsPage.css';

export default function AnalyticsPage() {
  const [count, setCount] = useState(0);
  const [registrations, setRegistrations] = useState({ registrations: 0, periodDays: 30 });
  const [events, setEvents] = useState([]);

  useEffect(() => {
    Promise.allSettled([
      analyticsService.getPatientCount(),
      analyticsService.getRegistrations('month'),
      analyticsService.getRecentEvents(),
    ]).then(([c, r, e]) => {
      if (c.status === 'fulfilled') setCount(c.value.data.totalPatients);
      if (r.status === 'fulfilled') setRegistrations(r.value.data);
      if (e.status === 'fulfilled') setEvents(e.value.data);
    });
  }, []);

  return (
    <div className="analytics-page">
      <h2>Analytics</h2>

      <div className="analytics-stats">
        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'var(--color-primary-light)', color: 'var(--color-primary)' }}>
            <Users size={20} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{count}</span>
            <span className="stat-label">Total Events Tracked</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'var(--bg-success)', color: 'var(--color-success)' }}>
            <Calendar size={20} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{registrations.registrations}</span>
            <span className="stat-label">Registrations (Last {registrations.periodDays || 30} days)</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'var(--bg-warning)', color: 'var(--color-warning)' }}>
            <Activity size={20} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{events.length}</span>
            <span className="stat-label">Recent Events</span>
          </div>
        </div>
      </div>

      <div className="analytics-section">
        <h3>Event Log</h3>
        <div className="events-table">
          <div className="events-header">
            <span>Patient</span>
            <span>Email</span>
            <span>Event Type</span>
            <span>Timestamp</span>
          </div>
          {events.length === 0 ? (
            <div className="empty-state">No events recorded yet</div>
          ) : (
            events.map((ev, i) => (
              <div key={i} className="events-row">
                <span className="text-heading">{ev.patientName || '—'}</span>
                <span>{ev.patientEmail || '—'}</span>
                <span>
                  <span className={`event-badge ${ev.eventType?.toLowerCase().includes('created') ? 'success' : ev.eventType?.toLowerCase().includes('deleted') ? 'danger' : 'info'}`}>
                    {ev.eventType}
                  </span>
                </span>
                <span>{ev.timestamp ? new Date(ev.timestamp).toLocaleString() : '—'}</span>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
