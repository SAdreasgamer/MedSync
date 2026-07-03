import api from './api';

const PATIENT_BASE = '/api/patients';

export const patientService = {
  getAll: () => api.get(PATIENT_BASE),
  getCounts: () => api.get(`${PATIENT_BASE}/count`),
  getBillingSummary: () => api.get(`${PATIENT_BASE}/billing/summary`),
  getById: (id) => api.get(`${PATIENT_BASE}/${id}`),
  search: (q) => api.get(`${PATIENT_BASE}/search`, { params: { q } }),
  create: (data) => api.post(PATIENT_BASE, data),
  update: (id, data) => api.put(`${PATIENT_BASE}/${id}`, data),
  delete: (id) => api.delete(`${PATIENT_BASE}/${id}`),
  admit: (id, roomNumber, bedNumber) => api.put(`${PATIENT_BASE}/${id}/admit`, null, { params: { roomNumber, bedNumber } }),
  discharge: (id) => api.put(`${PATIENT_BASE}/${id}/discharge`),
};
