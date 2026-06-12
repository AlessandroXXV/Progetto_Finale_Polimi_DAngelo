import { useEffect, useMemo, useState } from 'react'
import { useParams, useNavigate, useLocation, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { profileService } from '../../services/profileService'
import { availabilityService } from '../../services/availabilityService'
import { reviewService } from '../../services/reviewService'
import { bookingService } from '../../services/bookingService'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import Badge from '../../components/common/Badge'
import StarRating from '../../components/common/StarRating'
import UserAvatar from '../../components/common/UserAvatar'
import WeeklyCalendar from '../../components/calendar/WeeklyCalendar'
import useAuthStore from '../../store/authStore'
import toast from 'react-hot-toast'
import { addDays, getWeekStart, toDateInputValue } from '../../lib/weekCalendar'
import { getErrorMessage } from '../../lib/getErrorMessage'
import { ArrowLeft, MapPin, CalendarDays } from 'lucide-react'

const DAY_NAMES = ['Lunedì', 'Martedì', 'Mercoledì', 'Giovedì', 'Venerdì', 'Sabato', 'Domenica']

const isSameSlotDay = (dateValue, slotDayIndex) => {
  if (!dateValue || slotDayIndex == null) return false
  const pickedDate = new Date(`${dateValue}T00:00:00`)
  const pickedDay = (pickedDate.getDay() + 6) % 7
  return pickedDay === slotDayIndex
}

const formatSlotTime = (value) => {
  if (typeof value !== 'string') return '--:--'
  const match = value.match(/^(\d{2}:\d{2})/)
  return match ? match[1] : '--:--'
}

const shouldShowCancelledBooking = (bookingItem, currentUserId) =>
  bookingItem?.status === 'CANCELLED' && bookingItem.clientId === currentUserId

const BOOKING_STATUS_PRIORITY = {
  COMPLETED: 4,
  CONFIRMED: 3,
  PAYMENT_PENDING: 2,
  CANCELLED: 0,
}

const getBookingPriority = (bookingItem) => BOOKING_STATUS_PRIORITY[bookingItem?.status] ?? -1

const resolveVisibleBooking = (bookingsForCell, currentUserId) => {
  if (!bookingsForCell?.length) return null

  const activeBooking = bookingsForCell
    .filter((bookingItem) => bookingItem.status !== 'CANCELLED')
    .reduce((bestBooking, bookingItem) => {
      if (!bestBooking) return bookingItem
      return getBookingPriority(bookingItem) > getBookingPriority(bestBooking) ? bookingItem : bestBooking
    }, null)

  if (activeBooking) return activeBooking

  return bookingsForCell.find((bookingItem) => shouldShowCancelledBooking(bookingItem, currentUserId)) ?? null
}

export default function ProfileDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const { user } = useAuthStore()

  const [selectedSlot, setSelectedSlot] = useState(null)
  const [selectedDate, setSelectedDate] = useState('')
  const [notes, setNotes] = useState('')
  const [booking, setBooking] = useState(false)
  const [weekStart, setWeekStart] = useState(() => getWeekStart(new Date()))

  const { data, isLoading: loading, error } = useQuery({
    queryKey: ['profile', id, user?.userId ?? null],
    queryFn: async () => {
      const isAuth = !!user?.userId

       // Authenticated users receive the full profile payload with the provider userId.
       // Guests only receive the public profile payload, which keeps the avatar hidden.
       const { data: p } = isAuth
         ? await profileService.getDetailAuthenticated(id)
         : await profileService.getById(id)

       // Without a provider userId we cannot fetch availability, reviews, or bookings.
       if (!p.userId) {
         return { profile: p, slots: [], reviews: [], providerBookings: [] }
       }

      const requests = [
        availabilityService.getByStudent(p.userId),
        reviewService.getByProfile(p.id),
      ]
      if (user?.userId) {
        requests.push(bookingService.getProviderBookings(p.userId))
      }
      const [slotsRes, reviewsRes, bookingsRes] = await Promise.all(requests)
      return {
        profile: p,
        slots: slotsRes.data.filter((s) => s.status === 'AVAILABLE'),
        reviews: reviewsRes.data.reviews,
        providerBookings: bookingsRes?.data ?? [],
      }
    },
  })

  useEffect(() => {
    if (error) toast.error(getErrorMessage(error))
  }, [error])

  const profile = data?.profile ?? null
  const slots = data?.slots ?? []
  const reviews = data?.reviews ?? []
  const providerBookings = data?.providerBookings ?? []

  const slotsByDay = useMemo(
    () =>
      slots.reduce((acc, slot) => {
        if (!acc[slot.dayOfWeek]) acc[slot.dayOfWeek] = []
        acc[slot.dayOfWeek].push(slot)
        return acc
      }, {}),
    [slots]
  )

  const bookingByCell = useMemo(
    () =>
      providerBookings.reduce((acc, bookingItem) => {
        if (!bookingItem.bookingDate) return acc
        const key = `${bookingItem.slotId}:${bookingItem.bookingDate}`
        if (!acc[key]) acc[key] = []
        acc[key].push(bookingItem)
        return acc
      }, {}),
    [providerBookings]
  )

  const clearSelection = () => {
    setSelectedSlot(null)
    setSelectedDate('')
  }

  const handleSelectOccurrence = (slot, isoDate, bookingForCell) => {
    if (!user) {
      toast('Accedi o registrati per prenotare questo slot')
      navigate('/login', { state: { from: location } })
      return
    }

    const todayIso = toDateInputValue(new Date())
    const isPastDate = isoDate < todayIso
    const isBlocked = isPastDate || (bookingForCell && bookingForCell.status !== 'CANCELLED') || slot.status !== 'AVAILABLE'

    if (isBlocked && !(bookingForCell && bookingForCell.status === 'CANCELLED')) {
      toast.error(bookingForCell ? `Fascia già occupata (${bookingForCell.status})` : 'Seleziona una fascia disponibile')
      return
    }

    setSelectedSlot(slot)
    setSelectedDate(isoDate)
  }

  const handleBook = async () => {
    if (!user) {
      navigate('/login', { state: { from: location } })
      return
    }

    if (!selectedSlot) {
      toast.error('Seleziona uno slot')
      return
    }
    if (!selectedDate) {
      toast.error('Seleziona una data')
      return
    }
    if (!isSameSlotDay(selectedDate, selectedSlot.dayOfWeek)) {
      toast.error(`La data selezionata non è un ${DAY_NAMES[selectedSlot.dayOfWeek]}`)
      return
    }
    if (user?.role === 'CLIENT' && user?.isVerified === false) {
      toast.error('Per prenotare devi prima verificare il tuo account dalle impostazioni.')
      navigate('/settings')
      return
    }

    setBooking(true)
    try {
      const { data } = await bookingService.create({
        slotId: selectedSlot.id,
        profileId: profile.id,
        bookingDate: selectedDate,
        notes: notes || undefined,
      })
      toast.success('Prenotazione creata! Procedi al pagamento.')
      navigate(`/booking/${data.id}`)
    } catch (err) {
      toast.error(getErrorMessage(err))
    } finally {
      setBooking(false)
    }
  }

  const handlePrevWeek = () => {
    setWeekStart((current) => addDays(current, -7))
    clearSelection()
  }

  const handleNextWeek = () => {
    setWeekStart((current) => addDays(current, 7))
    clearSelection()
  }

  if (loading) return <LoadingSpinner />
  if (!profile) return <p className="py-12 text-center text-slate-500">Profilo non trovato</p>

  const avgRating = reviews.length ? (reviews.reduce((s, r) => s + r.rating, 0) / reviews.length).toFixed(1) : null
  const bookingBlockedForVerification = user?.role === 'CLIENT' && user?.isVerified === false
  const currentUserId = user?.userId
  const bookingBlockedForGuest = !user

  return (
    <div className="space-y-6">
      <button onClick={() => navigate(-1)} className="inline-flex items-center gap-2 text-sm font-medium text-indigo-600 transition hover:text-indigo-700 hover:underline">
        <ArrowLeft className="h-4 w-4" strokeWidth={1.5} />
        <span>Indietro</span>
      </button>

      <section className="page-surface overflow-hidden">
         {/* Header banner with the profile identity and the most important pricing data. */}
         <div className="bg-linear-to-br from-[#5B4EE8] to-[#4a3fd4] px-5 py-6 text-white sm:px-6 sm:py-8">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
            <div className="flex items-start gap-4">
              <UserAvatar userId={profile.userId} username={profile.username} sizeClassName="w-16 h-16" textClassName="text-lg" />
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.2em] text-white/70">Profilo servizio</p>
                <h1 className="mt-2 text-3xl font-bold tracking-tight sm:text-4xl">{profile.title}</h1>
                <p className="mt-2 text-sm font-medium text-white/85">@{profile.username}</p>
              </div>
            </div>

            <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
               <div className="rounded-2xl border border-white/15 bg-white/10 px-4 py-3 backdrop-blur">
                <p className="text-xs text-white/70">Tariffa oraria</p>
                <p className="text-2xl font-bold text-white">€{parseFloat(profile.hourlyRate).toFixed(2)}</p>
              </div>
            </div>
          </div>

          <div className="mt-6 grid gap-3 sm:grid-cols-3">
            <div className="rounded-2xl border border-white/15 bg-white/10 px-4 py-3 backdrop-blur">
              <p className="text-xs text-white/70">Recensioni</p>
              <p className="mt-1 text-xl font-bold">{reviews.length}</p>
            </div>
            <div className="rounded-2xl border border-white/15 bg-white/10 px-4 py-3 backdrop-blur">
              <p className="text-xs text-white/70">Modalità</p>
              <p className="mt-1 text-xl font-bold">{profile.deliveryMode}</p>
            </div>
            <div className="rounded-2xl border border-white/15 bg-white/10 px-4 py-3 backdrop-blur">
              <p className="text-xs text-white/70">Stato</p>
              <p className="mt-1 text-xl font-bold">{slots.length > 0 ? 'Disponibile' : 'In pausa'}</p>
            </div>
          </div>
        </div>

          <div className="space-y-4 px-5 py-5 sm:px-6">
            {avgRating && (
              <div className="flex flex-wrap items-center gap-2 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
              <StarRating rating={Math.round(avgRating)} />
              <span className="font-semibold">{avgRating}</span>
              <span className="text-amber-700/80">({reviews.length} recensioni)</span>
            </div>
          )}

          {profile.description && (
            <div className="panel p-4 sm:p-5">
              <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">Descrizione</h2>
              <p className="mt-3 leading-7 text-slate-600">{profile.description}</p>
            </div>
          )}

          {profile.address && (
            <div className="panel flex items-center gap-3 p-4 sm:p-5">
              <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600">
                <MapPin className="h-5 w-5" strokeWidth={1.5} />
              </span>
              <div>
                <p className="text-sm font-semibold text-slate-900">Sede / indirizzo</p>
                <p className="text-sm text-slate-500">{profile.address}</p>
              </div>
            </div>
          )}
        </div>
      </section>

      <div className="panel p-4 sm:p-6">
          {/* Weekly availability grid: each day column renders the bookable slot cards. */}
          <WeeklyCalendar
            weekStart={weekStart}
          onPrevWeek={handlePrevWeek}
          onNextWeek={handleNextWeek}
          title="Disponibilità settimanale"
          subtitle="Usa le frecce per scorrere le settimane e seleziona una fascia disponibile per prenotare."
            renderDay={(day) => {
              // Sort each day's slots so the earliest start time appears first.
              const daySlots = (slotsByDay[day.index] || []).sort((a, b) => a.startTime.localeCompare(b.startTime))

            if (daySlots.length === 0) {
              return <p className="text-sm text-slate-400">Nessuno slot disponibile</p>
            }

               return daySlots.map((slot) => {
                 const bookingForCell = resolveVisibleBooking(bookingByCell[`${slot.id}:${day.isoDate}`], currentUserId)
              const isPastDate = day.isoDate < toDateInputValue(new Date())
              const canSeeCancelled = shouldShowCancelledBooking(bookingForCell, currentUserId)
              const visibleBooking = bookingForCell?.status === 'CANCELLED' && !canSeeCancelled ? null : bookingForCell
              const isBlocked = isPastDate || (visibleBooking && visibleBooking.status !== 'CANCELLED') || slot.status !== 'AVAILABLE'
              const statusValue = visibleBooking?.status ?? 'AVAILABLE'
              const cardClass = visibleBooking && visibleBooking.status === 'CANCELLED'
                ? 'border-amber-200 bg-amber-50'
                : visibleBooking
                  ? 'border-orange-200 bg-orange-50'
                  : 'border-emerald-200 bg-emerald-50'

              return (
                <button
                  key={slot.id}
                  type="button"
                  onClick={() => handleSelectOccurrence(slot, day.isoDate, visibleBooking)}
                  disabled={isBlocked || Boolean(visibleBooking && visibleBooking.status === 'CANCELLED')}
                  className={`w-full overflow-hidden rounded-xl border px-3 py-3 text-left transition-all ${cardClass} ${
                    isBlocked || Boolean(visibleBooking && visibleBooking.status === 'CANCELLED')
                      ? 'cursor-not-allowed opacity-70'
                      : 'hover:shadow-sm'
                  }`}
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="min-w-0">
                      <Badge value={statusValue} />

                      <p className="text-sm font-semibold text-slate-800">
                        {formatSlotTime(slot.startTime)} - {formatSlotTime(slot.endTime)}
                      </p>
                      </div>
                    </div>

                  {visibleBooking ? (
                    <div className="mt-2 space-y-1 text-xs text-slate-600">
                      <p className="font-medium text-slate-700">
                        {visibleBooking.status === 'CANCELLED' ? 'Prenotazione annullata' : 'Fascia occupata'}
                      </p>
                      <p>Stato: <span className="font-semibold">{visibleBooking.status}</span></p>
                      <p>Data: <span className="font-semibold">{visibleBooking.bookingDate}</span></p>
                    </div>
                  ) : (
                    <p className="mt-2 text-xs text-slate-600">Disponibile per la prenotazione</p>
                  )}
                </button>
              )
            })
          }}
        />
      </div>

      {selectedSlot && selectedDate && (
        <section className="panel p-4 sm:p-6">
               {/* Selection summary shown only after the user picks a slot and date. */}
               <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <h2 className="text-xl font-semibold text-slate-900">Selezione corrente</h2>
              <p className="text-sm text-slate-500">Verifica i dettagli prima di confermare la prenotazione.</p>
            </div>
            <button type="button" onClick={clearSelection} className="secondary-button px-4 py-2 text-sm">
              Svuota selezione
            </button>
          </div>

          <div className="grid gap-4 lg:grid-cols-[1.1fr_0.9fr]">
            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 sm:p-5">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Slot selezionato</p>
              <p className="mt-2 text-lg font-semibold text-slate-900">
                {DAY_NAMES[selectedSlot.dayOfWeek]} · {formatSlotTime(selectedSlot.startTime)} - {formatSlotTime(selectedSlot.endTime)}
              </p>
              <p className="mt-1 text-sm text-slate-500">Data: {selectedDate}</p>
            </div>

            <div>
              <label className="field-label">
                Note per il tutor <span className="text-slate-400">(opzionale)</span>
              </label>
              <textarea
                rows={4}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                className="field resize-none"
                placeholder="Aggiungi eventuali note o richieste…"
              />
            </div>
          </div>

          {bookingBlockedForVerification && (
            <p className="mt-4 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
              Il tuo account non è verificato: completa la verifica dalle impostazioni per poter prenotare.
            </p>
          )}

          {bookingBlockedForGuest ? (
            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              <Link
                to="/login"
                state={{ from: location }}
                className="secondary-button block w-full px-4 py-3 text-center"
              >
                Accedi per prenotare
              </Link>
              <Link
                to="/register"
                className="primary-button block w-full px-4 py-3 text-center"
              >
                Registrati
              </Link>
            </div>
          ) : (
            <button
              disabled={!selectedSlot || !selectedDate || booking || bookingBlockedForVerification}
              onClick={handleBook}
              className="primary-button mt-4 w-full"
            >
              {booking ? (
                'Prenotazione in corso…'
              ) : (
                <span className="inline-flex items-center justify-center gap-2">
                  <CalendarDays className="h-4 w-4" strokeWidth={1.5} />
                  Prenota questo slot
                </span>
              )}
            </button>
          )}
        </section>
      )}

      <section className="panel p-4 sm:p-6">
          {/* Review list is shown below the booking controls to keep the booking flow first. */}
          <h2 className="mb-5 text-xl font-semibold text-slate-900">Recensioni</h2>
        {reviews.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 px-5 py-8 text-center">
            <p className="text-lg font-semibold text-slate-900">Ancora nessuna recensione ricevuta</p>
            <p className="mt-2 text-sm text-slate-500">
              I feedback dei clienti compariranno qui dopo le prime sessioni completate.
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {reviews.map((r) => (
              <div key={r.id} className="border-b border-slate-100 pb-4 last:border-0">
                <div className="mb-2 flex items-center gap-2">
                  <StarRating rating={r.rating} />
                  <span className="text-xs text-slate-400">{new Date(r.reviewedAt).toLocaleDateString('it-IT')}</span>
                </div>
                {r.comment && <p className="text-sm leading-6 text-slate-600">{r.comment}</p>}
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
