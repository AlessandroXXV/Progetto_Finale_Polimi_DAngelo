// Page that lets a tutor manage their recurring weekly availability slots.
// Displays a weekly calendar view and a form to add new slots (max 60 min each).
import { useState, useEffect, useCallback, useMemo } from 'react'
import { Link } from 'react-router-dom'
import { availabilityService } from '../../services/availabilityService'
import { bookingService } from '../../services/bookingService'
import useAuthStore from '../../store/authStore'
import WeeklyCalendar from '../../components/calendar/WeeklyCalendar'
import Badge from '../../components/common/Badge'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import toast from 'react-hot-toast'
import { addDays, getWeekStart } from '../../lib/weekCalendar'
import { getErrorMessage } from '../../lib/getErrorMessage'

const DAY_NAMES = ['Lunedì', 'Martedì', 'Mercoledì', 'Giovedì', 'Venerdì', 'Sabato', 'Domenica']
const DAY_OPTIONS = DAY_NAMES.map((name, i) => ({ value: i, label: name }))

const emptyForm = { dayOfWeek: 0, startTime: '09:00', endTime: '10:00' }

// A booking is "active" if it has not yet been resolved (cancelled or completed).
const isActiveBooking = (booking) => !['CANCELLED', 'COMPLETED'].includes(booking.status)

// Backend may return full HH:MM:SS strings; strip to HH:MM for display.
const formatSlotTime = (value) => {
  if (typeof value !== 'string') return '--:--'
  const match = value.match(/^(\d{2}:\d{2})/)
  return match ? match[1] : '--:--'
}

// Priority order used to choose which booking to surface in a calendar cell
// when multiple bookings share the same slot and date.
const BOOKING_STATUS_PRIORITY = {
  COMPLETED: 4,
  CONFIRMED: 3,
  PAYMENT_PENDING: 2,
  CANCELLED: 0,
}

const getBookingPriority = (booking) => BOOKING_STATUS_PRIORITY[booking?.status] ?? -1

// Given all bookings for a single (slot, date) cell, pick the most relevant one to show.
// Prefers the highest-priority non-cancelled booking; falls back to the first entry.
const resolveVisibleBooking = (bookingsForCell) => {
  if (!bookingsForCell?.length) return null

  const activeBooking = bookingsForCell
    .filter((booking) => booking.status !== 'CANCELLED')
    .reduce((bestBooking, booking) => {
      if (!bestBooking) return booking
      return getBookingPriority(booking) > getBookingPriority(bestBooking) ? booking : bestBooking
    }, null)

  if (activeBooking) return activeBooking

  return bookingsForCell[0] ?? null
}

export default function ManageAvailabilityPage() {
  const { user } = useAuthStore()
  const [slots, setSlots] = useState([])
  // Only bookings where the logged-in user is the provider, filtered for calendar display.
  const [providerBookings, setProviderBookings] = useState([])
  const [count, setCount] = useState({ count: 0, hasAvailability: false })
  const [loading, setLoading] = useState(true)
  const [form, setForm] = useState(emptyForm)
  const [saving, setSaving] = useState(false)
  const [weekStart, setWeekStart] = useState(() => getWeekStart(new Date()))

  // Load slots, summary count, and bookings in parallel to minimise wait time.
  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [slotsRes, countRes, myBookingsRes] = await Promise.all([
        availabilityService.getByStudent(user.userId),
        availabilityService.getMyCount(),
        bookingService.getMy(),
      ])
      setSlots(slotsRes.data)
      setCount(countRes.data)
      // Keep only bookings where this user is the service provider and a date is set.
      setProviderBookings(
        myBookingsRes.data.filter((booking) => booking.providerId === user.userId && booking.bookingDate)
      )
    } catch (err) {
      toast.error(getErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }, [user.userId])

  useEffect(() => {
    load()
  }, [load])

  const handleChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleAdd = async (e) => {
    e.preventDefault()
    // Convert HH:MM strings to total minutes to validate the slot duration.
    const start = form.startTime.split(':').map(Number)
    const end = form.endTime.split(':').map(Number)
    const durMin = end[0] * 60 + end[1] - (start[0] * 60 + start[1])
    if (durMin <= 0) {
      toast.error('Orario fine deve essere dopo inizio')
      return
    }
    if (durMin > 60) {
      toast.error('Durata massima 60 minuti')
      return
    }

    setSaving(true)
    try {
      await availabilityService.create({
        dayOfWeek: parseInt(form.dayOfWeek),
        startTime: form.startTime,
        endTime: form.endTime,
      })
      await load()
      toast.success('Slot aggiunto!')
      setForm(emptyForm)
    } catch (err) {
      toast.error(getErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (slotId) => {
    if (!window.confirm('Eliminare questo slot?')) return
    try {
      await availabilityService.delete(slotId)
      await load()
      toast.success('Slot eliminato')
    } catch (err) {
      toast.error(getErrorMessage(err))
    }
  }

  // Group slots by day index (0 = Monday … 6 = Sunday) for O(1) calendar lookups.
  const slotsByDay = useMemo(
    () =>
      slots.reduce((acc, slot) => {
        if (!acc[slot.dayOfWeek]) acc[slot.dayOfWeek] = []
        acc[slot.dayOfWeek].push(slot)
        return acc
      }, {}),
    [slots]
  )

  // Build a lookup keyed by "slotId:bookingDate" to find bookings for a specific cell.
  const bookingsBySlotAndDate = useMemo(
    () =>
      providerBookings.reduce((acc, booking) => {
        const key = `${booking.slotId}:${booking.bookingDate}`
        if (!acc[key]) acc[key] = []
        acc[key].push(booking)
        return acc
      }, {}),
    [providerBookings]
  )

  // Count active bookings per slot to decide whether deletion is allowed.
  const activeBookingCountsBySlot = useMemo(
    () =>
      providerBookings.reduce((acc, booking) => {
        if (!isActiveBooking(booking)) return acc
        acc[booking.slotId] = (acc[booking.slotId] ?? 0) + 1
        return acc
      }, {}),
    [providerBookings]
  )

  if (loading) return <LoadingSpinner />

  const stripeReady = Boolean(user?.stripeConnected)
  const activeSlotCount = slots.filter((slot) => slot.status === 'AVAILABLE').length
  const handlePrevWeek = () => setWeekStart((current) => addDays(current, -7))
  const handleNextWeek = () => setWeekStart((current) => addDays(current, 7))

  return (
    <div className="space-y-6">
      <section className="page-surface overflow-hidden">
        <div className="bg-linear-to-br from-[#5B4EE8] to-[#4a3fd4] px-5 py-6 text-white sm:px-6 sm:py-8">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div className="max-w-2xl">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-white/70">Calendario disponibilità</p>
              <h1 className="mt-2 text-3xl font-bold tracking-tight sm:text-4xl">Calendario sempre sotto controllo</h1>
              <p className="mt-3 text-sm leading-6 text-white/85 sm:text-base">
                Pubblica nuovi slot in pochi tap, monitora le prenotazioni e resta aggiornato anche da mobile.
              </p>
            </div>

            <div className="grid grid-cols-2 gap-3 sm:max-w-sm">
              <div className="rounded-2xl border border-white/15 bg-white/10 px-4 py-3 backdrop-blur">
                <p className="text-xs text-white/70">Slot creati</p>
                <p className="mt-1 text-2xl font-bold">{count.count}</p>
              </div>
              <div className="rounded-2xl border border-white/15 bg-white/10 px-4 py-3 backdrop-blur">
                <p className="text-xs text-white/70">Disponibili</p>
                <p className="mt-1 text-2xl font-bold">{activeSlotCount}</p>
              </div>
            </div>
          </div>
        </div>

        <div className="grid gap-3 border-t border-white/70 bg-white/70 px-5 py-4 sm:grid-cols-2 sm:px-6 backdrop-blur">
          <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
            <p className="text-sm font-semibold text-slate-900">Stato disponibilità</p>
            <p className="mt-1 text-sm text-slate-500">
              {count.hasAvailability ? 'Hai slot attivi nel tuo calendario' : 'Non hai ancora aggiunto disponibilità'}
            </p>
          </div>
          <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
            <p className="text-sm font-semibold text-slate-900">Stripe Connect</p>
            <p className="mt-1 text-sm text-slate-500">
              {stripeReady ? 'Attivo: puoi creare nuovi slot' : 'Attiva Stripe per sbloccare la creazione'}
            </p>
          </div>
        </div>
      </section>

      {/* Stripe guard banner: adding slots is blocked until payment is configured. */}
      {!stripeReady && (
        <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-4 text-amber-900 shadow-sm">
          <p className="font-semibold">Completa Stripe prima di aggiungere nuovi slot</p>
          <p className="mt-1 text-sm text-amber-800">Abilita Stripe Connect per sbloccare la creazione delle disponibilità.</p>
          <Link to="/student/stripe" className="mt-3 inline-flex font-semibold text-amber-900 underline">
            Vai a Stripe
          </Link>
        </div>
      )}

      <section className="panel p-5 sm:p-6">
        <div className="mb-5 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h2 className="text-xl font-semibold tracking-tight text-slate-900">Aggiungi slot</h2>
            <p className="text-sm text-slate-500">Crea finestre da massimo 60 minuti per la tua disponibilità</p>
          </div>
          <span className="rounded-full bg-indigo-50 px-3 py-1 text-xs font-semibold text-indigo-700">Max 60 min</span>
        </div>

        <form onSubmit={handleAdd} className="grid gap-4 md:grid-cols-[1fr_1fr_1fr_auto] md:items-end">
          <div>
            <label className="field-label">Giorno</label>
            <select name="dayOfWeek" value={form.dayOfWeek} onChange={handleChange} className="field">
              {DAY_OPTIONS.map((d) => (
                <option key={d.value} value={d.value}>{d.label}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="field-label">Inizio</label>
            <input type="time" name="startTime" value={form.startTime} onChange={handleChange} className="field" />
          </div>
          <div>
            <label className="field-label">Fine</label>
            <input type="time" name="endTime" value={form.endTime} onChange={handleChange} className="field" />
          </div>
          {/* Submit is disabled until Stripe is connected. */}
          <button
            type="submit"
            disabled={saving || !stripeReady}
            className="primary-button w-full md:w-auto"
          >
            {saving ? 'Aggiunta…' : 'Aggiungi slot'}
          </button>
        </form>
      </section>

      <section className="panel p-4 sm:p-6">
        <WeeklyCalendar
          weekStart={weekStart}
          onPrevWeek={handlePrevWeek}
          onNextWeek={handleNextWeek}
          title="Calendario settimanale"
          subtitle="Ogni colonna mostra i tuoi slot e l'eventuale prenotazione associata."
          // Quick-fill the day field when the user clicks "Aggiungi" in a column header.
          headerRight={(day) => (
            <button
              type="button"
              onClick={() => setForm((f) => ({ ...f, dayOfWeek: day.index }))}
              className="rounded-full bg-indigo-50 px-3 py-1 text-xs font-semibold text-indigo-700 transition hover:bg-indigo-100"
            >
              Aggiungi
            </button>
          )}
          renderDay={(day) => {
            // Sort slots chronologically within each day column.
            const daySlots = (slotsByDay[day.index] || []).sort((a, b) => a.startTime.localeCompare(b.startTime))

            if (daySlots.length === 0) {
              return <p className="text-sm text-slate-400">Nessuno slot</p>
            }

            return daySlots.map((slot) => {
              // Look up the most relevant booking for this specific slot+date cell.
              const booking = resolveVisibleBooking(bookingsBySlotAndDate[`${slot.id}:${day.isoDate}`])
              const hasActiveBookings = (activeBookingCountsBySlot[slot.id] ?? 0) > 0
              const isOccupied = Boolean(booking)
              const statusValue = booking?.status ?? 'AVAILABLE'
              const isCancelled = booking?.status === 'CANCELLED'
              // Card colour reflects occupancy: green = free, orange = booked, amber = cancelled.
              const cardClass = isCancelled
                ? 'border-amber-200 bg-amber-50'
                : isOccupied
                  ? 'border-orange-200 bg-orange-50'
                  : 'border-emerald-200 bg-emerald-50'

              return (
                <div key={slot.id} className={`overflow-hidden rounded-2xl border px-3 py-3 ${cardClass}`}>
                  <Badge value={statusValue} />

                  <div className="flex items-start justify-between gap-2">
                    <div className="min-w-0">
                      <p className="text-sm font-semibold text-slate-800">
                        {formatSlotTime(slot.startTime)} - {formatSlotTime(slot.endTime)}
                      </p>
                      {/*<p className="text-xs text-slate-500 break-words">{day.fullDateLabel}</p>*/}
                    </div>
                  </div>

                  {isOccupied ? (
                    <div className="mt-2 space-y-1 text-xs text-slate-600">
                      {/*<p className="font-medium text-slate-700">Prenotazione del giorno</p>*/}
                      <p>Servizio: <span className="font-semibold wrap-break-word">{booking.profileTitle}</span></p>
                      <p>Cliente: <span className="font-semibold break-all">@{booking.clientUsername}</span></p>
                      {/*<p>Stato: <span className="font-semibold">{booking.status}</span></p>*/}
                      {/*<p>Data: <span className="font-semibold">{booking.bookingDate}</span></p>*/}
                      {booking.status === 'COMPLETED' && booking.confirmationCode && (
                        <p>Codice: <span className="font-mono font-semibold tracking-widest">{booking.confirmationCode}</span></p>
                      )}
                    </div>
                  ) : (
                    <p className="mt-2 text-xs text-slate-600">Disponibile per nuove prenotazioni</p>
                  )}

                  {/* Deletion is blocked if any active booking references this slot. */}
                  {!hasActiveBookings && (
                    <button
                      onClick={() => handleDelete(slot.id)}
                      className="mt-3 text-xs font-semibold text-red-600 transition hover:text-red-700"
                    >
                      Elimina slot
                    </button>
                  )}
                </div>
              )
            })
          }}
        />
      </section>
    </div>
  )
}
