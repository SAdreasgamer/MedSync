import api from './api';

const APPOINTMENT_BASE = '/api/appointments';

export const appointmentService = {
  getAll: () => api.get(APPOINTMENT_BASE),
  getById: (id) => api.get(`${APPOINTMENT_BASE}/${id}`),
  getByPatient: (patientId) => api.get(`${APPOINTMENT_BASE}/patient/${patientId}`),
  getByDate: (date) => api.get(`${APPOINTMENT_BASE}/date`, { params: { date } }),
  getToday: () => api.get(`${APPOINTMENT_BASE}/today`),
  countToday: () => api.get(`${APPOINTMENT_BASE}/count/today`),
  create: (data) => api.post(APPOINTMENT_BASE, data),
  update: (id, data) => api.put(`${APPOINTMENT_BASE}/${id}`, data),
  updateStatus: (id, status) => api.put(`${APPOINTMENT_BASE}/${id}/status`, null, { params: { status } }),
  delete: (id) => api.delete(`${APPOINTMENT_BASE}/${id}`),
};
