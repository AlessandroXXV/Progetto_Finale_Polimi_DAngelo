// Detail view for a single booking, shown from the tutor (provider) perspective.
// Supports completing a session via a client-supplied confirmation code,
// cancelling an active booking, and a real-time payment countdown.
// Chat is available once a booking is confirmed or completed.
import { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { bookingService } from '../../services/bookingService'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import Badge from '../../components/common/Badge'
import BookingChat from '../../components/chat/BookingChat'
import usePaymentCountdown from '../../hooks/usePaymentCountdown'
import toast from 'react-hot-toast'
import { ArrowLeft, CheckCircle2 } from 'lucide-react'

// Parse ISO date string as local midnight to avoid timezone-shifted display.
const formatBookingDate = (value) => {
  if (!value) return 'Non definita'
  return new Date(`${value}T00:00:00`).toLocaleDateString('it-IT', {
    weekday: 'long',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}

// Backend may return full HH:MM:SS strings; strip to HH:MM for display.
const formatSlotTime = (value) => {
  if (typeof value !== 'string') return '--:--'
  const match = value.match(/^(\d{2}:\d{2})/)
  return match ? match[1] : '--:--'
}

export default function StudentBookingDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  // 6-digit numeric code that the client shows to the tutor at session end.
  const [code, setCode] = useState('')
  const [completing, setCompleting] = useState(false)
  // Ref prevents the expiry handler from triggering multiple refetches.
  const hasTriggeredExpiryReload = useRef(false)

  const { data: booking, isLoading: loading, refetch: refetchBooking, error } = useQuery({
    queryKey: ['booking', id],
    queryFn: () => bookingService.getById(id).then((r) => r.data),
  })

  useEffect(() => {
    if (error) toast.error('Errore caricamento prenotazione')
  }, [error])

  // Hook that computes the remaining time for the client to complete payment.
  const paymentCountdown = usePaymentCountdown(booking)

  // When the payment window expires, trigger a single background refetch so the
  // UI reflects the automatic cancellation performed by the backend.
  useEffect(() => {
    if (!paymentCountdown.isVisible || !paymentCountdown.isExpired) {
      hasTriggeredExpiryReload.current = false
      return
    }
    if (hasTriggeredExpiryReload.current) return
    hasTriggeredExpiryReload.current = true
    refetchBooking()
  }, [paymentCountdown.isVisible, paymentCountdown.isExpired, refetchBooking])

  // Provider accept flow removed: backend migrates REQUESTED -> PAYMENT_PENDING

  // Completes the session by submitting the confirmation code received from the client.
  // On success the backend releases the held payment to the tutor.
  const handleComplete = async () => {
    if (!code.trim()) { toast.error('Inserisci il codice di conferma'); return }
    setCompleting(true)
    try {
      await bookingService.complete(booking.id, code)
      toast.success('Sessione completata!')
      queryClient.invalidateQueries({ queryKey: ['booking', id] })
      queryClient.invalidateQueries({ queryKey: ['bookings', 'my'] })
    } catch (err) {
      toast.error(err.response?.data?.message ?? 'Codice non valido')
    } finally {
      setCompleting(false)
    }
  }

  const handleCancel = async () => {
    if (!window.confirm('Annullare questa prenotazione?')) return
    try {
      await bookingService.cancel(booking.id)
      toast.success('Prenotazione annullata')
      queryClient.invalidateQueries({ queryKey: ['booking', id] })
      queryClient.invalidateQueries({ queryKey: ['bookings', 'my'] })
    } catch (err) {
      toast.error(err.response?.data?.message ?? 'Errore')
    }
  }

  if (loading) return <LoadingSpinner />
  if (!booking) return <p className="text-center text-gray-500 py-12">Prenotazione non trovata</p>

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <button onClick={() => navigate(-1)} className="inline-flex items-center gap-1 text-sm text-indigo-600 hover:underline">
        <ArrowLeft className="h-4 w-4" strokeWidth={1.5} /> Indietro
      </button>

      <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-8">
        <div className="flex items-start justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold text-gray-800 mb-1">{booking.profileTitle}</h1>
            <p className="text-sm text-gray-500">Prenotazione #{booking.id}</p>
          </div>
          <Badge value={booking.status} />
        </div>

        <div className="grid grid-cols-2 gap-4 text-sm mb-6">
          <div>
            <p className="text-gray-400 mb-0.5">Cliente</p>
            <p className="font-medium">@{booking.clientUsername}</p>
            <p className="text-xs text-gray-400">{booking.clientEmail}</p>
          </div>
          <div>
            <p className="text-gray-400 mb-0.5">Data / Orario</p>
            <p className="font-medium capitalize">{formatBookingDate(booking.bookingDate)}</p>
            <p className="text-xs text-gray-500">
              {formatSlotTime(booking.slotStartTime)} - {formatSlotTime(booking.slotEndTime)}
            </p>
          </div>
          <div>
            <p className="text-gray-400 mb-0.5">Importo</p>
            <p className="font-bold text-indigo-700 text-lg">€{parseFloat(booking.totalAmount).toFixed(2)}</p>
          </div>
        </div>

        {booking.notes && (
          <div className="bg-gray-50 rounded-lg p-4 mb-4">
            <p className="text-xs font-medium text-gray-500 mb-1">Note del Cliente</p>
            <p className="text-sm text-gray-700">{booking.notes}</p>
          </div>
        )}

        {/* Explain why the booking was cancelled if it was due to a payment timeout. */}
        {booking.status === 'CANCELLED' && booking.cancellationReason === 'PAYMENT_TIMEOUT' && (
          <div className="mb-4 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
            Prenotazione annullata automaticamente: il cliente non ha pagato entro 10 minuti.
            La stessa data e fascia oraria restano bloccate in modo permanente per quel cliente.
          </div>
        )}

        {/* Progress bar countdown while the client is expected to pay. */}
        {paymentCountdown.isVisible && (
          <div className="mb-4 rounded-xl border border-amber-300 bg-amber-50 p-4">
            <div className="mb-2 flex items-center justify-between text-sm">
              <span className="font-semibold text-amber-900">Tempo rimanente pagamento cliente</span>
              <span className="font-mono text-base font-bold text-amber-800">{paymentCountdown.formatted}</span>
            </div>
            <div className="h-2 w-full overflow-hidden rounded-full bg-amber-100">
              {/* Bar shrinks from full width to zero as time elapses. */}
              <div
                className="h-full bg-amber-500 transition-all"
                style={{ width: `${100 - paymentCountdown.progressPct}%` }}
              />
            </div>
            {paymentCountdown.isExpired && (
              <p className="mt-2 text-xs text-amber-800">Tempo scaduto. Aggiornamento stato in corso...</p>
            )}
          </div>
        )}

          {/* Student Actions */}
          <div className="space-y-4">
          {/* CONFIRMED: enter client's code to release payment */}
          {booking.status === 'CONFIRMED' && (
            <div className="border-2 border-dashed border-indigo-300 rounded-xl p-5 bg-indigo-50">
              <p className="flex items-start gap-2 text-sm font-medium text-indigo-700 mb-3">
                <CheckCircle2 className="h-4 w-4 shrink-0 mt-0.5" strokeWidth={1.5} /> Sessione confermata — chiedi il codice al cliente e inseriscilo per completare la sessione e ricevere il pagamento.
              </p>
              <div className="flex gap-3">
                {/* Only digits are accepted; input is capped at 6 characters. */}
                <input
                  type="text"
                  inputMode="numeric"
                  value={code}
                  onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                  placeholder="000000"
                  maxLength={6}
                  className="flex-1 border border-indigo-300 rounded-lg px-4 py-2.5 text-center font-mono text-2xl tracking-widest focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                />
                <button
                  onClick={handleComplete}
                  disabled={completing || code.length !== 6}
                  className="inline-flex items-center gap-1.5 bg-green-600 hover:bg-green-700 text-white px-5 py-2.5 rounded-lg text-sm font-semibold transition-colors disabled:opacity-60 whitespace-nowrap"
                >
                  {completing ? 'Verifica…' : <><CheckCircle2 className="h-4 w-4" strokeWidth={1.5} /> Conferma e Incassa</>}
                </button>
              </div>
            </div>
          )}

          {/* Cancel: only after acceptance, before completion */}
          {['PAYMENT_PENDING', 'CONFIRMED'].includes(booking.status) && (
            <button
              onClick={handleCancel}
              className="w-full bg-red-50 hover:bg-red-100 text-red-600 border border-red-200 font-semibold py-3 rounded-xl transition-colors text-sm"
            >
              Annulla Prenotazione
            </button>
          )}
        </div>
      </div>

      {/* Chat is unlocked only when the booking has been confirmed or completed. */}
      {['CONFIRMED', 'COMPLETED'].includes(booking.status) && (
        <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6">
          <BookingChat bookingId={booking.id} />
        </div>
      )}
    </div>
  )
}
