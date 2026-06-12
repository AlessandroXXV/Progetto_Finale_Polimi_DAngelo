// Page for managing the tutor's Stripe Connect account.
// Shows balance, allows payout requests, and lists the 10 most recent payouts.
// If no Stripe account exists yet, renders an onboarding flow instead.
import { useState, useEffect, useCallback } from 'react'
import { paymentService } from '../../services/paymentService'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import { CreditCard, Link2, Settings } from 'lucide-react'
import toast from 'react-hot-toast'

// Stripe stores monetary amounts in cents; convert for display.
function centsToEur(cents) {
  return (cents / 100).toFixed(2)
}

// Small presentational component that maps a Stripe payout status to a coloured pill.
function StatusPill({ status }) {
  const classes =
    status === 'paid'
      ? 'bg-green-100 text-green-700'
      : status === 'pending'
      ? 'bg-yellow-100 text-yellow-700'
      : status === 'in_transit'
      ? 'bg-blue-100 text-blue-700'
      : 'bg-slate-100 text-slate-600'

  const label =
    status === 'paid' ? 'Pagato' : status === 'pending' ? 'In attesa' : status === 'in_transit' ? 'In transito' : status

  return <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${classes}`}>{label}</span>
}

export default function StripeDashboardPage() {
  const [balance, setBalance] = useState(null)
  const [payouts, setPayouts] = useState([])
  const [loading, setLoading] = useState(true)
  const [hasAccount, setHasAccount] = useState(false)
  const [onboarding, setOnboarding] = useState(false)
  const [payoutAmount, setPayoutAmount] = useState('')
  const [payoutLoading, setPayoutLoading] = useState(false)

  // Fetch balance and payout history in parallel. A 404/400 means no account yet.
  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [balRes, payRes] = await Promise.all([paymentService.getBalance(), paymentService.getPayouts(10)])
      setBalance(balRes.data)
      setPayouts(payRes.data)
      setHasAccount(true)
    } catch (err) {
      if (err.response?.status === 404 || err.response?.status === 400) {
        setHasAccount(false)
      } else {
        toast.error('Errore caricamento dati Stripe')
      }
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  // Creates a new Stripe Connect account and immediately redirects to the onboarding link.
  const handleCreateAccount = async () => {
    setOnboarding(true)
    try {
      const refreshUrl = `${window.location.origin}/student/stripe/refresh`
      const returnUrl = `${window.location.origin}/student/stripe/return`
      await paymentService.createConnectAccount(refreshUrl, returnUrl)
      toast.success("Account creato! Procedi con l'onboarding Stripe.")
      const { data: link } = await paymentService.getAccountLink(refreshUrl, returnUrl)
      window.location.href = link.url
    } catch (err) {
      toast.error(err.response?.data?.message ?? 'Errore creazione account')
    } finally {
      setOnboarding(false)
    }
  }

  // Retrieves a fresh Stripe dashboard link for an already-connected account.
  const handleGetLink = async () => {
    setOnboarding(true)
    try {
      const refreshUrl = `${window.location.origin}/student/stripe/refresh`
      const returnUrl = `${window.location.origin}/student/stripe/return`
      const { data } = await paymentService.getAccountLink(refreshUrl, returnUrl)
      window.location.href = data.url
    } catch (err) {
      toast.error(err.response?.data?.message ?? 'Errore')
    } finally {
      setOnboarding(false)
    }
  }

  const handlePayout = async (e) => {
    e.preventDefault()
    // Convert the euro amount entered by the user to integer cents as required by Stripe.
    const amountCents = Math.round(parseFloat(payoutAmount) * 100)
    if (isNaN(amountCents) || amountCents < 1) {
      toast.error('Inserisci un importo valido')
      return
    }

    setPayoutLoading(true)
    try {
      await paymentService.requestPayout(amountCents)
      toast.success('Payout richiesto!')
      setPayoutAmount('')
      // Reload to reflect the updated balance and payout list.
      load()
    } catch (err) {
      toast.error(err.response?.data?.message ?? 'Errore payout')
    } finally {
      setPayoutLoading(false)
    }
  }

  if (loading) return <LoadingSpinner />

  // No Stripe account: show step-by-step onboarding prompt.
  if (!hasAccount) {
    return (
      <div className="mx-auto max-w-2xl space-y-6 text-center">
        <section className="page-surface overflow-hidden">
          <div className="bg-linear-to-br from-[#5B4EE8] to-[#4a3fd4] px-5 py-8 text-white sm:px-6 sm:py-10">
            <div className="mx-auto max-w-xl">
              <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-2xl bg-white/15 backdrop-blur">
                <CreditCard className="h-7 w-7" strokeWidth={1.5} />
              </div>
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-white/70">Stripe Connect</p>
              <h1 className="mt-2 text-3xl font-bold tracking-tight sm:text-4xl">Collega il tuo account Stripe</h1>
              <p className="mt-3 text-sm leading-6 text-white/85 sm:text-base">
                Per ricevere pagamenti dai clienti, devi completare l'attivazione Stripe Connect. Il processo è semplice e sicuro.
              </p>
            </div>
          </div>
        </section>

        <section className="panel p-5 sm:p-6 text-left">
          <div className="grid gap-3 sm:grid-cols-3">
            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">1</p>
              <p className="mt-2 text-sm font-semibold text-slate-900">Crea account</p>
              <p className="mt-1 text-xs text-slate-500">Avvia la configurazione Stripe</p>
            </div>
            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">2</p>
              <p className="mt-2 text-sm font-semibold text-slate-900">Completa onboarding</p>
              <p className="mt-1 text-xs text-slate-500">Verifica dati e conto</p>
            </div>
            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">3</p>
              <p className="mt-2 text-sm font-semibold text-slate-900">Ricevi payout</p>
              <p className="mt-1 text-xs text-slate-500">Sblocca i guadagni</p>
            </div>
          </div>

          <button
            onClick={handleCreateAccount}
            disabled={onboarding}
            className="primary-button mt-6 inline-flex w-full items-center justify-center gap-2"
          >
            {onboarding ? (
              'Connessione in corso…'
            ) : (
              <>
                <Link2 className="h-4 w-4" strokeWidth={1.5} />
                Crea Account Stripe
              </>
            )}
          </button>
        </section>
      </div>
    )
  }

  // Stripe returns balance arrays per currency; we always use index 0 (EUR).
  const availableAmount = balance?.available?.[0]?.amount || 0
  const pendingAmount = balance?.pending?.[0]?.amount || 0

  return (
    <div className="space-y-6">
      <section className="page-surface overflow-hidden">
        <div className="bg-linear-to-br from-[#0F0E2A] to-[#4a3fd4] px-5 py-6 text-white sm:px-6 sm:py-8">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div className="max-w-2xl">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-white/65">Pagamenti e payout</p>
              <h1 className="mt-2 text-3xl font-bold tracking-tight sm:text-4xl">Dashboard Stripe</h1>
              <p className="mt-3 text-sm leading-6 text-white/80 sm:text-base">
                Monitora saldo, richieste di payout e storico pagamenti con una vista più ordinata e leggibile.
              </p>
            </div>

            <div className="grid grid-cols-2 gap-3 sm:max-w-sm">
              <div className="rounded-2xl border border-white/10 bg-white/10 px-4 py-3 backdrop-blur">
                <p className="text-xs text-white/70">Saldo disponibile</p>
                <p className="mt-1 text-2xl font-bold">€{centsToEur(availableAmount)}</p>
              </div>
              <div className="rounded-2xl border border-white/10 bg-white/10 px-4 py-3 backdrop-blur">
                <p className="text-xs text-white/70">In elaborazione</p>
                <p className="mt-1 text-2xl font-bold">€{centsToEur(pendingAmount)}</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {balance && (
        <section className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div className="rounded-3xl border border-emerald-200 bg-emerald-50 p-5 shadow-sm">
            <p className="text-sm font-semibold uppercase tracking-wide text-emerald-700">Saldo disponibile</p>
            <p className="mt-3 text-4xl font-bold text-emerald-800">€{centsToEur(availableAmount)}</p>
            <p className="mt-1 text-xs text-emerald-700/80">{(balance.available?.[0]?.currency || 'EUR').toUpperCase()}</p>
          </div>
          <div className="rounded-3xl border border-amber-200 bg-amber-50 p-5 shadow-sm">
            <p className="text-sm font-semibold uppercase tracking-wide text-amber-700">In elaborazione</p>
            <p className="mt-3 text-4xl font-bold text-amber-800">€{centsToEur(pendingAmount)}</p>
            <p className="mt-1 text-xs text-amber-700/80">Disponibile a breve</p>
          </div>
        </section>
      )}

      <section className="grid gap-6 lg:grid-cols-[0.95fr_1.05fr]">
        <div className="panel p-5 sm:p-6">
          <div className="mb-5">
            <h2 className="text-xl font-semibold tracking-tight text-slate-900">Richiedi payout</h2>
            <p className="mt-1 text-sm text-slate-500">Trasferisci il saldo disponibile al conto bancario collegato</p>
          </div>

          <form onSubmit={handlePayout} className="space-y-4">
            <div>
              <label className="field-label">Importo (€)</label>
              <div className="relative">
                <span className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 font-medium">€</span>
                <input
                  type="number"
                  value={payoutAmount}
                  onChange={(e) => setPayoutAmount(e.target.value)}
                  min="0.01"
                  step="0.01"
                  className="field pl-8"
                  placeholder="50.00"
                />
              </div>
              <p className="mt-1 text-xs text-slate-500">L'importo viene convertito in centesimi prima dell'invio.</p>
            </div>

            <button type="submit" disabled={payoutLoading} className="primary-button w-full bg-emerald-600 hover:bg-emerald-700">
              {payoutLoading ? 'Richiesta in corso…' : 'Richiedi payout'}
            </button>
          </form>

          <div className="mt-4 rounded-2xl border border-slate-200 bg-slate-50 p-4">
            <p className="text-sm font-semibold text-slate-900">Gestione account</p>
            <p className="mt-1 text-sm text-slate-500">Apri il dashboard Stripe per modificare dati, conto o verifiche.</p>
            <button
              onClick={handleGetLink}
              disabled={onboarding}
              className="secondary-button mt-3 inline-flex w-full items-center justify-center gap-2"
            >
              {onboarding ? (
                'Apertura…'
              ) : (
                <>
                  <Settings className="h-4 w-4" strokeWidth={1.5} />
                  Vai alla dashboard Stripe
                </>
              )}
            </button>
          </div>
        </div>

        <div className="panel p-5 sm:p-6">
          <div className="mb-5 flex items-start justify-between gap-4">
            <div>
              <h2 className="text-xl font-semibold tracking-tight text-slate-900">Storico payout</h2>
              <p className="mt-1 text-sm text-slate-500">Ultime richieste e stato di avanzamento</p>
            </div>
            <span className="rounded-full bg-indigo-50 px-3 py-1 text-xs font-semibold text-indigo-700">Ultimi 10</span>
          </div>

          {payouts.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 px-4 py-10 text-center">
              <p className="text-lg font-semibold text-slate-900">Nessun payout ancora</p>
              <p className="mt-2 text-sm text-slate-500">Quando richiederai un payout, lo vedrai in questa lista.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {payouts.map((p, i) => (
                // Use p.id when available; fall back to index for Stripe objects that may lack a client-side id.
                <div key={p.id ?? i} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <p className="text-sm font-semibold text-slate-900">€{centsToEur(p.amount)}</p>
                      {/* arrival_date is a Unix timestamp (seconds); multiply by 1000 for JS Date. */}
                      <p className="mt-1 text-xs text-slate-500">
                        {p.arrival_date ? new Date(p.arrival_date * 1000).toLocaleDateString('it-IT') : '—'}
                      </p>
                    </div>
                    <StatusPill status={p.status} />
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>
    </div>
  )
}
