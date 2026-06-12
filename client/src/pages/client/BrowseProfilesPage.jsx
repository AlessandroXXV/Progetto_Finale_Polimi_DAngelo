import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { profileService } from '../../services/profileService'
import Badge from '../../components/common/Badge'
import UserAvatar from '../../components/common/UserAvatar'
import ProfileCardSkeleton from '../../components/skeleton/ProfileCardSkeleton'
import toast from 'react-hot-toast'
import useAuthStore from '../../store/authStore'
import { getErrorMessage } from '../../lib/getErrorMessage'
import { Search, MapPin, ArrowRight } from 'lucide-react'

const DAY_OPTIONS = [
  { value: 'ALL', label: 'Qualsiasi giorno' },
  { value: '0', label: 'Lunedì' },
  { value: '1', label: 'Martedì' },
  { value: '2', label: 'Mercoledì' },
  { value: '3', label: 'Giovedì' },
  { value: '4', label: 'Venerdì' },
  { value: '5', label: 'Sabato' },
  { value: '6', label: 'Domenica' },
]

const MODE_OPTIONS = [
  { value: 'ALL', label: 'Tutti i metodi' },
  { value: 'ONLINE', label: 'Online' },
  { value: 'IN_PERSON', label: 'In presenza' },
  { value: 'HYBRID', label: 'Ibrido' },
]

export default function BrowseProfilesPage() {
  const [filtersOpen, setFiltersOpen] = useState(false)
  const [category, setCategory] = useState('')
  const [maxRate, setMaxRate] = useState('')
  const [dayOfWeek, setDayOfWeek] = useState('ALL')
  const [startTime, setStartTime] = useState('')
  const [endTime, setEndTime] = useState('')
  const [modeFilter, setModeFilter] = useState('ALL')
  const { user } = useAuthStore()
  const location = useLocation()
  const navigate = useNavigate()

  const hasInvalidTimeRange = Boolean(startTime && endTime && endTime <= startTime)
  // Smoothly jump to the services grid from the guest hero CTA.
  const scrollToServices = () => {
    document.getElementById('services')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
  const activeFiltersCount = [
    category.trim() !== '',
    maxRate !== '',
    dayOfWeek !== 'ALL',
    startTime !== '',
    endTime !== '',
    modeFilter !== 'ALL',
  ].filter(Boolean).length

  const clearFilters = () => {
    setCategory('')
    setMaxRate('')
    setDayOfWeek('ALL')
    setStartTime('')
    setEndTime('')
    setModeFilter('ALL')
  }

  const queryParams = useMemo(() => {
    // Only send filters that have an actual value to keep the query string compact.
    const p = {}
    if (category.trim()) p.category = category.trim()
    if (maxRate !== '') p.maxRate = maxRate
    if (dayOfWeek !== 'ALL') p.dayOfWeek = Number(dayOfWeek)
    if (startTime) p.startTime = startTime
    if (endTime) p.endTime = endTime
    return p
  }, [category, maxRate, dayOfWeek, startTime, endTime])

  const { data: profiles = [], isLoading: loading, error } = useQuery({
    queryKey: ['profiles', queryParams, !!user],
    queryFn: () => (user ? profileService.getAllAuthenticated(queryParams) : profileService.getAll(queryParams)).then((r) => r.data),
    enabled: !hasInvalidTimeRange,
    placeholderData: (prev) => prev,
    staleTime: 30_000,
  })

  useEffect(() => {
    if (error) toast.error(getErrorMessage(error))
  }, [error])

  const filtered = useMemo(
    () => profiles.filter((p) => p.isActive).filter((p) => modeFilter === 'ALL' || p.deliveryMode === modeFilter),
    [profiles, modeFilter]
  )

  const hasFiltersApplied = activeFiltersCount > 0

  const isGuest = !user

  return (
    <div className="space-y-6">
      <section className="page-surface overflow-hidden">
        {/* Guest hero: encourages login/registration while still allowing browsing. */}
        {isGuest && (
          <div className="border-b border-white/70 bg-linear-to-br from-[#0F0E2A] to-[#4a3fd4] px-5 py-6 text-white sm:px-6 sm:py-8 lg:px-8 lg:py-10">
            <div className="grid gap-6 lg:grid-cols-[1.15fr_0.85fr] lg:items-center">
              <div className="max-w-2xl">
                <p className="text-xs font-semibold uppercase tracking-[0.25em] text-white/60">Benvenuto in Eably</p>
                <h1 className="mt-3 text-3xl font-bold tracking-tight sm:text-4xl lg:text-5xl">
                  Trova il servizio giusto, anche da ospite.
                </h1>
                <p className="mt-4 max-w-xl text-sm leading-7 text-white/80 sm:text-base">
                  Esplora i profili disponibili, confronta tariffe e dettagli, e quando sei pronto accedi o registrati
                  per prenotare e gestire tutto in sicurezza.
                </p>

                <div className="mt-6 flex flex-wrap gap-3">
                  <Link
                    to="/login"
                    state={{ from: location }}
                    className="rounded-full bg-white px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-slate-100"
                  >
                    Accedi
                  </Link>
                  <Link
                    to="/register"
                    className="rounded-full border border-white/20 bg-white/10 px-5 py-3 text-sm font-semibold text-white transition hover:bg-white/15"
                  >
                    Registrati
                  </Link>
                  <button
                    type="button"
                    onClick={scrollToServices}
                    className="rounded-full border border-white/15 px-5 py-3 text-sm font-semibold text-white/90 transition hover:bg-white/10"
                  >
                    Vedi i servizi
                  </button>
                </div>
              </div>

              <div className="grid gap-3 sm:grid-cols-3 lg:grid-cols-1 xl:grid-cols-3">
                <div className="rounded-2xl border border-white/15 bg-white/10 p-4 backdrop-blur">
                  <p className="text-xs uppercase tracking-wide text-white/60">Catalogo pubblico</p>
                  <p className="mt-2 text-lg font-semibold">Sfoglia senza account</p>
                </div>
                <div className="rounded-2xl border border-white/15 bg-white/10 p-4 backdrop-blur">
                  <p className="text-xs uppercase tracking-wide text-white/60">Scelta rapida</p>
                  <p className="mt-2 text-lg font-semibold">Filtra per prezzo e disponibilità</p>
                </div>
                <div className="rounded-2xl border border-white/15 bg-white/10 p-4 backdrop-blur">
                  <p className="text-xs uppercase tracking-wide text-white/60">Accesso completo</p>
                  <p className="mt-2 text-lg font-semibold">Prenota dopo il login</p>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Authenticated hero: focuses on search metrics and discovery. */}
        {!isGuest && (
          <div className="bg-linear-to-br from-[#5B4EE8] to-[#4a3fd4] px-5 py-6 text-white sm:px-6 sm:py-8">
            <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
              <div className="max-w-2xl">
                <p className="text-xs font-semibold uppercase tracking-[0.2em] text-white/75">Esplora i servizi</p>
                <h1 className="mt-2 text-3xl font-bold tracking-tight sm:text-4xl">
                  Cerca il tutor o il servizio giusto per te
                </h1>
                <p className="mt-3 text-sm leading-6 text-white/85 sm:text-base">
                  Filtra per prezzo, disponibilità e modalità di erogazione per trovare subito la soluzione migliore.
                </p>
              </div>

              <div className="grid grid-cols-2 gap-3 sm:max-w-sm">
                <div className="rounded-2xl border border-white/15 bg-white/10 px-4 py-3 backdrop-blur">
                  <p className="text-xs text-white/70">Risultati</p>
                  <p className="mt-1 text-2xl font-bold">{filtered.length}</p>
                </div>
                <div className="rounded-2xl border border-white/15 bg-white/10 px-4 py-3 backdrop-blur">
                  <p className="text-xs text-white/70">Filtri attivi</p>
                  <p className="mt-1 text-2xl font-bold">{activeFiltersCount}</p>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Filter chips and quick actions for mobile and desktop. */}
        <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-4 sm:px-6">
          <div className="flex flex-wrap items-center gap-2">
            <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700">Aggiornamento live</span>
            {isGuest && (
              <span className="rounded-full bg-violet-50 px-3 py-1 text-xs font-semibold text-violet-700">
                Modalità ospite
              </span>
            )}
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setFiltersOpen((v) => !v)}
              className="secondary-button px-4 py-2 text-sm sm:hidden"
            >
              {filtersOpen ? 'Nascondi filtri' : 'Mostra filtri'}
            </button>
            <button
              type="button"
              onClick={clearFilters}
              disabled={!hasFiltersApplied}
              className="secondary-button px-4 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-50"
            >
              Rimuovi filtri
            </button>
          </div>
        </div>
      </section>

      {/* Search filters panel. Hidden on mobile until the user expands it. */}
      <section className={`${filtersOpen ? 'block' : 'hidden'} sm:block`}>
        <div className="panel p-4 sm:p-5">
          <div className="mb-4 flex items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-slate-900">Filtri ricerca</h2>
              <p className="text-sm text-slate-500">Affina la ricerca in pochi secondi</p>
            </div>
            <p className="text-sm text-slate-600">
              Attivi: <span className="font-semibold text-indigo-700">{activeFiltersCount}</span>
            </p>
          </div>

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
            <div>
              <label className="field-label">Categoria</label>
              <input
                type="text"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                placeholder="Es. matematica, inglese…"
                className="field"
              />
            </div>

            <div>
              <label className="field-label">Tariffa massima (€)</label>
              <input
                type="number"
                min="0"
                step="0.01"
                value={maxRate}
                onChange={(e) => setMaxRate(e.target.value)}
                placeholder="Es. 25"
                className="field"
              />
            </div>

            <div>
              <label className="field-label">Giorno</label>
              <select value={dayOfWeek} onChange={(e) => setDayOfWeek(e.target.value)} className="field">
                {DAY_OPTIONS.map((day) => (
                  <option key={day.value} value={day.value}>
                    {day.label}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="field-label">Modalità</label>
              <select value={modeFilter} onChange={(e) => setModeFilter(e.target.value)} className="field">
                {MODE_OPTIONS.map((mode) => (
                  <option key={mode.value} value={mode.value}>
                    {mode.label}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="field-label">Dalle</label>
              <input
                type="time"
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                className="field"
                aria-label="Orario inizio disponibilita"
              />
            </div>

            <div>
              <label className="field-label">Alle</label>
              <input
                type="time"
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
                className="field"
                aria-label="Orario fine disponibilita"
              />
            </div>
          </div>

          {hasInvalidTimeRange && (
            <p className="mt-3 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              L'orario finale deve essere successivo all'orario iniziale.
            </p>
          )}
        </div>
      </section>

      {/* Results area: skeletons while loading, empty state when nothing matches, grid otherwise. */}
      <section id="services" className="scroll-mt-6">
        {loading ? (
          <div className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">
            {Array.from({ length: 6 }, (_, i) => (
              <ProfileCardSkeleton key={i} />
            ))}
          </div>
        ) : filtered.length === 0 ? (
          <section className="panel px-6 py-16 text-center">
            <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-indigo-50 text-indigo-600">
              <Search className="h-7 w-7" strokeWidth={1.5} />
            </div>
            <h2 className="text-xl font-semibold text-slate-900">Nessun profilo con questi filtri</h2>
            <p className="mt-2 text-sm text-slate-500">
              Allarga i criteri o cambia fascia oraria: il match giusto potrebbe essere a un click.
            </p>
            <button type="button" onClick={clearFilters} className="primary-button mt-6">
              Reimposta filtri
            </button>
          </section>
        ) : (
          <section className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">
            {filtered.map((p) => (
              <button
                type="button"
                key={p.id}
                onClick={() => navigate(`/profile/${p.id}`)}
                className="group panel overflow-hidden p-5 text-left transition hover:-translate-y-0.5 hover:shadow-lg"
                aria-label={`Apri dettagli servizio ${p.title}`}
              >
                <div className="mb-4 flex items-start justify-between gap-3">
                  <div className="flex min-w-0 items-start gap-3">
                    <UserAvatar userId={p.userId} username={p.username} sizeClassName="w-12 h-12" textClassName="text-sm" />
                    <div className="min-w-0">
                      <h3 className="line-clamp-2 text-lg font-semibold leading-tight text-slate-900">{p.title}</h3>
                      <p className="mt-1 text-sm font-medium text-indigo-600">@{p.username}</p>
                    </div>
                  </div>
                  <Badge value={p.deliveryMode} />
                </div>

                {p.description && <p className="mb-4 line-clamp-3 text-sm leading-6 text-slate-500">{p.description}</p>}

                <div className="mb-4 flex flex-wrap gap-2">
                  {p.address && (
                    <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
                      <MapPin className="inline h-3.5 w-3.5 align-text-bottom" strokeWidth={1.5} /> {p.address}
                    </span>
                  )}
                  {p.isActive && (
                    <span className="rounded-full bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-700">
                      Disponibile
                    </span>
                  )}
                </div>

                <div className="flex items-center justify-between border-t border-slate-100 pt-4">
                  <div>
                    <p className="text-xs uppercase tracking-wide text-slate-400">Tariffa oraria</p>
                    <p className="text-2xl font-bold text-indigo-700">
                      €{Number(p.hourlyRate).toFixed(2)}
                      <span className="text-sm font-normal text-slate-400">/ora</span>
                    </p>
                  </div>
                  <span className="inline-flex items-center gap-1.5 rounded-full bg-indigo-50 px-4 py-2 text-xs font-semibold text-indigo-700 transition group-hover:bg-indigo-100">
                    Apri dettagli
                    <ArrowRight className="h-4 w-4" strokeWidth={1.5} />
                  </span>
                </div>
              </button>
            ))}
          </section>
        )}
      </section>
    </div>
  )
}
