import { useEffect } from 'react'
import { useParams, useNavigate, useLocation, Link } from 'react-router-dom'
import { ArrowLeft, MapPin } from 'lucide-react'
import { useQuery } from '@tanstack/react-query'
import { profileService } from '../../services/profileService'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import Badge from '../../components/common/Badge'
import UserAvatar from '../../components/common/UserAvatar'
import toast from 'react-hot-toast'
import { getErrorMessage } from '../../lib/getErrorMessage'

export default function ProfileDetailGuestPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()

  const { data: profile, isLoading: loading, error } = useQuery({
    queryKey: ['profile-guest', id],
    queryFn: () => profileService.getById(id).then((r) => r.data),
  })

  useEffect(() => {
    if (error) toast.error(getErrorMessage(error))
  }, [error])

  if (loading) return <LoadingSpinner />
  if (!profile) return <p className="py-12 text-center text-slate-500">Profilo non trovato</p>

  return (
    <div className="space-y-6">
      {/* Back button */}
      <button
        onClick={() => navigate(-1)}
        className="inline-flex items-center gap-2 text-sm font-medium text-indigo-600 transition hover:text-indigo-700 hover:underline"
      >
        <ArrowLeft className="h-4 w-4" strokeWidth={1.5} />
        <span>Indietro</span>
      </button>

      {/* Hero banner with public profile information. */}
      <section className="page-surface overflow-hidden">
        <div className="bg-linear-to-br from-[#5B4EE8] to-[#4a3fd4] px-5 py-6 text-white sm:px-6 sm:py-8">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
            <div className="flex items-start gap-4">
              {/* Guests do not receive a userId, so the avatar falls back to the initial. */}
              <UserAvatar
                userId={undefined}
                username={profile.username}
                sizeClassName="w-16 h-16"
                textClassName="text-lg"
              />
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.2em] text-white/70">Profilo servizio</p>
                <h1 className="mt-2 text-3xl font-bold tracking-tight sm:text-4xl">{profile.title}</h1>
                <p className="mt-2 text-sm font-medium text-white/85">@{profile.username}</p>
              </div>
            </div>

            <div className="rounded-2xl border border-white/15 bg-white/10 px-4 py-3 backdrop-blur">
              <p className="text-xs text-white/70">Tariffa oraria</p>
              <p className="text-2xl font-bold text-white">€{parseFloat(profile.hourlyRate).toFixed(2)}</p>
            </div>
          </div>

          <div className="mt-6 flex flex-wrap gap-3">
            {profile.deliveryMode && <Badge value={profile.deliveryMode} />}
            {profile.address && (
              <span className="inline-flex items-center gap-1.5 rounded-full border border-white/20 bg-white/10 px-3 py-1 text-xs font-medium text-white/90 backdrop-blur">
                <MapPin className="h-4 w-4" strokeWidth={1.5} /> {profile.address}
              </span>
            )}
          </div>
        </div>

        {/* Public description block. */}
        {profile.description && (
          <div className="border-b border-slate-100 px-5 py-5 sm:px-6">
            <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-400">Descrizione</h2>
            <p className="leading-7 text-slate-600">{profile.description}</p>
          </div>
        )}

        {/* Locked sections: availability and booking are blurred until the user signs in. */}
        <div className="relative px-5 py-6 sm:px-6">
          {/* Decorative blurred preview of the hidden schedule. */}
          <div className="pointer-events-none select-none blur-sm" aria-hidden="true">
            <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-400">Disponibilità settimanale</h2>
            <div className="grid grid-cols-5 gap-2">
              {['Lun', 'Mar', 'Mer', 'Gio', 'Ven'].map((d) => (
                <div key={d} className="rounded-xl bg-slate-100 p-3 text-center">
                  <p className="text-xs font-semibold text-slate-500">{d}</p>
                  <div className="mt-2 space-y-1.5">
                    <div className="h-8 rounded-lg bg-indigo-200" />
                    <div className="h-8 rounded-lg bg-indigo-100" />
                  </div>
                </div>
              ))}
            </div>
            <div className="mt-4 grid grid-cols-3 gap-3">
              {[1, 2, 3].map((i) => (
                <div key={i} className="h-16 rounded-xl bg-slate-100" />
              ))}
            </div>
          </div>

          {/* Centered lock icon and call to action over the blurred preview. */}
          <div className="absolute inset-0 flex flex-col items-center justify-center gap-4 px-6 text-center">
            <div className="flex h-14 w-14 items-center justify-center rounded-full bg-white shadow-lg">
              <svg className="h-7 w-7 text-indigo-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round"
                  d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z"
                />
              </svg>
            </div>
            <div>
              <p className="text-base font-semibold text-slate-800">Registrati per vedere disponibilità e prenotare</p>
              <p className="mt-1 text-sm text-slate-500">
                Crea un account gratuito come cliente per accedere al calendario, alle recensioni e alla prenotazione.
              </p>
            </div>
            <div className="flex flex-wrap justify-center gap-3">
              <Link
                to="/register"
                state={{ from: location }}
                className="rounded-full bg-indigo-600 px-6 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-indigo-700"
              >
                Registrati come cliente
              </Link>
              <Link
                to="/login"
                state={{ from: location }}
                className="rounded-full border border-slate-200 bg-white px-6 py-2.5 text-sm font-semibold text-slate-700 shadow-sm transition hover:bg-slate-50"
              >
                Accedi
              </Link>
            </div>
          </div>
        </div>
      </section>
    </div>
  )
}
