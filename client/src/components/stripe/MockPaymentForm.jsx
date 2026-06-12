import { useState } from 'react'
import { Info, CreditCard } from 'lucide-react'
import { bookingService } from '../../services/bookingService'
import toast from 'react-hot-toast'

// Development-only payment form used when no real Stripe public key is configured.
// It skips the actual Stripe flow and calls the booking confirmation endpoint directly,
// passing clientSecret as a stand-in for the paymentIntentId.
// Props mirror PaymentForm so the two components are interchangeable at the call site.
export default function MockPaymentForm({ clientSecret, bookingId, onSuccess }) {
  const [processing, setProcessing] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setProcessing(true)

    setTimeout(async () => {
      try {
        // clientSecret is reused as a fake paymentIntentId understood by the backend in test mode
        await bookingService.confirm(bookingId, clientSecret)
        toast.success('[Mock] Pagamento autorizzato! Prenotazione confermata.')
        onSuccess?.()
      } catch (err) {
        toast.error(err.response?.data?.message ?? 'Errore conferma prenotazione')
        setProcessing(false)
      }
    }, 1500)
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 text-sm text-blue-800">
        <Info className="inline-block h-4 w-4 align-text-bottom" strokeWidth={1.5} /> <strong>Modalità Test Attiva:</strong> Nessuna chiave Stripe reale configurata.
        Clicca sul pulsante sottostante per simulare il pagamento e confermare la prenotazione.
      </div>

      <button
        type="submit"
        disabled={processing}
        className="w-full flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 disabled:opacity-60 text-white font-semibold py-3 rounded-xl transition-colors"
      >
        {processing ? (
          'Elaborazione finta in corso…'
        ) : (
          <>
            <CreditCard className="h-4 w-4" strokeWidth={1.5} />
            Simula Pagamento
          </>
        )}
      </button>

      <p className="text-xs text-gray-400 text-center">
        Bypass per test locali. Il backend riceverà la conferma simulata.
      </p>
    </form>
  )
}
