// Stripe Connect "refresh" URL handler.
// Stripe redirects here when the onboarding link expires; this page generates a fresh link and redirects immediately.
import { useState } from 'react'
import { paymentService } from '../../services/paymentService'
import { RefreshCw } from 'lucide-react'
import toast from 'react-hot-toast'

export default function StripeRefreshPage() {
  const [loading, setLoading] = useState(false)

  const handleRetry = async () => {
    setLoading(true)
    try {
      const refreshUrl = `${window.location.origin}/student/stripe/refresh`
      const returnUrl = `${window.location.origin}/student/stripe/return`
      const { data } = await paymentService.getAccountLink(refreshUrl, returnUrl)
      window.location.href = data.url
    } catch (err) {
      toast.error(err.response?.data?.message ?? 'Errore')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-amber-50 to-orange-50">
      <div className="text-center max-w-md">
        <RefreshCw className="mx-auto mb-6 h-16 w-16 text-amber-500" strokeWidth={1.5} />
        <h1 className="text-2xl font-bold text-gray-800 mb-3">Link Scaduto</h1>
        <p className="text-gray-500 mb-6">
          Il link di onboarding Stripe è scaduto. Clicca il pulsante per generarne uno nuovo.
        </p>
        <button
          onClick={handleRetry}
          disabled={loading}
          className="bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60 text-white font-semibold px-8 py-3 rounded-xl transition-colors"
        >
          {loading ? 'Generazione…' : 'Riprova Onboarding'}
        </button>
      </div>
    </div>
  )
}
