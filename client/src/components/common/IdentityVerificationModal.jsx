import { useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import { submitVerificationDocuments } from '../../services/verificationService'

const MAX_SIZE_MB = 3

export default function IdentityVerificationModal({ isOpen, onClose, onSuccess }) {
  const [frontDocument, setFrontDocument] = useState(null)
  const [backDocument, setBackDocument] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!isOpen) {
      setFrontDocument(null)
      setBackDocument(null)
      setLoading(false)
      setError('')
    }
  }, [isOpen])

  if (!isOpen) return null

  // The backend expects two PDF files, one for the front and one for the back of the document.
  const validatePdf = (file) => {
    if (!file) return 'Seleziona un file PDF'
    if (file.type !== 'application/pdf') return 'Sono ammessi solo file PDF'
    if (file.size > MAX_SIZE_MB * 1024 * 1024) return `File troppo grande (max ${MAX_SIZE_MB}MB)`
    return ''
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const frontError = validatePdf(frontDocument)
    const backError = validatePdf(backDocument)
    if (frontError || backError) {
      setError(frontError || backError)
      return
    }

    setLoading(true)
    setError('')
    try {
      const { data } = await submitVerificationDocuments(frontDocument, backDocument)
      toast.success(data.message ?? 'Documenti inviati')
      onSuccess?.(data)
      onClose?.()
    } catch (err) {
      setError(err.response?.data?.message ?? 'Errore invio documenti')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4">
      {/* Centered modal card with a simple vertical form layout. */}
      <div className="w-full max-w-xl rounded-2xl bg-white shadow-xl">
        <div className="flex items-center justify-between border-b border-gray-200 px-6 py-4">
          <div>
            <h2 className="text-lg font-semibold text-gray-800">Verifica identità</h2>
            <p className="text-sm text-gray-500">Carica fronte e retro del documento in PDF</p>
          </div>
          <button type="button" onClick={onClose} className="text-gray-400 hover:text-gray-600 text-2xl leading-none">×</button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4 px-6 py-5">
          <div>
            <label className="mb-2 block text-sm font-medium text-gray-700">Fronte documento</label>
            <input
              type="file"
              accept="application/pdf"
              onChange={(e) => setFrontDocument(e.target.files?.[0] ?? null)}
              className="block w-full text-sm text-gray-600 file:mr-4 file:rounded-lg file:border-0 file:bg-indigo-50 file:px-4 file:py-2 file:text-indigo-700"
            />
            {frontDocument && <p className="mt-1 text-xs text-gray-500">Selezionato: {frontDocument.name}</p>}
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium text-gray-700">Retro documento</label>
            <input
              type="file"
              accept="application/pdf"
              onChange={(e) => setBackDocument(e.target.files?.[0] ?? null)}
              className="block w-full text-sm text-gray-600 file:mr-4 file:rounded-lg file:border-0 file:bg-indigo-50 file:px-4 file:py-2 file:text-indigo-700"
            />
            {backDocument && <p className="mt-1 text-xs text-gray-500">Selezionato: {backDocument.name}</p>}
          </div>

          <p className="text-xs text-gray-500">Formati consentiti: PDF · Dimensione massima: {MAX_SIZE_MB}MB ciascuno.</p>
          {error && <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{error}</p>}

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} className="rounded-lg border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">
              Annulla
            </button>
            <button type="submit" disabled={loading} className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-60">
              {loading ? 'Invio in corso…' : 'Invia documenti'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

