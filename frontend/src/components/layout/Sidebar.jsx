import { NavLink, useNavigate } from 'react-router-dom';
import { LayoutDashboard, Users, Bot, BarChart3, LogOut } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import './Sidebar.css';

export default function Sidebar() {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const links = [
    { to: '/', icon: LayoutDashboard, label: 'Dashboard' },
    { to: '/patients', icon: Users, label: 'Patients' },
    { to: '/agent', icon: Bot, label: 'AI Assistant' },
    { to: '/analytics', icon: BarChart3, label: 'Analytics' },
  ];

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <div className="sidebar-brand-icon">
          <Bot size={18} />
        </div>
        <h1>MedSync</h1>
      </div>

      <nav className="sidebar-nav">
        {links.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
          >
            <Icon size={18} />
            {label}
          </NavLink>
        ))}
      </nav>

      <div className="sidebar-footer">
        <button className="sidebar-link" onClick={handleLogout}>
          <LogOut size={18} />
          Sign Out
        </button>
      </div>
    </aside>
  );
}
