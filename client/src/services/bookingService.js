// Booking service: CRUD for bookings, cancellation, session completion, and chat history.
import api from '../lib/api'

export const bookingService = {
  create: (data) => api.post('/bookings', data),
  confirm: (id, paymentIntentId) => api.post(`/bookings/${id}/confirm`, { paymentIntentId }),
  complete: (id, confirmationCode) => api.post(`/bookings/${id}/complete`, { confirmationCode }),
  cancel: (id) => api.post(`/bookings/${id}/cancel`),
  getById: (id) => api.get(`/bookings/${id}`),
  getMy: () => api.get('/bookings/my'),
  getProviderBookings: (providerUserId) => api.get(`/bookings/provider/${providerUserId}`),
  getByStatus: (status) => api.get(`/bookings/status/${status}`),
  getChatMessages: (id, params = {}) => api.get(`/bookings/${id}/messages`, { params }),
}
