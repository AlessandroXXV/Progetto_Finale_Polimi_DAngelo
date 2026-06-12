// Admin-only page that provides a centralised view of all users.
// Displays aggregate stats, a filterable/searchable user list, and a side-panel
// with full details and moderation actions for the selected user.
import { useCallback, useEffect, useMemo, useState } from 'react'
import toast from 'react-hot-toast'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import Badge from '../../components/common/Badge'
import { adminService } from '../../services/adminService'

const initialFilters = { role: 'ALL', verified: 'ALL', active: 'ALL', query: '' }

// Small reusable pill that renders a green/red label based on a boolean value.
// trueClass/falseClass allow the colour scheme to be overridden per use case.
function BooleanPill({
  value,
  trueLabel = 'Sì',
  falseLabel = 'No',
  trueClass = 'bg-green-100 text-green-800',
  falseClass = 'bg-red-100 text-red-800',
}) {
  const isTrue = Boolean(value)
  return (
    <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-medium ${isTrue ? trueClass : falseClass}`}>
      {isTrue ? trueLabel : falseLabel}
    </span>
  )
}

export default function AdminDashboardPage() {
  const [stats, setStats] = useState(null)
  const [users, setUsers] = useState([])
  // Full detail object for the user currently selected in the side-panel
  const [userDetails, setUserDetails] = useState(null)
  const [loading, setLoading] = useState(true)
  const [detailLoading, setDetailLoading] = useState(false)
  // Tracks which user ID has a pending moderation action to disable its buttons
  const [actionLoading, setActionLoading] = useState(null)
  const [filters, setFilters] = useState(initialFilters)

  const loadUserDetails = useCallback(async (userId) => {
    setDetailLoading(true)
    try {
      const { data } = await adminService.getUser(userId)
      setUserDetails(data)
    } catch {
      toast.error('Errore caricamento dettaglio utente')
    } finally {
      setDetailLoading(false)
    }
  }, [])

  // Loads both the summary stats and the full user list in parallel,
  // then auto-selects the first user in the detail panel.
  const loadDashboard = useCallback(async () => {
    setLoading(true)
    try {
      const [statsRes, usersRes] = await Promise.all([adminService.getStats(), adminService.getUsers()])
      setStats(statsRes.data)
      setUsers(usersRes.data)
      if (usersRes.data.length > 0) {
        await loadUserDetails(usersRes.data[0].id)
      } else {
        setUserDetails(null)
      }
    } catch {
      toast.error('Errore caricamento dashboard admin')
    } finally {
      setLoading(false)
    }
  }, [loadUserDetails])

  useEffect(() => {
    loadDashboard()
  }, [loadDashboard])

  // Client-side filtering — avoids an extra network round-trip for each filter change
  const filteredUsers = useMemo(() => {
    return users.filter((user) => {
      const matchesRole = filters.role === 'ALL' || user.role === filters.role
      const matchesVerified =
        filters.verified === 'ALL' ||
        (filters.verified === 'VERIFIED' ? user.isVerified : !user.isVerified)
      const matchesActive =
        filters.active === 'ALL' ||
        (filters.active === 'ACTIVE' ? user.isActive : !user.isActive)
      // Search across username, email and full name with a single lowercase pass
      const haystack = `${user.username} ${user.email} ${user.fullName ?? ''}`.toLowerCase()
      const matchesQuery = !filters.query.trim() || haystack.includes(filters.query.trim().toLowerCase())
      return matchesRole && matchesVerified && matchesActive && matchesQuery
    })
  }, [users, filters])

  const selectedId = userDetails?.id ?? null

  // After any moderation action, refresh both the stats bar and the user list,
  // and re-fetch the detail panel only if the acted-on user is still selected.
  const refreshAfterAction = async (userId) => {
    await Promise.all([
      adminService.getStats().then((r) => setStats(r.data)),
      adminService.getUsers().then((r) => setUsers(r.data)),
    ])
    if (selectedId === userId) {
      await loadUserDetails(userId)
    }
  }

  // Generic action wrapper: sets loading state, calls the provided async action,
  // then refreshes the UI. Decouples the button handlers from the API calls.
  const handleAction = async (userId, action) => {
    setActionLoading(userId)
    try {
      await action()
      await refreshAfterAction(userId)
    } catch (err) {
      toast.error(err.response?.data?.message ?? 'Operazione non riuscita')
    } finally {
      setActionLoading(null)
    }
  }

  const selectedUser = userDetails
  // Stripe details are only relevant for STUDENT accounts
  const showStripeSection = selectedUser?.role === 'STUDENT'

  if (loading) return <LoadingSpinner />

  // Derive counters from the local user list (stats endpoint may lag behind)
  const totalUsers = stats?.totalUsers ?? users.length
  const verifiedUsers = users.filter((u) => u.isVerified).length
  const inactiveUsers = users.filter((u) => !u.isActive).length
  const stripeUsers = users.filter((u) => u.stripeConnected).length

  return (
    <div className="space-y-6">
      {/* Hero banner */}
      <section className="page-surface overflow-hidden">
        <div className="bg-linear-to-br from-slate-950 via-violet-950 to-indigo-900 px-5 py-6 text-white sm:px-6 sm:py-8">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div className="max-w-2xl">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-white/65">Pannello amministratore</p>
              <h1 className="mt-2 text-3xl font-bold tracking-tight sm:text-4xl">Admin dashboard</h1>
              <p className="mt-3 text-sm leading-6 text-white/80 sm:text-base">
                Gestisci utenti, verifiche e stato account con una vista centralizzata e ottimizzata per desktop e mobile.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Summary stat cards */}
      <section className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {[
          { label: 'Utenti totali', value: totalUsers },
          { label: 'Verificati', value: verifiedUsers },
          { label: 'Sospesi', value: inactiveUsers },
          { label: 'Stripe collegati', value: stripeUsers },
        ].map((card) => (
          <div key={card.label} className="rounded-2xl border border-slate-200 bg-white/80 p-4 shadow-sm backdrop-blur">
            <p className="text-sm text-slate-500">{card.label}</p>
            <p className="mt-2 text-2xl font-bold text-slate-900">{card.value}</p>
          </div>
        ))}
      </section>

      {/* Two-column layout: user list on the left, detail panel on the right.
          The grid ratio (1.25fr / 0.95fr) gives slightly more space to the list. */}
      <section className="grid gap-6 xl:grid-cols-[1.25fr_0.95fr]">
        <div className="space-y-4">
          <div className="rounded-3xl border border-slate-200 bg-white/80 p-4 shadow-sm backdrop-blur sm:p-5">
            {/* Filter bar */}
            <div className="mb-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
              <select
                value={filters.role}
                onChange={(e) => setFilters((f) => ({ ...f, role: e.target.value }))}
                className="field"
              >
                <option value="ALL">Tutti i ruoli</option>
                <option value="CLIENT">CLIENT</option>
                <option value="STUDENT">STUDENT</option>
                <option value="ADMIN">ADMIN</option>
              </select>
              <select
                value={filters.verified}
                onChange={(e) => setFilters((f) => ({ ...f, verified: e.target.value }))}
                className="field"
              >
                <option value="ALL">Verifica: tutti</option>
                <option value="VERIFIED">Verificati</option>
                <option value="UNVERIFIED">Non verificati</option>
              </select>
              <select
                value={filters.active}
                onChange={(e) => setFilters((f) => ({ ...f, active: e.target.value }))}
                className="field"
              >
                <option value="ALL">Stato: tutti</option>
                <option value="ACTIVE">Attivi</option>
                <option value="SUSPENDED">Sospesi</option>
              </select>
              <input
                value={filters.query}
                onChange={(e) => setFilters((f) => ({ ...f, query: e.target.value }))}
                placeholder="Cerca username, email o nome"
                className="field"
              />
            </div>

            {/* Card-based list shown on small screens (hidden on lg+) */}
            <div className="space-y-3 lg:hidden">
              {filteredUsers.length === 0 && (
                <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-8 text-center text-slate-500">
                  Nessun utente trovato con i filtri attivi.
                </div>
              )}

              {filteredUsers.map((user) => (
                <div key={user.id} className={`rounded-2xl border p-4 ${selectedId === user.id ? 'border-indigo-300 bg-indigo-50/50' : 'border-slate-200 bg-white'}`}>
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="font-semibold text-slate-900">{user.fullName || user.username}</p>
                      <p className="text-xs text-slate-500">@{user.username} · {user.email}</p>
                    </div>
                    <Badge value={user.role} />
                  </div>

                  <div className="mt-3 flex flex-wrap gap-2">
                    <BooleanPill value={user.isVerified} trueLabel="Verificato" falseLabel="Non verificato" />
                    <BooleanPill value={user.isActive} trueLabel="Attivo" falseLabel="Sospeso" />
                    <BooleanPill value={user.stripeConnected} trueLabel="Stripe OK" falseLabel="No Stripe" />
                  </div>

                  <div className="mt-4 flex flex-wrap gap-2">
                    <button type="button" onClick={() => loadUserDetails(user.id)} className="primary-button px-3 py-2 text-xs">
                      Dettagli
                    </button>
                    {user.isVerified ? (
                      <button
                        type="button"
                        onClick={() => handleAction(user.id, () => adminService.unverifyUser(user.id))}
                        disabled={actionLoading === user.id}
                        className="rounded-xl bg-yellow-100 px-3 py-2 text-xs font-semibold text-yellow-800 hover:bg-yellow-200 disabled:opacity-50"
                      >
                        {actionLoading === user.id ? '...' : 'Rimuovi verifica'}
                      </button>
                    ) : (
                      <button
                        type="button"
                        onClick={() => handleAction(user.id, () => adminService.verifyUser(user.id))}
                        disabled={actionLoading === user.id}
                        className="rounded-xl bg-green-600 px-3 py-2 text-xs font-semibold text-white hover:bg-green-700 disabled:opacity-50"
                      >
                        {actionLoading === user.id ? '...' : 'Approva verifica'}
                      </button>
                    )}
                    {user.isActive ? (
                      <button
                        type="button"
                        onClick={() => handleAction(user.id, () => adminService.suspendUser(user.id))}
                        disabled={actionLoading === user.id}
                        className="rounded-xl bg-red-100 px-3 py-2 text-xs font-semibold text-red-700 hover:bg-red-200 disabled:opacity-50"
                      >
                        {actionLoading === user.id ? '...' : 'Sospendi'}
                      </button>
                    ) : (
                      <button
                        type="button"
                        onClick={() => handleAction(user.id, () => adminService.activateUser(user.id))}
                        disabled={actionLoading === user.id}
                        className="rounded-xl bg-emerald-600 px-3 py-2 text-xs font-semibold text-white hover:bg-emerald-700 disabled:opacity-50"
                      >
                        {actionLoading === user.id ? '...' : 'Riattiva'}
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>

            {/* Full table shown on large screens (hidden below lg) */}
            <div className="hidden overflow-hidden rounded-2xl border border-slate-200 lg:block">
              <table className="min-w-full divide-y divide-slate-200 text-sm">
                <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500">
                  <tr>
                    <th className="px-4 py-3">Utente</th>
                    <th className="px-4 py-3">Ruolo</th>
                    <th className="px-4 py-3">Verifica</th>
                    <th className="px-4 py-3">Account</th>
                    <th className="px-4 py-3">Stripe</th>
                    <th className="px-4 py-3 text-right">Azioni</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 bg-white">
                  {filteredUsers.map((user) => (
                    <tr key={user.id} className={`hover:bg-slate-50 ${selectedId === user.id ? 'bg-indigo-50/40' : ''}`}>
                      <td className="px-4 py-3">
                        <p className="font-semibold text-slate-800">{user.fullName || user.username}</p>
                        <p className="text-xs text-slate-500">@{user.username} · {user.email}</p>
                      </td>
                      <td className="px-4 py-3"><Badge value={user.role} /></td>
                      <td className="px-4 py-3"><BooleanPill value={user.isVerified} trueLabel="Verificato" falseLabel="Non verificato" /></td>
                      <td className="px-4 py-3"><BooleanPill value={user.isActive} trueLabel="Attivo" falseLabel="Sospeso" /></td>
                      <td className="px-4 py-3"><BooleanPill value={user.stripeConnected} trueLabel="Connesso" falseLabel="No" /></td>
                      <td className="px-4 py-3 text-right">
                        <div className="flex flex-wrap justify-end gap-2">
                          <button type="button" onClick={() => loadUserDetails(user.id)} className="rounded-lg bg-indigo-600 px-3 py-2 text-xs font-medium text-white hover:bg-indigo-700">Dettagli</button>
                          {user.isVerified ? (
                            <button type="button" onClick={() => handleAction(user.id, () => adminService.unverifyUser(user.id))} disabled={actionLoading === user.id} className="rounded-lg bg-yellow-100 px-3 py-2 text-xs font-medium text-yellow-800 hover:bg-yellow-200 disabled:opacity-50">{actionLoading === user.id ? '...' : 'Rimuovi verifica'}</button>
                          ) : (
                            <button type="button" onClick={() => handleAction(user.id, () => adminService.verifyUser(user.id))} disabled={actionLoading === user.id} className="rounded-lg bg-green-600 px-3 py-2 text-xs font-medium text-white hover:bg-green-700 disabled:opacity-50">{actionLoading === user.id ? '...' : 'Approva verifica'}</button>
                          )}
                          {user.isActive ? (
                            <button type="button" onClick={() => handleAction(user.id, () => adminService.suspendUser(user.id))} disabled={actionLoading === user.id} className="rounded-lg bg-red-100 px-3 py-2 text-xs font-medium text-red-700 hover:bg-red-200 disabled:opacity-50">{actionLoading === user.id ? '...' : 'Sospendi'}</button>
                          ) : (
                            <button type="button" onClick={() => handleAction(user.id, () => adminService.activateUser(user.id))} disabled={actionLoading === user.id} className="rounded-lg bg-emerald-600 px-3 py-2 text-xs font-medium text-white hover:bg-emerald-700 disabled:opacity-50">{actionLoading === user.id ? '...' : 'Riattiva'}</button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                  {filteredUsers.length === 0 && (
                    <tr>
                      <td colSpan={6} className="px-4 py-8 text-center text-slate-500">Nessun utente trovato con i filtri attivi.</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        {/* Detail side-panel */}
        <div>
          <div className="rounded-3xl border border-slate-200 bg-white/80 p-5 shadow-sm backdrop-blur sm:p-6">
            {detailLoading ? (
              <p className="py-10 text-center text-slate-500">Caricamento dettaglio…</p>
            ) : selectedUser ? (
              <div className="space-y-6">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <h2 className="text-xl font-bold text-slate-900">{selectedUser.fullName || selectedUser.username}</h2>
                    <p className="text-sm text-slate-500">@{selectedUser.username}</p>
                    <p className="text-sm text-slate-500">{selectedUser.email}</p>
                  </div>
                  <Badge value={selectedUser.role} />
                </div>

                <section>
                  <h3 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">Informazioni personali</h3>
                  <div className="grid grid-cols-2 gap-3 text-sm">
                    <Info label="Nome" value={selectedUser.fullName || '—'} />
                    <Info label="Genere" value={selectedUser.gender || '—'} />
                    <Info label="Username" value={selectedUser.username} />
                    <Info label="Email" value={selectedUser.email} />
                  </div>
                </section>

                <section>
                  <h3 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">Stato account</h3>
                  <div className="grid grid-cols-2 gap-3 text-sm">
                    <Info label="Attivo" value={<BooleanPill value={selectedUser.isActive} trueLabel="Sì" falseLabel="No" />} />
                    <Info label="Verificato" value={<BooleanPill value={selectedUser.isVerified} trueLabel="Sì" falseLabel="No" />} />
                    {showStripeSection && (
                      <Info label="Stripe" value={<BooleanPill value={selectedUser.stripeConnected} trueLabel="Connesso" falseLabel="Non connesso" />} />
                    )}
                    <Info label="Ruolo" value={<Badge value={selectedUser.role} />} />
                  </div>
                </section>

                <section>
                  <h3 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">Statistiche prenotazioni</h3>
                  <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                    <StatBox label="Come cliente" value={selectedUser.bookingStats?.totalAsClient ?? 0} />
                    <StatBox label="Come provider" value={selectedUser.bookingStats?.totalAsProvider ?? 0} />
                    <StatBox label="Totale" value={selectedUser.bookingStats?.totalDistinctBookings ?? 0} />
                    <StatBox label="Completate" value={selectedUser.bookingStats?.completed ?? 0} />
                  </div>
                </section>

                {/* Stripe section is conditionally rendered only for STUDENT role */}
                {showStripeSection && (
                  <section>
                    <h3 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">Stripe</h3>
                    <div className="space-y-2 text-sm">
                      <Info label="Stripe collegato" value={<BooleanPill value={selectedUser.stripeInfo?.stripeConnected} trueLabel="Sì" falseLabel="No" />} />
                      <Info label="Account ID" value={selectedUser.stripeInfo?.stripeAccountId || '—'} />
                      <Info label="Account presente" value={<BooleanPill value={selectedUser.stripeInfo?.hasStripeAccount} trueLabel="Sì" falseLabel="No" />} />
                    </div>
                  </section>
                )}

                <section>
                  <h3 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">Servizi</h3>
                  <div className="space-y-2">
                    {(selectedUser.services || []).length > 0 ? selectedUser.services.map((service) => (
                      <div key={service.id} className="rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm">
                        <div className="flex items-center justify-between gap-3">
                          <div>
                            <p className="font-semibold text-slate-800">{service.title}</p>
                            <p className="text-xs text-slate-500">{service.deliveryMode} · €{service.hourlyRate?.toFixed?.(2) ?? service.hourlyRate}/h</p>
                          </div>
                          <BooleanPill value={service.isActive} trueLabel="Attivo" falseLabel="Inattivo" />
                        </div>
                      </div>
                    )) : <p className="text-sm text-slate-500">Nessun servizio associato.</p>}
                  </div>
                </section>

                {/* Moderation action buttons duplicated here for the detail panel */}
                <div className="flex flex-wrap gap-2 border-t border-slate-200 pt-4">
                  {selectedUser.isVerified ? (
                    <button type="button" onClick={() => handleAction(selectedUser.id, () => adminService.unverifyUser(selectedUser.id))} disabled={actionLoading === selectedUser.id} className="rounded-lg bg-yellow-100 px-4 py-2 text-sm font-medium text-yellow-800 hover:bg-yellow-200 disabled:opacity-50">Rimuovi verifica</button>
                  ) : (
                    <button type="button" onClick={() => handleAction(selectedUser.id, () => adminService.verifyUser(selectedUser.id))} disabled={actionLoading === selectedUser.id} className="rounded-lg bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50">Approva verifica</button>
                  )}
                  {selectedUser.isActive ? (
                    <button type="button" onClick={() => handleAction(selectedUser.id, () => adminService.suspendUser(selectedUser.id))} disabled={actionLoading === selectedUser.id} className="rounded-lg bg-red-100 px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-200 disabled:opacity-50">Sospendi account</button>
                  ) : (
                    <button type="button" onClick={() => handleAction(selectedUser.id, () => adminService.activateUser(selectedUser.id))} disabled={actionLoading === selectedUser.id} className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50">Riattiva account</button>
                  )}
                </div>
              </div>
            ) : (
              <p className="py-10 text-center text-slate-500">Seleziona un utente per visualizzare il dettaglio.</p>
            )}
          </div>
        </div>
      </section>
    </div>
  )
}

// Labelled info cell used inside the detail panel
function Info({ label, value }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-slate-50 p-3">
      <p className="text-xs uppercase tracking-wide text-slate-500">{label}</p>
      <div className="mt-1 text-sm font-medium text-slate-800">{value}</div>
    </div>
  )
}

// Centred numeric stat cell used in the booking statistics section
function StatBox({ label, value }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 text-center">
      <p className="text-xs text-slate-500">{label}</p>
      <p className="mt-1 text-xl font-bold text-slate-900">{value}</p>
    </div>
  )
}
