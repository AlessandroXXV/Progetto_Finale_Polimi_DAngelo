// Page that shows all bookings received by the logged-in tutor (provider view).
// Bookings are filterable by status and navigated week by week.
// Bookings without a date are treated as legacy entries and shown in a warning banner.
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { bookingService } from '../../services/bookingService'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import Badge from '../../components/common/Badge'
import toast from 'react-hot-toast'
import { addDays, getWeekStart, toDateInputValue } from '../../lib/weekCalendar'
import { getErrorMessage } from '../../lib/getErrorMessage'
import { ArrowLeft, ArrowRight, CalendarDays } from 'lucide-react'

// Parse ISO date string as local midnight to avoid timezone-shifted display.
const formatBookingDate = (value) => {
  if (!value) return 'Data non definita'
  return new Date(`${value}T00:00:00`).toLocaleDateString('it-IT', {
    weekday: 'short',
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

const FILTER_OPTIONS = [
  { value: 'ALL', label: 'Tutte' },
  { value: 'PAYMENT_PENDING', label: 'In attesa di pagamento' },
  { value: 'CONFIRMED', label: 'Confermata' },
  { value: 'COMPLETED', label: 'Completata' },
  { value: 'CANCELLED', label: 'Annullata' },
]

export default function StudentBookingsPage() {
  const [filter, setFilter] = useState('ALL')
  const [weekStart, setWeekStart] = useState(() => getWeekStart(new Date()))
  const navigate = useNavigate()

  const { data: bookings = [], isLoading: loading, error } = useQuery({
    queryKey: ['bookings', 'my'],
    queryFn: () => bookingService.getMy().then((r) => r.data),
  })

  useEffect(() => {
    if (error) toast.error(getErrorMessage(error))
  }, [error])

  // Apply the status filter first, then slice to the current week.
  const filtered = filter === 'ALL' ? bookings : bookings.filter((b) => b.status === filter)
  const weekStartIso = toDateInputValue(weekStart)
  const weekEnd = addDays(weekStart, 6)
  const weekEndIso = toDateInputValue(weekEnd)
  const weekRangeLabel = `${weekStart.toLocaleDateString('it-IT', { day: '2-digit', month: 'short' })} - ${weekEnd.toLocaleDateString('it-IT', { day: '2-digit', month: 'short' })}`

  // ISO string comparison works correctly here because dates are in YYYY-MM-DD format.
  const weekFiltered = filtered.filter(
    (b) => b.bookingDate && b.bookingDate >= weekStartIso && b.bookingDate <= weekEndIso
  )
  // Bookings without a bookingDate are legacy records excluded from the weekly view.
  const unscheduled = filtered.filter((b) => !b.bookingDate)

  const handlePrevWeek = () => setWeekStart((current) => addDays(current, -7))
  const handleNextWeek = () => setWeekStart((current) => addDays(current, 7))

  if (loading) return <LoadingSpinner />

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-800 mb-2">Prenotazioni Ricevute</h1>
        <p className="text-gray-500">Gestisci le prenotazioni dei tuoi clienti</p>
      </div>

      {/* Status filter pills */}
      <div className="flex flex-wrap gap-2 mb-6">
        {FILTER_OPTIONS.map(({ value, label }) => (
          <button
            key={value}
            onClick={() => setFilter(value)}
            className={`text-sm px-4 py-1.5 rounded-full font-medium border transition-colors ${
              filter === value
                ? 'bg-indigo-600 text-white border-indigo-600'
                : 'bg-white text-gray-600 border-gray-300 hover:border-indigo-400'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {/* Week navigator */}
      <div className="mb-6 flex items-center justify-between gap-2 sm:gap-3">
        <button
          type="button"
          onClick={handlePrevWeek}
          className="inline-flex h-10 w-10 items-center justify-center rounded-full border border-gray-300 bg-white text-gray-700 transition-colors hover:border-indigo-400 hover:text-indigo-700"
          aria-label="Settimana precedente"
        >
          <ArrowLeft className="h-4 w-4" strokeWidth={1.5} />
        </button>
        <div className="min-w-0 flex-1 rounded-2xl bg-teal-50 px-3 py-2 text-center text-xs leading-tight font-semibold text-teal-700 sm:flex-none sm:px-4 sm:text-sm">
          {weekRangeLabel}
        </div>
        <button
          type="button"
          onClick={handleNextWeek}
          className="inline-flex h-10 w-10 items-center justify-center rounded-full border border-gray-300 bg-white text-gray-700 transition-colors hover:border-indigo-400 hover:text-indigo-700"
          aria-label="Settimana successiva"
        >
          <ArrowRight className="h-4 w-4" strokeWidth={1.5} />
        </button>
      </div>

      {filtered.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <CalendarDays className="mx-auto mb-4 h-12 w-12" strokeWidth={1.5} />
          <p className="text-lg font-semibold text-slate-900">Nessuna prenotazione ricevuta, per ora.</p>
          <p className="mt-2 text-sm text-slate-500">Quando un cliente prenota, troverai qui tutti i dettagli.</p>
        </div>
      ) : weekFiltered.length === 0 ? (
        // There are bookings overall, but none fall in the currently displayed week.
        <div className="bg-white rounded-xl border border-gray-200 px-5 py-6 text-sm text-gray-500">
          Nessuna prenotazione in questa settimana. Prova a spostarti con le frecce.
        </div>
      ) : (
        <div className="space-y-4">
          {weekFiltered.map((b) => (
            // Clicking the card navigates to the booking detail page.
            <div
              key={b.id}
              onClick={() => navigate(`/student/booking/${b.id}`)}
              className="bg-white rounded-xl border border-gray-200 p-5 cursor-pointer hover:shadow-md hover:border-indigo-300 transition-all"
            >
              <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div className="min-w-0 flex-1">
                  <h3 className="font-semibold text-gray-800 mb-1">{b.profileTitle}</h3>
                  <p className="text-sm text-gray-500 wrap-break-word">
                    {formatBookingDate(b.bookingDate)} · {formatSlotTime(b.slotStartTime)} - {formatSlotTime(b.slotEndTime)}
                  </p>
                  <p className="text-sm text-indigo-600 mt-0.5">Cliente: <span className="break-all">@{b.clientUsername}</span></p>
                </div>
                <div className="flex flex-row items-center justify-between gap-2 sm:flex-col sm:items-end">
                  <Badge value={b.status} />
                  <span className="text-lg font-bold text-indigo-700">€{parseFloat(b.totalAmount).toFixed(2)}</span>
                </div>
              </div>
              {b.status === 'COMPLETED' && b.confirmationCode && (
                <p className="text-xs text-green-600 font-medium mt-2">
                  Codice: <span className="tracking-widest font-bold">{b.confirmationCode}</span>
                </p>
              )}
              {b.notes && (
                <p className="text-xs text-gray-400 mt-1 italic">Note: {b.notes}</p>
              )}
            </div>
          ))}
        </div>
      )}
      {/* Warn the user about legacy bookings that cannot be shown in the weekly view. */}
      {filtered.length > 0 && unscheduled.length > 0 && (
        <div className="mt-4 bg-amber-50 border border-amber-200 rounded-xl px-4 py-3 text-xs text-amber-900">
          {unscheduled.length} prenotazioni senza data esplicita (legacy) non sono incluse nella vista settimanale.
        </div>
      )}
    </div>
  )
}
