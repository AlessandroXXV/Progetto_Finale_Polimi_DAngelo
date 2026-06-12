// Auth service: register and login endpoints.
import api from '../lib/api'

export const authService = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
}
