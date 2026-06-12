// Profile service: CRUD for tutor service profiles.
// getAllAuthenticated returns extra fields (e.g. provider userId) that the public endpoint omits.
import api from '../lib/api'

export const profileService = {
  create: (data) => api.post('/profiles', data),
  getAll: (params = {}) => api.get('/profiles', { params }),
  getAllAuthenticated: (params = {}) => api.get('/profiles/authenticated', { params }),
  getMe: () => api.get('/profiles/me'),
  getById: (id) => api.get(`/profiles/${id}`),
  getDetailAuthenticated: (id) => api.get(`/profiles/${id}/detail`),
  getByStudent: (studentId) => api.get(`/profiles/student/${studentId}`),
  update: (id, data) => api.put(`/profiles/${id}`, data),
  delete: (id) => api.delete(`/profiles/${id}`),
  reactivate: (id) => api.patch(`/profiles/${id}/reactivate`),
}
