// Admin service: user management and moderation actions (verify, suspend, activate).
import api from '../lib/api'

export const adminService = {
  getStats: () => api.get('/admin/stats'),
  getUsers: (params = {}) => api.get('/admin/users', { params }),
  getUser: (id) => api.get(`/admin/users/${id}`),
  verifyUser: (id) => api.put(`/admin/users/${id}/verify`),
  unverifyUser: (id) => api.put(`/admin/users/${id}/unverify`),
  suspendUser: (id) => api.put(`/admin/users/${id}/suspend`),
  activateUser: (id) => api.put(`/admin/users/${id}/activate`),
}

