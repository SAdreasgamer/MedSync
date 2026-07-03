import api from './api';

const PATIENTS_BASE = '/api/patients';

export const billingService = {
  getDetails: (patientId) => api.get(`${PATIENTS_BASE}/${patientId}/billing`),
  addInvoice: (patientId, description, amount) => 
    api.post(`${PATIENTS_BASE}/${patientId}/invoices`, null, { params: { description, amount } }),
  payInvoice: (invoiceId) => 
    api.put(`${PATIENTS_BASE}/invoices/${invoiceId}/pay`),
};
