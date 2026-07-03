import api from './api';

const PATIENT_BASE = '/api/patients';

export const patientService = {
  getAll: () => api.get(PATIENT_BASE),
  getById: (id) => api.get(`${PATIENT_BASE}/${id}`),
  search: (q) => api.get(`${PATIENT_BASE}/search`, { params: { q } }),
  create: (data) => api.post(PATIENT_BASE, data),
  update: (id, data) => api.put(`${PATIENT_BASE}/${id}`, data),
  delete: (id) => api.delete(`${PATIENT_BASE}/${id}`),
};
