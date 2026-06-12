// Availability service: create/delete recurring weekly slots and fetch slot counts.
import api from '../lib/api'

export const availabilityService = {
  create: (data) => api.post('/availability/', data),
  delete: (slotId) => api.delete(`/availability/${slotId}`),
  getByStudent: (studentId) => api.get(`/availability/student/${studentId}`),
  getMyCount: () => api.get('/availability/me/count'),
}
