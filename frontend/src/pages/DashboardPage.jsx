import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Users, TrendingUp, Activity } from 'lucide-react';
import { patientService } from '../services/patientService';
import { analyticsService } from '../services/analyticsService';
import './DashboardPage.css';

export default function DashboardPage() {
  const [stats, setStats] = useState({ totalPatients: 0, registrations: 0, recentEvents: [] });
  const [patients, setPatients] = useState([]);
  const navigate = useNavigate();

  useEffect(() => {
    Promise.allSettled([
      patientService.getAll(),
      analyticsService.getPatientCount(),
      analyticsService.getRecentEvents(),
    ]).then(([patientsRes, countRes, eventsRes]) => {
      const p = patientsRes.status === 'fulfilled' ? patientsRes.value.data : [];
      setPatients(p.slice(0, 5));
      setStats({
        totalPatients: p.length,
        registrations: countRes.status === 'fulfilled' ? countRes.value.data.totalPatients : 0,
        recentEvents: eventsRes.status === 'fulfilled' ? eventsRes.value.data.slice(0, 5) : [],
      });
    });
  }, []);

  return (
    <div className="dashboard">
      <h2>Overview</h2>

      <div className="stat-grid">
        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'var(--color-primary-light)', color: 'var(--color-primary)' }}>
            <Users size={20} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{stats.totalPatients}</span>
            <span className="stat-label">Total Patients</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'var(--bg-success)', color: 'var(--color-success)' }}>
            <TrendingUp size={20} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{stats.registrations}</span>
            <span className="stat-label">Event Count</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'var(--bg-warning)', color: 'var(--color-warning)' }}>
            <Activity size={20} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{stats.recentEvents.length}</span>
            <span className="stat-label">Recent Events</span>
          </div>
        </div>
      </div>

      <div className="dashboard-grid">
        <div className="dashboard-section">
          <div className="section-header">
            <h3>Recent Patients</h3>
            <button className="link-btn" onClick={() => navigate('/patients')}>View all</button>
          </div>
          <div className="simple-table">
            <div className="simple-table-header">
              <span>Name</span>
              <span>Email</span>
              <span>DOB</span>
            </div>
            {patients.map((p) => (
              <div key={p.id} className="simple-table-row">
                <span className="text-heading">{p.name}</span>
                <span>{p.email}</span>
                <span>{p.dateOfBirth}</span>
              </div>
            ))}
            {patients.length === 0 && <div className="empty-state">No patients yet</div>}
          </div>
        </div>

        <div className="dashboard-section">
          <div className="section-header">
            <h3>Recent Activity</h3>
          </div>
          <div className="activity-list">
            {stats.recentEvents.map((ev, i) => (
              <div key={i} className="activity-item">
                <div className="activity-dot" />
                <div className="activity-content">
                  <span className="text-heading">{ev.patientName || 'Unknown'}</span>
                  <span className="activity-type">{ev.eventType}</span>
                </div>
                <span className="activity-time">
                  {ev.timestamp ? new Date(ev.timestamp).toLocaleDateString() : ''}
                </span>
              </div>
            ))}
            {stats.recentEvents.length === 0 && <div className="empty-state">No recent activity</div>}
          </div>
        </div>
      </div>
    </div>
  );
}
