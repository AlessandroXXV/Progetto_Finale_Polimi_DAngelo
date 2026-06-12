// Review service: create reviews and fetch them by client, student, or profile.
import api from '../lib/api'

export const reviewService = {
  create: (data) => api.post('/reviews', data),
  getByClient: (userId) => api.get(`/reviews/client/${userId}`),
  getByClientDetails: (userId) => api.get(`/reviews/client/${userId}/details`),
  getByStudent: (userId) => api.get(`/reviews/student/${userId}`),
  getByProfile: (profileId) => api.get(`/reviews/profile/${profileId}`),
  checkExists: (bookingId) => api.get(`/reviews/booking/${bookingId}/exists`),
}
