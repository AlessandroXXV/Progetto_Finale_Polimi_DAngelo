// Payment service: create Stripe PaymentIntents and manage Stripe Connect accounts/payouts.
import api from '../lib/api'

export const paymentService = {

  createIntent: (bookingId) => api.post('/payments/intent', { bookingId }),

  // Stripe Connect
  getAccountLink: (refreshUrl, returnUrl) => api.post('/payments/connect/account-link', { refreshUrl, returnUrl }),
  completeConnectAccount: () => api.post('/payments/connect/complete'),
  getBalance: () => api.get('/payments/connect/balance'),
  requestPayout: (amount, currency = 'eur') => api.post('/payments/connect/payout', { amount, currency }),
  getPayouts: (limit = 10) => api.get('/payments/connect/payouts', { params: { limit } }),
}
