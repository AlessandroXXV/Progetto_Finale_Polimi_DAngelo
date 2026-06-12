import { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { loadStripe } from '@stripe/stripe-js'
import { Elements } from '@stripe/react-stripe-js'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { bookingService } from '../../services/bookingService'
import { paymentService } from '../../services/paymentService'
import { reviewService } from '../../services/reviewService'
import useAuthStore from '../../store/authStore'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import Badge from '../../components/common/Badge'
import BookingChat from '../../components/chat/BookingChat'
import PaymentForm from '../../components/stripe/PaymentForm'
import MockPaymentForm from '../../components/stripe/MockPaymentForm'
import usePaymentCountdown from '../../hooks/usePaymentCountdown'
import { ArrowLeft, CreditCard, CheckCircle2, Star } from 'lucide-react'
import toast from 'react-hot-toast'

const stripePromise = loadStripe(import.meta.env.VITE_STRIPE_PK ?? 'pk_test_your_key_here')
const MIN_REVIEW_COMMENT_LENGTH = 10

const formatBookingDate = (value) => {
  if (!value) return 'Non definita'
  return new Date(`${value}T00:00:00`).toLocaleDateString('it-IT', {
    weekday: 'long',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}

const formatSlotTime = (value) => {
  if (typeof value !== 'string') return '--:--'
  const match = value.match(/^(\d{2}:\d{2})/)
  return match ? match[1] : value
}

function SectionCard({ title, subtitle, children, className = '' }) {
  return (
    <section className={`panel p-4 sm:p-6 ${className}`}>
      {(title || subtitle) && (
        <div className="mb-5">
          {title && <h2 className="text-xl font-semibold tracking-tight text-slate-900">{title}</h2>}
          {subtitle && <p className="mt-1 text-sm text-slate-500">{subtitle}</p>}
        </div>
      )}
      {children}
    </section>
  )
}

function InfoItem({ label, value, valueClassName = '' }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
      <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p>
      <div className={`mt-2 text-sm font-medium text-slate-900 ${valueClassName}`}>{value}</div>
    </div>
  )
}

function ActionButton({ children, className = '', ...props }) {
  return (
    <button
      {...props}
      className={`inline-flex min-h-11 items-center justify-center rounded-xl px-4 py-3 text-sm font-semibold transition ${className}`}
    >
      {children}
    </button>
  )
}

export default function BookingDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuthStore()
  const queryClient = useQueryClient()
  const [clientSecret, setClientSecret] = useState(null)
  const [reviewForm, setReviewForm] = useState({ rating: 5, comment: '' })
  const [submittingReview, setSubmittingReview] = useState(false)
  const [confirmCode, setConfirmCode] = useState('')
  const hasTriggeredExpiryReload = useRef(false)

  const { data: booking, isLoading: loading, refetch: refetchBooking, error } = useQuery({
    queryKey: ['booking', id],
    queryFn: () => bookingService.getById(id).then((r) => r.data),
  })

  const { data: reviewExists = false, refetch: refetchReviewExists } = useQuery({
    queryKey: ['booking', id, 'review-exists'],
    queryFn: () => reviewService.checkExists(id).then((r) => r.data.exists),
    enabled: booking?.status === 'COMPLETED',
  })

  useEffect(() => {
    if (error) toast.error('Errore caricamento prenotazione')
  }, [error])

  const paymentCountdown = usePaymentCountdown(booking)

  useEffect(() => {
    if (!paymentCountdown.isVisible || !paymentCountdown.isExpired) {
      hasTriggeredExpiryReload.current = false
      return
    }
    if (hasTriggeredExpiryReload.current) return
    hasTriggeredExpiryReload.current = true
    refetchBooking()
  }, [paymentCountdown.isVisible, paymentCountdown.isExpired, refetchBooking])


  const handleComplete = async (e) => {
    e.preventDefault()
    try {
      await bookingService.complete(booking.id, confirmCode)
      toast.success('Sessione completata! Il pagamento è stato rilasciato al tutor.')
      setConfirmCode('')
      queryClient.invalidateQueries({ queryKey: ['booking', id] })
    } catch (err) {
      toast.error(err.response?.data?.message ?? 'Codice non valido')
    }
  }

  const handleCreateIntent = async () => {
    try {
      const { data } = await paymentService.createIntent(booking.id)
      setClientSecret(data.clientSecret)
    } catch (err) {
      toast.error(err.response?.data?.message ?? 'Errore creazione pagamento')
    }
  }

  const handleCancel = async () => {
    if (!window.confirm('Sei sicuro di voler annullare la prenotazione?')) return
    try {
      await bookingService.cancel(booking.id)
      toast.success('Prenotazione annullata')
      queryClient.invalidateQueries({ queryKey: ['booking', id] })
      queryClient.invalidateQueries({ queryKey: ['bookings', 'my'] })
    } catch (err) {
      toast.error(err.response?.data?.message ?? 'Errore')
    }
  }

  const handleReview = async (e) => {
    e.preventDefault()
    const normalizedComment = reviewForm.comment.trim()
    if (normalizedComment.length > 0 && normalizedComment.length < 10) {
      toast.error('Il commento deve avere almeno 10 caratteri se inserito')
      return
    }

    setSubmittingReview(true)
    try {
      await reviewService.create({
        bookingId: booking.id,
        rating: reviewForm.rating,
        comment: normalizedComment,
      })
      toast.success('Recensione inviata!')
      refetchReviewExists()
      queryClient.invalidateQueries({ queryKey: ['reviews', 'my'] })
    } catch (err) {
      toast.error(err.response?.data?.message ?? 'Errore recensione')
    } finally {
      setSubmittingReview(false)
    }
  }

  if (loading) return <LoadingSpinner />
  if (!booking) return <p className="text-center text-gray-500 py-12">Prenotazione non trovata</p>

  const isClient = user?.userId === booking.clientId
  const isProvider = user?.userId === booking.providerId
  const bookingAmount = parseFloat(booking.totalAmount)
  const clientTotalAmount = bookingAmount + 0.5
  const trimmedCommentLength = reviewForm.comment.trim().length
  const isReviewCommentTooShort =
    trimmedCommentLength > 0 && trimmedCommentLength < MIN_REVIEW_COMMENT_LENGTH

  return (
    <div className="space-y-6">
      <button
        onClick={() => navigate(-1)}
        className="inline-flex items-center gap-2 text-sm font-medium text-indigo-600 transition hover:text-indigo-700 hover:underline"
      >
        <ArrowLeft className="h-4 w-4" strokeWidth={1.5} />
        <span>Indietro</span>
      </button>

      {/* Header area with booking status and pricing summary. */}
      <section className="page-surface overflow-hidden">
        <div className="bg-linear-to-br from-[#5B4EE8] to-[#4a3fd4] px-5 py-6 text-white sm:px-6 sm:py-8">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
            <div className="max-w-2xl">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-white/70">Dettaglio prenotazione</p>
              <h1 className="mt-2 text-3xl font-bold tracking-tight sm:text-4xl">{booking.profileTitle}</h1>
              <p className="mt-2 text-sm text-white/80">Prenotazione #{booking.id}</p>
            </div>
            <div className="flex flex-col items-start gap-3 sm:flex-row sm:items-center">
              <Badge value={booking.status} />
              <div className="rounded-2xl border border-white/15 bg-white/10 px-4 py-3 backdrop-blur">
                <p className="text-xs text-white/70">Importo base</p>
                <p className="text-2xl font-bold">€{bookingAmount.toFixed(2)}</p>
              </div>
            </div>
          </div>
        </div>

        <div className="space-y-4 px-5 py-5 sm:px-6">
          <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
            <InfoItem label="Cliente" value={`@${booking.clientUsername}`} />
            <InfoItem label="Tutor" value={`@${booking.providerUsername}`} />
            <InfoItem label="Data" value={<span className="capitalize">{formatBookingDate(booking.bookingDate)}</span>} />
            <InfoItem valueClassName="text-base font-bold text-indigo-700" label="Orario" value={`${formatSlotTime(booking.slotStartTime)} - ${formatSlotTime(booking.slotEndTime)}`} />
          </div>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <InfoItem
              label="Importo"
              value={
                <div>
                  <p className="text-2xl font-bold text-indigo-700">€{bookingAmount.toFixed(2)}</p>
                  {isClient && (
                    <p className="mt-1 text-xs text-slate-500">
                      + €0,50 costi mantenimento del servizio · Totale da pagare: €{clientTotalAmount.toFixed(2)}
                    </p>
                  )}
                </div>
              }
            />
            {booking.confirmationCode && (isClient || booking.status === 'COMPLETED') && (
              <InfoItem
                label="Codice conferma"
                value={<span className="font-mono text-2xl tracking-[0.25em] text-emerald-700">{booking.confirmationCode}</span>}
              />
            )}
          </div>

          {booking.notes && (
            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 sm:p-5">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Note</p>
              <p className="mt-2 text-sm leading-6 text-slate-700">{booking.notes}</p>
            </div>
          )}

          {booking.status === 'CANCELLED' && booking.cancellationReason === 'PAYMENT_TIMEOUT' && (
            <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
              Prenotazione annullata: il pagamento non è stato completato entro 10 minuti. Questa data e fascia oraria sono bloccate in modo permanente per questo cliente.
            </div>
          )}

          {paymentCountdown.isVisible && (
            <div className="rounded-2xl border border-amber-300 bg-amber-50 p-4">
              <div className="mb-2 flex items-center justify-between gap-3 text-sm">
                <span className="font-semibold text-amber-900">Tempo rimanente per completare il pagamento</span>
                <span className="font-mono text-base font-bold text-amber-800">{paymentCountdown.formatted}</span>
              </div>
              <div className="h-2 w-full overflow-hidden rounded-full bg-amber-100">
                <div
                  className="h-full bg-amber-500 transition-all"
                  style={{ width: `${100 - paymentCountdown.progressPct}%` }}
                />
              </div>
              {paymentCountdown.isExpired && <p className="mt-2 text-xs text-amber-800">Tempo scaduto. Aggiornamento stato in corso...</p>}
            </div>
          )}
        </div>
      </section>

      {/* Primary actions depend on the current booking state. */}
      <SectionCard title="Azioni rapide" subtitle="Le opzioni disponibili cambiano in base allo stato della prenotazione.">
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {/* Legacy REQUESTED actions were removed because the backend now starts from PAYMENT_PENDING. */}

          {isClient && booking.status === 'PAYMENT_PENDING' && !clientSecret && (
            <ActionButton onClick={handleCreateIntent} className="gap-2 bg-indigo-600 text-white hover:bg-indigo-700">
              <CreditCard className="h-4 w-4" strokeWidth={1.5} />
              Procedi al pagamento
            </ActionButton>
          )}

          {['PAYMENT_PENDING', 'CONFIRMED'].includes(booking.status) && (
            <ActionButton onClick={handleCancel} className="border border-red-200 bg-red-50 text-red-700 hover:bg-red-100">
              Annulla prenotazione
            </ActionButton>
          )}
        </div>
      </SectionCard>

      {isClient && booking.status === 'CONFIRMED' && booking.confirmationCode && (
        <SectionCard className="border border-indigo-200 bg-indigo-50/80" title="Codice di conferma" subtitle="Mostralo al tutor al termine della sessione per sbloccare il pagamento.">
          <div className="text-center">
            <p className="font-mono text-4xl font-bold tracking-[0.25em] text-indigo-800 sm:text-5xl">{booking.confirmationCode}</p>
            <p className="mt-3 text-xs text-indigo-700/80">Il tutor lo inserirà per completare la sessione.</p>
          </div>
        </SectionCard>
      )}

      {/* Provider confirmation form: the tutor enters the 6-digit code received from the client. */}
      {isProvider && booking.status === 'CONFIRMED' && (
        <SectionCard title="Completa la sessione" subtitle="Inserisci il codice a 6 cifre fornito dal cliente per rilasciare il pagamento.">
          <form onSubmit={handleComplete} className="grid gap-3 sm:grid-cols-[1fr_auto]">
            <input
              type="text"
              inputMode="numeric"
              value={confirmCode}
              onChange={(e) => setConfirmCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
              placeholder="000000"
              maxLength={6}
              className="field font-mono text-center text-2xl tracking-[0.35em]"
            />
            <button
              type="submit"
              disabled={confirmCode.length !== 6}
              className="primary-button inline-flex items-center justify-center gap-2 bg-emerald-600 px-6 hover:bg-emerald-700"
            >
              <CheckCircle2 className="h-4 w-4" strokeWidth={1.5} />
              Conferma e incassa
            </button>
          </form>
        </SectionCard>
      )}

      {/* Stripe Elements is mounted only after a payment intent has been created. */}
      {clientSecret && booking.status === 'PAYMENT_PENDING' && (
        <SectionCard title="Pagamento" subtitle="Il pagamento viene autorizzato subito e catturato solo a sessione completata.">
          {clientSecret.startsWith('pi_test_secret') ? (
            <MockPaymentForm
              clientSecret={clientSecret}
              bookingId={booking.id}
                onSuccess={() => {
                  setClientSecret(null)
                  refetchBooking()
                }}
              />
            ) : (
              <Elements stripe={stripePromise} options={{ clientSecret }}>
                <PaymentForm
                  clientSecret={clientSecret}
                  bookingId={booking.id}
                  onSuccess={() => {
                    setClientSecret(null)
                    refetchBooking()
                  }}
                />
              </Elements>
            )}
        </SectionCard>
      )}

      {/* Review form only appears after completion and only once per booking. */}
      {isClient && booking.status === 'COMPLETED' && !reviewExists && (
        <SectionCard title="Lascia una recensione" subtitle="Il tuo feedback aiuta altri utenti a scegliere meglio.">
          <form onSubmit={handleReview} className="space-y-5">
            <div>
              <label className="field-label">Voto</label>
              <div className="flex flex-wrap items-center gap-2">
                {[1, 2, 3, 4, 5].map((v) => (
                  <button
                    key={v}
                    type="button"
                    onClick={() => setReviewForm((f) => ({ ...f, rating: v }))}
                    className={`transition-transform hover:scale-110 ${v <= reviewForm.rating ? 'text-yellow-400' : 'text-slate-300'}`}
                  >
                    <Star className="h-7 w-7" strokeWidth={1.5} fill={v <= reviewForm.rating ? 'currentColor' : 'none'} />
                  </button>
                ))}
                <span className="ml-1 text-sm text-slate-500">{reviewForm.rating}/5</span>
              </div>
            </div>

            <div>
              <label className="field-label">Commento</label>
              <textarea
                rows={4}
                value={reviewForm.comment}
                onChange={(e) => setReviewForm((f) => ({ ...f, comment: e.target.value }))}
                className="field resize-none"
                placeholder="Condividi la tua esperienza (opzionale, min 10 caratteri)…"
              />
              <p className={`mt-1 text-xs ${isReviewCommentTooShort ? 'text-red-500' : 'text-slate-500'}`}>
                {trimmedCommentLength}/{MIN_REVIEW_COMMENT_LENGTH} caratteri minimi
              </p>
            </div>

            <button type="submit" disabled={submittingReview} className="primary-button inline-flex items-center justify-center gap-2 bg-yellow-500 hover:bg-yellow-600">
              {submittingReview ? (
                'Invio…'
              ) : (
                <>
                  <Star className="h-4 w-4" strokeWidth={1.5} />
                  Invia recensione
                </>
              )}
            </button>
          </form>
        </SectionCard>
      )}

      {reviewExists && booking.status === 'COMPLETED' && (
        <div className="flex items-center gap-2 rounded-2xl border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700">
          <CheckCircle2 className="h-4 w-4" strokeWidth={1.5} />
          Hai già lasciato una recensione per questa sessione
        </div>
      )}

      {/* Chat remains available for confirmed and completed bookings. */}
      {['CONFIRMED', 'COMPLETED'].includes(booking.status) && (
        <SectionCard title="Chat prenotazione" subtitle="Restate in contatto con messaggi salvati e sincronizzati in tempo reale.">
          <BookingChat bookingId={booking.id} />
        </SectionCard>
      )}
    </div>
  )
}
