// Main dashboard for the tutor (student) role.
// Aggregates key metrics in a single query and displays them as stat cards,
// quick-action shortcuts, and a performance summary panel.
import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { profileService } from '../../services/profileService'
import { availabilityService } from '../../services/availabilityService'
import { bookingService } from '../../services/bookingService'
import useAuthStore from '../../store/authStore'
import DashboardStatsSkeleton from '../../components/skeleton/DashboardStatsSkeleton'
import toast from 'react-hot-toast'
import { ClipboardList, CalendarDays, Calendar, CheckCircle2, Trophy, Wallet, CreditCard } from 'lucide-react'

export default function StudentDashboardPage() {
  const { user } = useAuthStore()

  // Fetch all data needed for the dashboard in parallel and derive stats in the queryFn.
  // This keeps the component free of separate loading states for each resource.
  const { data: stats, isLoading: loading, error } = useQuery({
    queryKey: ['dashboard', 'student'],
    queryFn: async () => {
      const [p, a, b] = await Promise.all([
        profileService.getMe(),
        availabilityService.getMyCount(),
        bookingService.getMy(),
      ])
      const bookings = b.data
      const activeProfiles = p.data.filter((profile) => profile.isActive)
      return {
        profileCount: activeProfiles.length,
        slotCount: a.data.count,
        totalBookings: bookings.length,
        confirmedBookings: bookings.filter((x) => x.status === 'CONFIRMED').length,
        completedBookings: bookings.filter((x) => x.status === 'COMPLETED').length,
        // Sum earnings only from completed bookings (payment has been released).
        earnings: bookings
          .filter((x) => x.status === 'COMPLETED')
          .reduce((sum, x) => sum + parseFloat(x.totalAmount), 0),
      }
    },
  })

  useEffect(() => {
    if (error) toast.error('Errore caricamento dashboard')
  }, [error])

  if (loading) return <DashboardStatsSkeleton />
  if (!stats) return <div className="text-center p-8 text-slate-500">Impossibile caricare le statistiche.</div>

  const stripeReady = Boolean(user?.stripeConnected)

  // Stat card definitions; each card links to the corresponding section.
  const cards = [
    { label: 'Profili attivi', value: stats.profileCount, to: '/student/profiles', icon: ClipboardList, color: 'from-indigo-50 to-indigo-100 border-indigo-200 text-indigo-700' },
    { label: 'Slot disponibili', value: stats.slotCount, to: '/student/availability', icon: CalendarDays, color: 'from-teal-50 to-teal-100 border-teal-200 text-teal-700' },
    { label: 'Prenotazioni totali', value: stats.totalBookings, to: '/student/bookings', icon: Calendar, color: 'from-amber-50 to-amber-100 border-amber-200 text-amber-700' },
    { label: 'In corso', value: stats.confirmedBookings, to: '/student/bookings', icon: CheckCircle2, color: 'from-green-50 to-green-100 border-green-200 text-green-700' },
    { label: 'Completate', value: stats.completedBookings, to: '/student/bookings', icon: Trophy, color: 'from-purple-50 to-purple-100 border-purple-200 text-purple-700' },
    { label: 'Guadagni totali', value: `€${stats.earnings.toFixed(2)}`, to: '/student/stripe', icon: Wallet, color: 'from-emerald-50 to-emerald-100 border-emerald-200 text-emerald-700' },
  ]

  return (
    <div className="space-y-6">
      <section className="page-surface overflow-hidden">
        <div className="bg-linear-to-br from-[#5B4EE8] to-[#4a3fd4] px-5 py-6 text-white sm:px-6 sm:py-8">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div className="max-w-2xl">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-white/70">Dashboard tutor / studente</p>
              <h1 className="mt-2 text-3xl font-bold tracking-tight sm:text-4xl">La tua dashboard</h1>
              <p className="mt-3 text-sm leading-6 text-white/85 sm:text-base">
                Tieni sotto controllo profili, disponibilità, prenotazioni e guadagni in un unico posto.
              </p>
            </div>

            <div className="grid grid-cols-2 gap-3 sm:max-w-sm">
              <div className="rounded-2xl border border-white/15 bg-white/10 px-4 py-3 backdrop-blur">
                <p className="text-xs text-white/70">Profili attivi</p>
                <p className="mt-1 text-2xl font-bold">{stats.profileCount}</p>
              </div>
              <div className="rounded-2xl border border-white/15 bg-white/10 px-4 py-3 backdrop-blur">
                <p className="text-xs text-white/70">Slot totali</p>
                <p className="mt-1 text-2xl font-bold">{stats.slotCount}</p>
              </div>
            </div>
          </div>
        </div>

        {/* Stripe warning banner: blocks creation of profiles and slots. */}
        {!stripeReady && (
          <div className="border-t border-amber-200 bg-amber-50 px-5 py-4 text-amber-900 sm:px-6">
            <p className="text-sm font-semibold">Completa Stripe per sbloccare il flusso completo</p>
            <p className="mt-1 text-sm text-amber-800">
              Senza Stripe non puoi creare nuovi servizi o disponibilità.
            </p>
          </div>
        )}
      </section>

      <section>
        <div className="mb-4 flex items-end justify-between gap-3">
          <div>
            <h2 className="text-xl font-semibold tracking-tight text-slate-900">Panoramica rapida</h2>
            <p className="text-sm text-slate-500">Statistiche essenziali della tua attività</p>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4 md:grid-cols-3 xl:grid-cols-6">
          {cards.map((c) => (
            <Link
              key={c.label}
              to={c.to}
              className={`group rounded-2xl border bg-linear-to-br p-4 shadow-sm transition hover:-translate-y-0.5 hover:shadow-lg ${c.color}`}
            >
              <div className="mb-3 flex items-start justify-between gap-2">
                <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/70 shadow-sm">
                  <c.icon className="h-5 w-5" strokeWidth={1.5} />
                </span>
                <span className="text-xs font-semibold uppercase tracking-wide opacity-70">Apri</span>
              </div>
              <p className="text-2xl font-bold text-slate-900">{c.value}</p>
              <p className="mt-1 text-sm font-medium text-slate-600">{c.label}</p>
            </Link>
          ))}
        </div>
      </section>

      <section className="grid gap-6 lg:grid-cols-[1.15fr_0.85fr]">
        <div className="rounded-3xl border border-white/70 bg-white/80 p-5 shadow-sm backdrop-blur sm:p-6">
          <div className="mb-4 flex items-start justify-between gap-4">
            <div>
              <h2 className="text-xl font-semibold tracking-tight text-slate-900">Azioni rapide</h2>
              <p className="text-sm text-slate-500">Crea nuovi contenuti o apri i moduli più usati</p>
            </div>
          </div>

          <div className="grid gap-3 sm:grid-cols-3">
            {/* Redirect to Stripe setup if not yet connected, otherwise go directly to profile creation. */}
            <Link
              to={stripeReady ? '/student/profiles/new' : '/student/stripe'}
              className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-center transition hover:border-indigo-200 hover:bg-indigo-50/60"
            >
              <ClipboardList className="mx-auto h-7 w-7 text-indigo-600" strokeWidth={1.5} />
              <p className="mt-2 text-sm font-semibold text-slate-900">Nuovo profilo</p>
              <p className="mt-1 text-xs text-slate-500">Crea un nuovo servizio</p>
            </Link>
            <Link
              to={stripeReady ? '/student/availability' : '/student/stripe'}
              className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-center transition hover:border-teal-200 hover:bg-teal-50/60"
            >
              <CalendarDays className="mx-auto h-7 w-7 text-teal-600" strokeWidth={1.5} />
              <p className="mt-2 text-sm font-semibold text-slate-900">Aggiungi slot</p>
              <p className="mt-1 text-xs text-slate-500">Aperture disponibili</p>
            </Link>
            <Link
              to="/student/stripe"
              className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-center transition hover:border-emerald-200 hover:bg-emerald-50/60"
            >
              <CreditCard className="mx-auto h-7 w-7 text-emerald-600" strokeWidth={1.5} />
              <p className="mt-2 text-sm font-semibold text-slate-900">Pagamenti</p>
              <p className="mt-1 text-xs text-slate-500">Stripe Connect e payout</p>
            </Link>
          </div>

          {!stripeReady && (
            <div className="mt-4 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
              Completa Stripe per sbloccare la creazione di servizi e disponibilità.
            </div>
          )}
        </div>

        {/* Performance summary: mirrors the stat cards in a more readable list format. */}
        <div className="rounded-3xl border border-white/70 bg-white/80 p-5 shadow-sm backdrop-blur sm:p-6">
          <h2 className="text-xl font-semibold tracking-tight text-slate-900">Riepilogo performance</h2>
          <p className="mt-1 text-sm text-slate-500">Stato rapido delle prenotazioni</p>

          <div className="mt-5 space-y-3">
            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <div className="flex items-center justify-between gap-3">
                <span className="text-sm font-medium text-slate-600">Prenotazioni totali</span>
                <span className="text-xl font-bold text-slate-900">{stats.totalBookings}</span>
              </div>
            </div>
            <div className="rounded-2xl border border-slate-200 bg-green-50 p-4">
              <div className="flex items-center justify-between gap-3">
                <span className="text-sm font-medium text-slate-600">In corso</span>
                <span className="text-xl font-bold text-green-700">{stats.confirmedBookings}</span>
              </div>
            </div>
            <div className="rounded-2xl border border-slate-200 bg-purple-50 p-4">
              <div className="flex items-center justify-between gap-3">
                <span className="text-sm font-medium text-slate-600">Completate</span>
                <span className="text-xl font-bold text-purple-700">{stats.completedBookings}</span>
              </div>
            </div>
            <div className="rounded-2xl border border-slate-200 bg-emerald-50 p-4">
              <div className="flex items-center justify-between gap-3">
                <span className="text-sm font-medium text-slate-600">Guadagni</span>
                <span className="text-xl font-bold text-emerald-700">€{stats.earnings.toFixed(2)}</span>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  )
}
