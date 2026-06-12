import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { bookingService } from '../../services/bookingService'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import Badge from '../../components/common/Badge'
import WeeklyCalendar from '../../components/calendar/WeeklyCalendar'
import toast from 'react-hot-toast'
import { CalendarDays } from 'lucide-react'
import { addDays, getWeekStart, toDateInputValue } from '../../lib/weekCalendar'

const getCardClass = (status) => {
    if (status === 'CANCELLED') return 'border-amber-200 bg-amber-50'
    if (status === 'CONFIRMED' || status === 'COMPLETED') return 'border-emerald-200 bg-emerald-50'
    return 'border-orange-200 bg-orange-50'
}

const formatSlotTime = (value) => {
    if (typeof value !== 'string') return '--:--'
    const match = value.match(/^(\d{2}:\d{2})/)
    return match ? match[1] : value
}

const FILTER_OPTIONS = [
    { value: 'ALL', label: 'Tutte' },
    { value: 'PAYMENT_PENDING', label: 'In attesa di pagamento' },
    { value: 'CONFIRMED', label: 'Confermata' },
    { value: 'COMPLETED', label: 'Completata' },
    { value: 'CANCELLED', label: 'Annullata' },
]

export default function MyBookingsPage() {
    const [filter, setFilter] = useState('ALL')
    const [weekStart, setWeekStart] = useState(() => getWeekStart(new Date()))
    const navigate = useNavigate()

    const { data: bookings = [], isLoading: loading, error } = useQuery({
        queryKey: ['bookings', 'my'],
        queryFn: () => bookingService.getMy().then((r) => r.data),
    })

    useEffect(() => {
        if (error) toast.error('Errore caricamento prenotazioni')
    }, [error])

    const filtered = filter === 'ALL' ? bookings : bookings.filter((b) => b.status === filter)

    // Focus the weekly view on the currently selected calendar window.
    const weekStartIso = toDateInputValue(weekStart)
    const weekEndIso = toDateInputValue(addDays(weekStart, 6))

    // Split bookings into the current week and everything else so the UI stays readable.
    const weekFiltered = filtered.filter(
        (booking) => booking.bookingDate && booking.bookingDate >= weekStartIso && booking.bookingDate <= weekEndIso
    )
    const unscheduled = filtered.filter((booking) => !booking.bookingDate)

    const bookingsByDate = useMemo(() => {
        return weekFiltered
            .slice()
            .sort((left, right) => {
                const leftDate = left.bookingDate || ''
                const rightDate = right.bookingDate || ''
                if (leftDate !== rightDate) return leftDate.localeCompare(rightDate)
                return left.slotStartTime.localeCompare(right.slotStartTime)
            })
            .reduce((acc, booking) => {
                if (!acc[booking.bookingDate]) acc[booking.bookingDate] = []
                acc[booking.bookingDate].push(booking)
                return acc
            }, {})
    }, [weekFiltered])

    const handlePrevWeek = () => setWeekStart((current) => addDays(current, -7))
    const handleNextWeek = () => setWeekStart((current) => addDays(current, 7))

    if (loading) return <LoadingSpinner />

    return (
        <div>
            <div className="mb-8">
                <h1 className="text-2xl font-bold text-slate-900 mb-2">Le mie Prenotazioni</h1>
                <p className="text-slate-500">Gestisci tutti i servizi prenotati</p>
            </div>

            <div className="flex flex-wrap gap-2 mb-6">
                {FILTER_OPTIONS.map(({ value, label }) => (
                    <button
                        key={value}
                        onClick={() => setFilter(value)}
                        className={`text-sm px-4 py-1.5 rounded-full font-medium border transition-colors ${
                            filter === value
                                ? 'bg-indigo-600 text-white border-indigo-600'
                                : 'bg-white text-slate-600 border-slate-200 hover:border-indigo-400'
                        }`}
                    >
                        {label}
                    </button>
                ))}
            </div>

            {filtered.length === 0 ? (
                <div className="text-center py-16 text-slate-400">
                    <CalendarDays className="mx-auto mb-4 h-12 w-12" strokeWidth={1.5} />
                    <p className="text-lg">Nessuna prenotazione trovata</p>
                </div>
            ) : (
                <div>
                    {/* Weekly calendar shows only bookings that fall inside the selected seven-day window. */}
                    <WeeklyCalendar
                        weekStart={weekStart}
                        onPrevWeek={handlePrevWeek}
                        onNextWeek={handleNextWeek}
                        title="Calendario servizi prenotati"
                        subtitle="Usa le frecce per scorrere le settimane e apri una prenotazione per vedere i dettagli."
                        renderDay={(day) => {
                            const dateBookings = bookingsByDate[day.isoDate] || []

                            if (dateBookings.length === 0) {
                                return <p className="text-sm text-slate-400">Nessuna prenotazione</p>
                            }

                            return dateBookings.map((booking) => (
                                <button
                                    key={booking.id}
                                    onClick={() => navigate(`/booking/${booking.id}`)}
                                    className={`w-full overflow-hidden rounded-2xl border px-3 py-2.5 text-left transition-opacity hover:opacity-80 ${getCardClass(booking.status)}`}
                                >
                                    <Badge value={booking.status} />
                                    <p className="text-sm font-semibold text-slate-800">{formatSlotTime(booking.slotStartTime)} - {formatSlotTime(booking.slotEndTime)}</p>
                                    <p className="mt-1 text-sm text-slate-700 wrap-break-word">{booking.profileTitle}</p>
                                    <p className="mt-1 text-xs text-slate-600">Tutor: <span className="break-all">@{booking.providerUsername}</span></p>
                                    <div className="mt-2 flex items-center justify-between gap-2">
                                        <span className="text-sm font-bold text-slate-700">€{parseFloat(booking.totalAmount).toFixed(2)}</span>
                                    </div>
                                    {booking.status === 'COMPLETED' && booking.confirmationCode && (
                                        <p className="mt-2 text-[11px] font-semibold text-green-700">
                                            Codice: {booking.confirmationCode}
                                        </p>
                                    )}
                                </button>
                            ))
                        }}
                    />

                    {weekFiltered.length === 0 && (
                        <div className="mt-4 rounded-2xl border border-slate-200 bg-white px-5 py-6 text-sm text-slate-500">
                            Nessuna prenotazione nella settimana selezionata.
                        </div>
                    )}

                    {unscheduled.length > 0 && (
                        <div className="mt-4 bg-amber-50 border border-amber-200 rounded-2xl px-4 py-3 text-xs text-amber-900">
                            {unscheduled.length} prenotazioni senza data esplicita (legacy) non sono incluse nella vista settimanale.
                        </div>
                    )}
                </div>
            )}
        </div>
    )
}
