// Page that lists all tutor profiles owned by the logged-in user.
// Profiles can be edited, soft-deleted (deactivated), or reactivated.
// Profile creation is capped at 5 and requires an active Stripe account.
import { useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { profileService } from '../../services/profileService'
import useAuthStore from '../../store/authStore'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import Badge from '../../components/common/Badge'
import toast from 'react-hot-toast'
import { ClipboardList } from 'lucide-react'

export default function ManageProfilesPage() {
  const navigate = useNavigate()
  const { user } = useAuthStore()
  const queryClient = useQueryClient()

  // Fetch the tutor's own profiles; cached under ['profiles', 'my'].
  const { data: profiles = [], isLoading: loading, error } = useQuery({
    queryKey: ['profiles', 'my'],
    queryFn: () => profileService.getMe().then((r) => r.data),
  })

  useEffect(() => {
    if (error) toast.error('Errore caricamento profili')
  }, [error])

  // Soft-delete: the profile is deactivated on the backend, not permanently removed.
  const handleDelete = async (id, e) => {
    // Prevent the click from bubbling to any parent card/link.
    e.stopPropagation()
    if (!window.confirm('Disattivare questo profilo?')) return
    try {
      await profileService.delete(id)
      toast.success('Profilo disattivato')
      // Invalidate the cache so the list re-fetches with the updated state.
      queryClient.invalidateQueries({ queryKey: ['profiles', 'my'] })
    } catch (err) {
      toast.error(err.response?.data?.message ?? 'Errore')
    }
  }

  const handleReactivate = async (id, e) => {
    e.stopPropagation()
    if (!window.confirm('Riattivare questo profilo?')) return
    try {
      await profileService.reactivate(id)
      toast.success('Profilo riattivato')
      queryClient.invalidateQueries({ queryKey: ['profiles', 'my'] })
    } catch (err) {
      toast.error(err.response?.data?.message ?? 'Errore durante la riattivazione')
    }
  }

  if (loading) return <LoadingSpinner />
  const stripeReady = Boolean(user?.stripeConnected)
  const activeCount = profiles.filter((p) => p.isActive).length
  const inactiveCount = profiles.length - activeCount

  return (
    <div className="space-y-6">
      <section className="page-surface overflow-hidden">
        <div className="bg-linear-to-br from-[#0F0E2A] to-[#4a3fd4] px-5 py-6 text-white sm:px-6 sm:py-8">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div className="max-w-2xl">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-white/65">Gestione servizi</p>
              <h1 className="mt-2 text-3xl font-bold tracking-tight sm:text-4xl">I miei profili</h1>
              <p className="mt-3 text-sm leading-6 text-white/80 sm:text-base">
                Crea, modifica e disattiva i tuoi servizi in modo semplice, con una vista più chiara anche da mobile.
              </p>
            </div>

            <div className="grid grid-cols-2 gap-3 sm:max-w-sm">
              <div className="rounded-2xl border border-white/10 bg-white/10 px-4 py-3 backdrop-blur">
                <p className="text-xs text-white/70">Attivi</p>
                <p className="mt-1 text-2xl font-bold">{activeCount}</p>
              </div>
              <div className="rounded-2xl border border-white/10 bg-white/10 px-4 py-3 backdrop-blur">
                <p className="text-xs text-white/70">Totali</p>
                <p className="mt-1 text-2xl font-bold">{profiles.length}</p>
              </div>
            </div>
          </div>
        </div>

        {/* Stripe warning: existing profiles remain visible but new ones cannot be created. */}
        {!stripeReady && (
          <div className="border-t border-amber-200 bg-amber-50 px-5 py-4 text-amber-900 sm:px-6">
            <p className="text-sm font-semibold">Completa Stripe prima di creare nuovi servizi</p>
            <p className="mt-1 text-sm text-amber-800">
              I profili esistenti restano visibili, ma la creazione è bloccata finché Stripe non è attivo.
            </p>
          </div>
        )}
      </section>

      <section className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-sm font-semibold uppercase tracking-wide text-slate-500">Riepilogo</p>
          {/* Show the hard cap of 5 profiles per tutor. */}
          <h2 className="text-xl font-semibold tracking-tight text-slate-900">
            Gestisci i tuoi servizi ({profiles.length}/5)
          </h2>
          <p className="text-sm text-slate-500">{inactiveCount} profili non attivi al momento</p>
        </div>
        {/* "New profile" button is hidden once the cap is reached, and disabled without Stripe. */}
        {profiles.length < 5 && (
          <Link
            to="/student/profiles/new"
            className={`inline-flex items-center justify-center rounded-xl px-4 py-3 text-sm font-semibold transition ${
              stripeReady
                ? 'bg-indigo-600 text-white hover:bg-indigo-700'
                : 'pointer-events-none bg-slate-200 text-slate-500'
            }`}
          >
            + Nuovo profilo
          </Link>
        )}
      </section>

      {profiles.length === 0 ? (
        <div className="rounded-3xl border border-dashed border-slate-300 bg-white/70 px-6 py-16 text-center shadow-sm">
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-indigo-50 text-indigo-600">
            <ClipboardList className="h-7 w-7" strokeWidth={1.5} />
          </div>
          <p className="text-lg font-semibold text-slate-900">Nessun profilo ancora</p>
          <p className="mt-2 text-sm text-slate-500">Crea il tuo primo servizio per iniziare a ricevere prenotazioni.</p>
          <Link to="/student/profiles/new" className="primary-button mt-6">
            Crea il tuo primo profilo
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">
          {profiles.map((p) => (
            <div
              key={p.id}
              className={`group rounded-3xl border bg-white/80 p-5 shadow-sm transition hover:-translate-y-0.5 hover:shadow-lg ${
                p.isActive ? 'border-slate-200' : 'border-slate-200 opacity-70'
              }`}
            >
              <div className="mb-4 flex items-start justify-between gap-3">
                <div>
                  <h3 className="text-lg font-semibold leading-tight text-slate-900">{p.title}</h3>
                  <p className="mt-1 text-sm text-slate-500">€{parseFloat(p.hourlyRate).toFixed(2)}/ora</p>
                </div>
                <Badge value={p.deliveryMode} />
              </div>

              {p.description && <p className="line-clamp-3 text-sm leading-6 text-slate-500">{p.description}</p>}

              <div className="mt-4 flex flex-wrap gap-2">
                <span className={`rounded-full px-3 py-1 text-xs font-semibold ${p.isActive ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-600'}`}>
                  {p.isActive ? 'Attivo' : 'Disattivato'}
                </span>
                <span className="rounded-full bg-indigo-50 px-3 py-1 text-xs font-semibold text-indigo-700">
                  Servizio #{p.id}
                </span>
              </div>

              <div className="mt-5 flex flex-wrap gap-2 border-t border-slate-100 pt-4">
                <button
                  onClick={() => navigate(`/student/profiles/${p.id}/edit`)}
                  className="secondary-button px-3 py-2 text-sm"
                >
                  Modifica
                </button>
                {/* Toggle between deactivate and reactivate based on current state. */}
                {p.isActive ? (
                  <button
                    onClick={(e) => handleDelete(p.id, e)}
                    className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm font-semibold text-red-700 transition hover:bg-red-100"
                  >
                    Disattiva
                  </button>
                ) : (
                  <button
                    onClick={(e) => handleReactivate(p.id, e)}
                    className="rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm font-semibold text-emerald-700 transition hover:bg-emerald-100"
                  >
                    Riattiva
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
