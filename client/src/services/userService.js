// User service: account updates, profile image management, and verification status.
import api from '../lib/api'

export const userService = {
  updateMe: (data) => api.patch('/users/me', data),
  uploadProfileImage: (file) => {
    const form = new FormData()
    form.append('image', file)
    return api.post('/users/me/profile-image', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
  // `v` is a cache-buster query param; Cache-Control: no-cache forces revalidation with the server.
  getProfileImage: (userId, cacheBustKey) =>
    api.get(`/users/${userId}/profile-image`, {
      responseType: 'blob',
      params: cacheBustKey ? { v: cacheBustKey } : undefined,
      headers: { 'Cache-Control': 'no-cache' },
    }),
  deleteProfileImage: () => api.delete('/users/me/profile-image'),
  getVerificationStatus: () => api.get('/verification/status'),
}
