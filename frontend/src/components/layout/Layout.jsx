import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import { useAuth } from '../../context/AuthContext';
import './Layout.css';

export default function Layout({ title }) {
  const { user } = useAuth();
  const initials = user?.email ? user.email.charAt(0).toUpperCase() : 'U';

  return (
    <div className="layout">
      <Sidebar />
      <div className="layout-main">
        <header className="layout-header">
          <span className="layout-header-title">{title || ''}</span>
          <div className="layout-header-user">
            <span>{user?.email}</span>
            <div className="layout-header-avatar">{initials}</div>
          </div>
        </header>
        <main className="layout-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
