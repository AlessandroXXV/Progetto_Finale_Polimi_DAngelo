import { useState } from 'react'
import { Info, Lock } from 'lucide-react'
import { useStripe, useElements, CardElement } from '@stripe/react-stripe-js'
import { bookingService } from '../../services/bookingService'
import toast from 'react-hot-toast'

// Visual style passed to Stripe's hosted CardElement iframe
const CARD_STYLE = {
  style: {
    base: {
      fontSize: '16px',
      color: '#1f2937',
      '::placeholder': { color: '#9ca3af' },
    },
    invalid: { color: '#ef4444' },
  },
}

// Renders a Stripe card payment form for an existing booking.
// Props:
//   clientSecret – PaymentIntent client secret returned by the backend
//   bookingId    – used to confirm the booking server-side after payment succeeds
//   onSuccess    – callback invoked when both Stripe and backend confirmation succeed
export default function PaymentForm({ clientSecret, bookingId, onSuccess }) {
  const stripe = useStripe()
  const elements = useElements()
  const [processing, setProcessing] = useState(false)
  const [cardError, setCardError] = useState(null)

  const handleSubmit = async (e) => {
    e.preventDefault()
    // Guard: Stripe.js and Elements must be fully loaded before submitting
    if (!stripe || !elements) return

    setProcessing(true)
    const cardElement = elements.getElement(CardElement)

    // Authorize the card; the backend will capture the charge only after the session ends
    const { error, paymentIntent } = await stripe.confirmCardPayment(clientSecret, {
      payment_method: { card: cardElement },
    })

    if (error) {
      setCardError(error.message)
      setProcessing(false)
      return
    }

    // Confirm booking on backend
    try {
      await bookingService.confirm(bookingId, paymentIntent.id)
      toast.success('Pagamento autorizzato! Prenotazione confermata.')
      onSuccess?.()
    } catch (err) {
      toast.error(err.response?.data?.message ?? 'Errore conferma prenotazione')
    } finally {
      setProcessing(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Dati Carta di Credito
        </label>
        {/* Stripe's CardElement is rendered inside an iframe; focus ring is applied on the wrapper */}
        <div className="border border-gray-300 rounded-lg px-4 py-3 focus-within:ring-2 focus-within:ring-indigo-500">
          <CardElement options={CARD_STYLE} onChange={(e) => setCardError(e.error?.message)} />
        </div>
        {cardError && <p className="text-red-500 text-sm mt-1">{cardError}</p>}
      </div>

      {/* Inform the user that this is an authorization-only charge, not an immediate capture */}
      <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 text-sm text-amber-800">
        <Info className="inline-block h-4 w-4 align-text-bottom" strokeWidth={1.5} /> Il pagamento verrà <strong>autorizzato</strong> ora, ma catturato solo a sessione completata.
      </div>

      <button
        type="submit"
        disabled={!stripe || processing}
        className="w-full flex items-center justify-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60 text-white font-semibold py-3 rounded-xl transition-colors"
      >
        {processing ? (
          'Elaborazione…'
        ) : (
          <>
            <Lock className="h-4 w-4" strokeWidth={1.5} />
            Paga ora
          </>
        )}
      </button>

      <p className="text-xs text-gray-400 text-center">
        Pagamento sicuro tramite Stripe · Dati card non vengono salvati
      </p>
    </form>
  )
}
