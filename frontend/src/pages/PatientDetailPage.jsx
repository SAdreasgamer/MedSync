import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Edit, Trash2, User, Phone, Mail, MapPin, Calendar, Heart, Shield, BedDouble, Plus, DollarSign, LogOut } from 'lucide-react';
import { patientService } from '../services/patientService';
import { appointmentService } from '../services/appointmentService';
import { billingService } from '../services/billingService';
import './PatientDetailPage.css';

export default function PatientDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [patient, setPatient] = useState(null);
  const [appointments, setAppointments] = useState([]);
  const [billing, setBilling] = useState(null);
  const [activeTab, setActiveTab] = useState('overview');
  const [loading, setLoading] = useState(true);
  
  const [showAppointmentModal, setShowAppointmentModal] = useState(false);
  const [apptForm, setApptForm] = useState({ doctorName: '', department: '', appointmentDate: '', timeSlot: '', notes: '' });

  const [showBillingModal, setShowBillingModal] = useState(false);
  const [billingForm, setBillingForm] = useState({ description: '', amount: '' });

  const [showAdmitModal, setShowAdmitModal] = useState(false);
  const [admitForm, setAdmitForm] = useState({ roomNumber: '', bedNumber: '' });

  const [showEditPatientModal, setShowEditPatientModal] = useState(false);
  const [editPatientForm, setEditPatientForm] = useState({
    name: '',
    email: '',
    dateOfBirth: '',
    address: '',
    phone: '',
    gender: '',
    bloodGroup: '',
    emergencyContactName: '',
    emergencyContactPhone: '',
    notes: ''
  });

  useEffect(() => {
    loadPatient();
    loadAppointments();
    loadBilling();
  }, [id]);

  function addToRecentlyAccessed(patientData) {
    try {
      const stored = localStorage.getItem('recentlyAccessedPatients');
      let list = stored ? JSON.parse(stored) : [];
      list = list.filter(p => p.id !== patientData.id);
      list.unshift({
        id: patientData.id,
        name: patientData.name,
        email: patientData.email,
        dateOfBirth: patientData.dateOfBirth
      });
      list = list.slice(0, 5);
      localStorage.setItem('recentlyAccessedPatients', JSON.stringify(list));
    } catch (err) {
      console.error('Error saving recently accessed patient:', err);
    }
  }

  async function loadPatient() {
    try {
      const res = await patientService.getById(id);
      setPatient(res.data);
      addToRecentlyAccessed(res.data);
    } catch (e) { console.error(e); }
    finally { setLoading(false); }
  }

  async function loadAppointments() {
    try {
      const res = await appointmentService.getByPatient(id);
      setAppointments(res.data);
    } catch (e) { console.error(e); }
  }

  async function loadBilling() {
    try {
      const res = await billingService.getDetails(id);
      setBilling(res.data);
    } catch (e) { console.error(e); }
  }

  async function handleDelete() {
    if (!window.confirm('Are you sure you want to delete this patient?')) return;
    await patientService.delete(id);
    navigate('/patients');
  }

  async function handleSchedule(e) {
    e.preventDefault();
    await appointmentService.create({ ...apptForm, patientId: id });
    setShowAppointmentModal(false);
    setApptForm({ doctorName: '', department: '', appointmentDate: '', timeSlot: '', notes: '' });
    loadAppointments();
  }

  async function handleApptStatus(apptId, status) {
    await appointmentService.updateStatus(apptId, status);
    loadAppointments();
  }

  async function handleAddCharge(e) {
    e.preventDefault();
    try {
      await billingService.addInvoice(id, billingForm.description, parseFloat(billingForm.amount));
      setShowBillingModal(false);
      setBillingForm({ description: '', amount: '' });
      loadBilling();
    } catch (err) {
      alert(err.response?.data?.message || 'Error adding charge');
    }
  }

  async function handlePayInvoice(invoiceId) {
    try {
      await billingService.payInvoice(invoiceId);
      loadBilling();
    } catch (err) {
      alert('Error recording payment');
    }
  }

  async function handleAdmit(e) {
    e.preventDefault();
    try {
      await patientService.admit(id, admitForm.roomNumber, admitForm.bedNumber);
      setShowAdmitModal(false);
      setAdmitForm({ roomNumber: '', bedNumber: '' });
      loadPatient();
    } catch (err) {
      alert(err.response?.data?.message || 'Error admitting patient');
    }
  }

  async function handleDischarge() {
    if (!window.confirm('Are you sure you want to discharge this patient?')) return;
    try {
      await patientService.discharge(id);
      loadPatient();
    } catch (err) {
      alert('Error discharging patient');
    }
  }

  function openEditPatient() {
    setEditPatientForm({
      name: patient.name || '',
      email: patient.email || '',
      dateOfBirth: patient.dateOfBirth || '',
      address: patient.address || '',
      phone: patient.phone || '',
      gender: patient.gender || '',
      bloodGroup: patient.bloodGroup || '',
      emergencyContactName: patient.emergencyContactName || '',
      emergencyContactPhone: patient.emergencyContactPhone || '',
      notes: patient.notes || ''
    });
    setShowEditPatientModal(true);
  }

  async function handleEditPatientSave(e) {
    e.preventDefault();
    try {
      await patientService.update(id, editPatientForm);
      setShowEditPatientModal(false);
      loadPatient();
    } catch (err) {
      alert(err.response?.data?.message || 'Error updating patient');
    }
  }

  if (loading) return <div className="detail-loading">Loading...</div>;
  if (!patient) return <div className="detail-loading">Patient not found</div>;

  const statusClass = (patient.status || 'ACTIVE').toLowerCase();

  return (
    <div className="patient-detail">
      <div className="detail-topbar">
        <button className="btn-back" onClick={() => navigate('/patients')}>
          <ArrowLeft size={16} /> Back to Patients
        </button>
        <div className="detail-actions">
          {patient.status === 'ADMITTED' ? (
            <button className="btn-warning" onClick={handleDischarge}>
              <LogOut size={14} /> Discharge
            </button>
          ) : (
            <button className="btn-success" onClick={() => setShowAdmitModal(true)}>
              <BedDouble size={14} /> Admit
            </button>
          )}
          <button className="btn-secondary" onClick={openEditPatient}>
            <Edit size={14} /> Edit
          </button>
          <button className="btn-danger" onClick={handleDelete}>
            <Trash2 size={14} /> Delete
          </button>
        </div>
      </div>

      <div className="detail-header">
        <div className="detail-avatar">{patient.name?.charAt(0)}</div>
        <div className="detail-info">
          <h2>{patient.name}</h2>
          <div className="detail-meta">
            <span><Mail size={14} /> {patient.email}</span>
            {patient.phone && <span><Phone size={14} /> {patient.phone}</span>}
            {patient.gender && <span><User size={14} /> {patient.gender}</span>}
            {patient.bloodGroup && <span><Heart size={14} /> {patient.bloodGroup}</span>}
          </div>
        </div>
        <span className={`status-badge status-${statusClass}`}>{patient.status || 'ACTIVE'}</span>
      </div>

      <div className="detail-tabs">
        <button className={activeTab === 'overview' ? 'active' : ''} onClick={() => setActiveTab('overview')}>Overview</button>
        <button className={activeTab === 'appointments' ? 'active' : ''} onClick={() => setActiveTab('appointments')}>Appointments ({appointments.length})</button>
        <button className={activeTab === 'billing' ? 'active' : ''} onClick={() => setActiveTab('billing')}>Billing</button>
      </div>

      <div className="detail-content">
        {activeTab === 'overview' && (
          <div className="overview-grid">
            <div className="info-card">
              <h3>Personal Information</h3>
              <div className="info-rows">
                <div className="info-row"><span className="label">Date of Birth</span><span>{patient.dateOfBirth}</span></div>
                <div className="info-row"><span className="label">Address</span><span><MapPin size={14} /> {patient.address}</span></div>
                <div className="info-row"><span className="label">Registered</span><span><Calendar size={14} /> {patient.registeredDate}</span></div>
                {patient.gender && <div className="info-row"><span className="label">Gender</span><span>{patient.gender}</span></div>}
                {patient.bloodGroup && <div className="info-row"><span className="label">Blood Group</span><span>{patient.bloodGroup}</span></div>}
              </div>
            </div>

            {(patient.status === 'ADMITTED' || patient.roomNumber) && (
              <div className="info-card">
                <h3><BedDouble size={16} /> Admission Info</h3>
                <div className="info-rows">
                  {patient.roomNumber && <div className="info-row"><span className="label">Room</span><span>{patient.roomNumber}</span></div>}
                  {patient.bedNumber && <div className="info-row"><span className="label">Bed</span><span>{patient.bedNumber}</span></div>}
                  {patient.admissionDate && <div className="info-row"><span className="label">Admitted</span><span>{patient.admissionDate}</span></div>}
                </div>
              </div>
            )}

            {patient.status === 'DISCHARGED' && patient.dischargeDate && (
              <div className="info-card">
                <h3>Discharge Info</h3>
                <div className="info-rows">
                  <div className="info-row"><span className="label">Discharged</span><span>{patient.dischargeDate}</span></div>
                </div>
              </div>
            )}

            {(patient.emergencyContactName || patient.emergencyContactPhone) && (
              <div className="info-card">
                <h3><Shield size={16} /> Emergency Contact</h3>
                <div className="info-rows">
                  {patient.emergencyContactName && <div className="info-row"><span className="label">Name</span><span>{patient.emergencyContactName}</span></div>}
                  {patient.emergencyContactPhone && <div className="info-row"><span className="label">Phone</span><span>{patient.emergencyContactPhone}</span></div>}
                </div>
              </div>
            )}

            {patient.notes && (
              <div className="info-card full-width">
                <h3>Notes</h3>
                <p className="notes-text">{patient.notes}</p>
              </div>
            )}
          </div>
        )}

        {activeTab === 'appointments' && (
          <div className="appointments-tab">
            <div className="tab-header">
              <h3>Appointments</h3>
              <button className="btn-primary" onClick={() => setShowAppointmentModal(true)}>
                <Plus size={14} /> Schedule New
              </button>
            </div>
            {appointments.length === 0 ? (
              <p className="empty-text">No appointments found for this patient.</p>
            ) : (
              <div className="appt-list">
                {appointments.map(a => (
                  <div key={a.id} className="appt-card">
                    <div className="appt-top">
                      <div>
                        <strong>{a.doctorName}</strong>
                        <span className="appt-dept">{a.department}</span>
                      </div>
                      <span className={`status-badge status-${a.status?.toLowerCase()}`}>{a.status}</span>
                    </div>
                    <div className="appt-meta">
                      <span><Calendar size={14} /> {a.appointmentDate}</span>
                      <span>{a.timeSlot}</span>
                    </div>
                    {a.notes && <p className="appt-notes">{a.notes}</p>}
                    {a.status === 'SCHEDULED' && (
                      <div className="appt-actions">
                        <button className="btn-sm btn-complete" onClick={() => handleApptStatus(a.id, 'COMPLETED')}>Mark Completed</button>
                        <button className="btn-sm btn-cancel" onClick={() => handleApptStatus(a.id, 'CANCELLED')}>Cancel</button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {activeTab === 'billing' && (
          <div className="billing-tab">
            {billing ? (
              <div className="billing-grid">
                <div className="info-card">
                  <h3>Billing Account Overview</h3>
                  <div className="info-rows">
                    <div className="info-row"><span className="label">Account ID</span><span>{billing.accountId}</span></div>
                    <div className="info-row"><span className="label">Status</span><span className={`status-badge status-${billing.status?.toLowerCase()}`}>{billing.status}</span></div>
                    {billing.insuranceProvider && (
                      <div className="info-row"><span className="label">Insurance</span><span>{billing.insuranceProvider} ({billing.insurancePolicyNumber})</span></div>
                    )}
                  </div>
                </div>

                <div className="info-card">
                  <h3>Financial Summary</h3>
                  <div className="info-rows">
                    <div className="info-row"><span className="label">Total Billed</span><span>${billing.totalBilled.toFixed(2)}</span></div>
                    <div className="info-row"><span className="label">Total Paid</span><span>${billing.totalPaid.toFixed(2)}</span></div>
                    <div className="info-row" style={{ fontWeight: 'bold' }}>
                      <span className="label" style={{ color: 'var(--text-heading)' }}>Outstanding Balance</span>
                      <span style={{ color: billing.outstandingBalance > 0 ? '#DC2626' : '#166534' }}>
                        ${billing.outstandingBalance.toFixed(2)}
                      </span>
                    </div>
                  </div>
                </div>

                <div className="info-card full-width">
                  <div className="tab-header" style={{ marginBottom: '14px' }}>
                    <h3>Invoices</h3>
                    <button className="btn-primary" onClick={() => setShowBillingModal(true)}>
                      <Plus size={14} /> Add Charge
                    </button>
                  </div>
                  {billing.invoices.length === 0 ? (
                    <p className="empty-text">No invoices found for this patient.</p>
                  ) : (
                    <div className="invoice-list">
                      <div className="invoice-header">
                        <span>Date</span>
                        <span>Description</span>
                        <span>Amount</span>
                        <span>Status</span>
                        <span></span>
                      </div>
                      {billing.invoices.map(inv => (
                        <div key={inv.invoiceId} className="invoice-row">
                          <span>{inv.invoiceDate}</span>
                          <strong>{inv.description}</strong>
                          <span>${inv.amount.toFixed(2)}</span>
                          <span>
                            <span className={`status-badge status-${inv.status?.toLowerCase()}`}>{inv.status}</span>
                          </span>
                          <span>
                            {inv.status === 'PENDING' && (
                              <button className="btn-sm btn-complete" onClick={() => handlePayInvoice(inv.invoiceId)}>
                                Pay Charge
                              </button>
                            )}
                          </span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            ) : (
              <p className="empty-text">Loading billing account...</p>
            )}
          </div>
        )}
      </div>

      {showAppointmentModal && (
        <div className="modal-overlay" onClick={() => setShowAppointmentModal(false)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <h3>Schedule Appointment</h3>
            <form onSubmit={handleSchedule}>
              <div className="form-group">
                <label>Doctor Name</label>
                <input required value={apptForm.doctorName} onChange={e => setApptForm({...apptForm, doctorName: e.target.value})} placeholder="Dr. Mehta" />
              </div>
              <div className="form-group">
                <label>Department</label>
                <select required value={apptForm.department} onChange={e => setApptForm({...apptForm, department: e.target.value})}>
                  <option value="">Select department</option>
                  <option>General Medicine</option>
                  <option>Cardiology</option>
                  <option>Orthopedics</option>
                  <option>General Surgery</option>
                  <option>Dermatology</option>
                  <option>Pediatrics</option>
                  <option>Neurology</option>
                  <option>ENT</option>
                </select>
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label>Date</label>
                  <input type="date" required value={apptForm.appointmentDate} onChange={e => setApptForm({...apptForm, appointmentDate: e.target.value})} />
                </div>
                <div className="form-group">
                  <label>Time Slot</label>
                  <input required value={apptForm.timeSlot} onChange={e => setApptForm({...apptForm, timeSlot: e.target.value})} placeholder="10:00 AM - 10:30 AM" />
                </div>
              </div>
              <div className="form-group">
                <label>Notes</label>
                <textarea value={apptForm.notes} onChange={e => setApptForm({...apptForm, notes: e.target.value})} placeholder="Optional notes..." />
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setShowAppointmentModal(false)}>Cancel</button>
                <button type="submit" className="btn-primary">Schedule</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showBillingModal && (
        <div className="modal-overlay" onClick={() => setShowBillingModal(false)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <h3>Add Charge / Invoice</h3>
            <form onSubmit={handleAddCharge}>
              <div className="form-group">
                <label>Description</label>
                <input required value={billingForm.description} onChange={e => setBillingForm({...billingForm, description: e.target.value})} placeholder="e.g. Lab Work - CBC, Consultation Fee" />
              </div>
              <div className="form-group">
                <label>Amount ($)</label>
                <input type="number" step="0.01" required value={billingForm.amount} onChange={e => setBillingForm({...billingForm, amount: e.target.value})} placeholder="0.00" />
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setShowBillingModal(false)}>Cancel</button>
                <button type="submit" className="btn-primary">Add Charge</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showAdmitModal && (
        <div className="modal-overlay" onClick={() => setShowAdmitModal(false)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <h3>Admit Patient</h3>
            <form onSubmit={handleAdmit}>
              <div className="form-group">
                <label>Room Number</label>
                <input required value={admitForm.roomNumber} onChange={e => setAdmitForm({...admitForm, roomNumber: e.target.value})} placeholder="e.g. ICU-201, GEN-305" />
              </div>
              <div className="form-group">
                <label>Bed Number</label>
                <input required value={admitForm.bedNumber} onChange={e => setAdmitForm({...admitForm, bedNumber: e.target.value})} placeholder="e.g. A, B, C" />
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setShowAdmitModal(false)}>Cancel</button>
                <button type="submit" className="btn-primary">Admit Patient</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showEditPatientModal && (
        <div className="modal-overlay" onClick={() => setShowEditPatientModal(false)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <h3>Edit Patient Details</h3>
            <form onSubmit={handleEditPatientSave}>
              <div className="form-row">
                <div className="form-group">
                  <label>Full Name</label>
                  <input required value={editPatientForm.name} onChange={e => setEditPatientForm({...editPatientForm, name: e.target.value})} />
                </div>
                <div className="form-group">
                  <label>Email Address</label>
                  <input required type="email" value={editPatientForm.email} onChange={e => setEditPatientForm({...editPatientForm, email: e.target.value})} />
                </div>
              </div>
              
              <div className="form-row">
                <div className="form-group">
                  <label>Phone Number</label>
                  <input value={editPatientForm.phone} onChange={e => setEditPatientForm({...editPatientForm, phone: e.target.value})} placeholder="+91-XXXXXXXXXX" />
                </div>
                <div className="form-group">
                  <label>Date of Birth</label>
                  <input type="date" required value={editPatientForm.dateOfBirth} onChange={e => setEditPatientForm({...editPatientForm, dateOfBirth: e.target.value})} />
                </div>
              </div>

              <div className="form-group">
                <label>Address</label>
                <input required value={editPatientForm.address} onChange={e => setEditPatientForm({...editPatientForm, address: e.target.value})} />
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Gender</label>
                  <select value={editPatientForm.gender} onChange={e => setEditPatientForm({...editPatientForm, gender: e.target.value})}>
                    <option value="">Select Gender</option>
                    <option value="MALE">Male</option>
                    <option value="FEMALE">Female</option>
                    <option value="OTHER">Other</option>
                  </select>
                </div>
                <div className="form-group">
                  <label>Blood Group</label>
                  <input value={editPatientForm.bloodGroup} onChange={e => setEditPatientForm({...editPatientForm, bloodGroup: e.target.value})} placeholder="e.g. O+, A-" />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Emergency Contact Name</label>
                  <input value={editPatientForm.emergencyContactName} onChange={e => setEditPatientForm({...editPatientForm, emergencyContactName: e.target.value})} />
                </div>
                <div className="form-group">
                  <label>Emergency Contact Phone</label>
                  <input value={editPatientForm.emergencyContactPhone} onChange={e => setEditPatientForm({...editPatientForm, emergencyContactPhone: e.target.value})} />
                </div>
              </div>

              <div className="form-group">
                <label>Notes</label>
                <textarea value={editPatientForm.notes} onChange={e => setEditPatientForm({...editPatientForm, notes: e.target.value})} placeholder="Patient medical notes..." />
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setShowEditPatientModal(false)}>Cancel</button>
                <button type="submit" className="btn-primary">Save Changes</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
