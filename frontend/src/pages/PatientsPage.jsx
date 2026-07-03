import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, Plus, MoreVertical, Trash2, Edit, X } from 'lucide-react';
import { patientService } from '../services/patientService';
import './PatientsPage.css';

export default function PatientsPage() {
  const navigate = useNavigate();
  const [patients, setPatients] = useState([]);
  const [query, setQuery] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({ name: '', email: '', dateOfBirth: '', address: '' });
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try {
      const res = query
        ? await patientService.search(query)
        : await patientService.getAll();
      setPatients(res.data);
    } catch { setPatients([]); }
    setLoading(false);
  };

  useEffect(() => { load(); }, []);

  const handleSearch = (e) => {
    e.preventDefault();
    load();
  };

  const openCreate = () => {
    setEditing(null);
    setForm({ name: '', email: '', dateOfBirth: '', address: '' });
    setShowModal(true);
  };

  const openEdit = (p) => {
    setEditing(p);
    setForm({ name: p.name, email: p.email, dateOfBirth: p.dateOfBirth, address: p.address || '' });
    setShowModal(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    try {
      if (editing) {
        await patientService.update(editing.id, form);
      } else {
        await patientService.create({ ...form, registeredDate: new Date().toISOString().split('T')[0] });
      }
      setShowModal(false);
      load();
    } catch (err) {
      alert(err.response?.data?.message || 'Error saving patient');
    }
  };

  const handleDelete = async (id) => {
    if (!confirm('Delete this patient?')) return;
    try {
      await patientService.delete(id);
      load();
    } catch (err) {
      alert('Error deleting patient');
    }
  };

  return (
    <div className="patients-page">
      <div className="page-header">
        <h2>Patients</h2>
        <button className="btn-primary" onClick={openCreate}>
          <Plus size={16} /> Add Patient
        </button>
      </div>

      <form className="search-bar" onSubmit={handleSearch}>
        <Search size={16} className="search-icon" />
        <input
          type="text"
          placeholder="Search by name or email..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
      </form>

      <div className="patients-table">
        <div className="table-header">
          <span>Name</span>
          <span>Status</span>
          <span>Room / Bed</span>
          <span>Email</span>
          <span>Date of Birth</span>
          <span>Address</span>
          <span></span>
        </div>
        {loading ? (
          <div className="empty-state">Loading...</div>
        ) : patients.length === 0 ? (
          <div className="empty-state">No patients found</div>
        ) : (
          patients.map((p) => (
            <div key={p.id} className="table-row clickable" onClick={() => navigate(`/patients/${p.id}`)}>
              <span className="text-heading">{p.name}</span>
              <span>
                <span className={`status-badge status-${(p.status || 'active').toLowerCase()}`}>{p.status || 'ACTIVE'}</span>
              </span>
              <span>{p.roomNumber ? `${p.roomNumber} - ${p.bedNumber || 'A'}` : '—'}</span>
              <span>{p.email}</span>
              <span>{p.dateOfBirth}</span>
              <span className="text-truncate">{p.address}</span>
              <span className="row-actions" onClick={e => e.stopPropagation()}>
                <button className="icon-btn" onClick={() => openEdit(p)} title="Edit">
                  <Edit size={14} />
                </button>
                <button className="icon-btn danger" onClick={() => handleDelete(p.id)} title="Delete">
                  <Trash2 size={14} />
                </button>
              </span>
            </div>
          ))
        )}
      </div>

      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{editing ? 'Edit Patient' : 'Add Patient'}</h3>
              <button className="icon-btn" onClick={() => setShowModal(false)}><X size={18} /></button>
            </div>
            <form onSubmit={handleSave} className="modal-form">
              <div className="form-group">
                <label>Full Name</label>
                <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
              </div>
              <div className="form-group">
                <label>Email</label>
                <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
              </div>
              <div className="form-group">
                <label>Date of Birth</label>
                <input type="date" value={form.dateOfBirth} onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })} required />
              </div>
              <div className="form-group">
                <label>Address</label>
                <input value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} required />
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                <button type="submit" className="btn-primary">{editing ? 'Save Changes' : 'Create Patient'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
