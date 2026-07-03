import api from './api';

const ANALYTICS_BASE = '/api/analytics';

export const analyticsService = {
  getPatientCount: () => api.get(`${ANALYTICS_BASE}/patient-count`),
  getRegistrations: (period = 'month') => api.get(`${ANALYTICS_BASE}/registrations`, { params: { period } }),
  getRecentEvents: () => api.get(`${ANALYTICS_BASE}/recent-events`),
};
