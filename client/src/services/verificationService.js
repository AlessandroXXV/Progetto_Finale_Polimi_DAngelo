// Verification service: submit identity documents and check verification status.
import api from '../lib/api'

export const submitVerificationDocuments = (frontDocument, backDocument) => {
  const form = new FormData()
  form.append('frontDocument', frontDocument)
  form.append('backDocument', backDocument)
  return api.post('/verification/documents', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const getVerificationStatus = () => api.get('/verification/status')


