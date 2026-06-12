// Return URL handler for Stripe Connect onboarding.
// Stripe redirects here after the user completes (or exits) the onboarding flow.
import { useNavigate } from 'react-router-dom'
import { useEffect } from 'react'
import useAuthStore from '../../store/authStore'
import { paymentService } from '../../services/paymentService'
import { CheckCircle2 } from 'lucide-react'
import toast from 'react-hot-toast'

export default function StripeReturnPage() {
  const navigate = useNavigate()
  const { setStripeConnected } = useAuthStore()

  useEffect(() => {
    // `cancelled` prevents setState calls after unmount (e.g. React StrictMode double-invoke).
    let cancelled = false
    let timeoutId

    const finalize = async () => {
      try {
        const { data } = await paymentService.completeConnectAccount()
        if (!cancelled && data?.stripeConnected) {
          setStripeConnected(true)
        }
        toast.success('Onboarding Stripe completato!')
      } catch {
        toast.error('Impossibile confermare l\'onboarding Stripe')
      } finally {
        // Redirect to the Stripe dashboard after a short delay so the user sees the confirmation.
        if (!cancelled) {
          timeoutId = setTimeout(() => navigate('/student/stripe'), 2000)
        }
      }
    }

    finalize()
    return () => {
      cancelled = true
      if (timeoutId) clearTimeout(timeoutId)
    }
  }, [navigate, setStripeConnected])

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-emerald-50 to-teal-50">
      <div className="text-center max-w-md">
        <CheckCircle2 className="mx-auto mb-6 h-16 w-16 text-emerald-500" strokeWidth={1.5} />
        <h1 className="text-2xl font-bold text-gray-800 mb-3">Account Stripe Collegato!</h1>
        <p className="text-gray-500 mb-6">
          Il tuo account Stripe è stato configurato correttamente. Ora puoi ricevere pagamenti.
        </p>
        <p className="text-sm text-gray-400">Reindirizzamento in corso…</p>
      </div>
    </div>
  )
}
