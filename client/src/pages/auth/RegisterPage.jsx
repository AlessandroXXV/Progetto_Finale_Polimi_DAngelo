// Registration page: collects user details and role choice, calls authService, auto-logs the user in on success.
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authService } from '../../services/authService'
import useAuthStore from '../../store/authStore'
import toast from 'react-hot-toast'
import { Check, User, GraduationCap } from 'lucide-react'

export default function RegisterPage() {
  const [form, setForm] = useState({
    username: '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    // Default role is CLIENT; users can switch to STUDENT via the radio buttons below.
    role: 'CLIENT',
  })
  const [loading, setLoading] = useState(false)
  const { login } = useAuthStore()
  const navigate = useNavigate()

  // Generic controlled-input handler: uses the input's name attribute as the state key.
  const handleChange = (e) =>
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    // Client-side guard: the minLength HTML attribute alone doesn't block submission in all cases.
    if (form.password.length < 8) {
      toast.error('La password deve essere di almeno 8 caratteri')
      return
    }
    setLoading(true)
    try {
      const { data } = await authService.register(form)
      // Immediately log the user in with the token returned by the registration endpoint.
      login(data)
      toast.success('Account creato con successo!')
      navigate('/', { replace: true })
    } catch (err) {
      toast.error(err.response?.data?.message ?? 'Errore durante la registrazione')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen px-4 py-6 sm:px-6 lg:px-8">
      {/* Two-column layout on large screens: decorative panel left, form right (column order is reversed vs LoginPage). */}
      <div className="mx-auto grid min-h-[calc(100vh-3rem)] w-full max-w-6xl overflow-hidden rounded-4xl border border-white/70 bg-white/70 shadow-[0_25px_80px_rgba(15,23,42,0.12)] backdrop-blur lg:grid-cols-[0.95fr_1.05fr]">
        {/* Left decorative panel — hidden on mobile. */}
        <div className="hidden bg-linear-to-br from-[#0F0E2A] to-[#4a3fd4] p-8 text-white lg:flex lg:flex-col lg:justify-between">
          <div>
            <div className="mb-10 flex items-center gap-3">
              <Link to="/" className="flex items-center gap-3 font-bold tracking-tight text-white">
                <img
                  src="/logo.png"
                  alt="Logo Eably"
                  className="h-10 w-10 rounded-2xl border border-white/20 bg-white object-cover shadow-sm"
                />
              </Link>
              <div>
                <p className="text-lg font-semibold">Crea il tuo profilo</p>
                <p className="text-sm text-white/70">In pochi passaggi sei pronto a usare Eably</p>
              </div>
            </div>
            <h1 className="max-w-md text-4xl font-bold tracking-tight">Un solo account per prenotare e offrire servizi.</h1>
            <p className="mt-4 max-w-md text-base leading-7 text-white/80">
              Registrati come cliente per prenotare, oppure come studente per offrire i tuoi servizi.
            </p>
          </div>
          <div className="grid gap-3 text-sm text-white/90">
            <div className="flex items-center gap-2 rounded-2xl border border-white/15 bg-white/10 px-4 py-3"><Check className="h-4 w-4" strokeWidth={1.5} /> Prenota lezioni e servizi</div>
            <div className="flex items-center gap-2 rounded-2xl border border-white/15 bg-white/10 px-4 py-3"><Check className="h-4 w-4" strokeWidth={1.5} /> Paga in sicurezza con Stripe</div>
            <div className="flex items-center gap-2 rounded-2xl border border-white/15 bg-white/10 px-4 py-3"><Check className="h-4 w-4" strokeWidth={1.5} /> Chatta in tempo reale con gli studenti</div>
          </div>
        </div>

        {/* Right panel: the registration form. */}
        <div className="flex items-center justify-center p-5 sm:p-8">
          <div className="w-full max-w-xl">
            <div className="mb-8 text-center lg:text-left">
              {/* Logo shown only on mobile since the left panel is hidden there. */}
              <div className="mb-4 inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-white lg:hidden">
                <img src="/logo.png" alt="Logo Eably" className="h-full w-full rounded-2xl object-cover shadow-lg shadow-indigo-500/20" />
              </div>
              <h1 className="text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">Crea il tuo account</h1>
              <p className="mt-2 text-sm text-slate-500 sm:text-base">Ti bastano pochi minuti per iniziare</p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-5">
              {/* First name and last name share a two-column grid on wider viewports. */}
              <div className="grid gap-4 sm:grid-cols-2">
                <div>
                  <label className="field-label">Nome</label>
                  <input
                    type="text"
                    name="firstName"
                    value={form.firstName}
                    onChange={handleChange}
                    required
                    className="field"
                    placeholder="Mario"
                  />
                </div>
                <div>
                  <label className="field-label">Cognome</label>
                  <input
                    type="text"
                    name="lastName"
                    value={form.lastName}
                    onChange={handleChange}
                    required
                    className="field"
                    placeholder="Rossi"
                  />
                </div>
              </div>

              <div>
                <label className="field-label">Username</label>
                <input
                  type="text"
                  name="username"
                  value={form.username}
                  onChange={handleChange}
                  required
                  minLength={3}
                  maxLength={20}
                  className="field"
                  placeholder="mario_rossi"
                />
              </div>

              <div>
                <label className="field-label">Email</label>
                <input
                  type="email"
                  name="email"
                  value={form.email}
                  onChange={handleChange}
                  required
                  className="field"
                  placeholder="mario@esempio.it"
                />
              </div>

              <div>
                <label className="field-label">Password <span className="text-slate-400">(min 8 caratteri)</span></label>
                <input
                  type="password"
                  name="password"
                  value={form.password}
                  onChange={handleChange}
                  required
                  minLength={8}
                  className="field"
                  placeholder="••••••••"
                />
              </div>

              {/* Role selector rendered as styled radio cards; only CLIENT and STUDENT are available at registration. */}
              <div>
                <label className="field-label">Tipo di account</label>
                <div className="grid gap-3 sm:grid-cols-2">
                  {['CLIENT', 'STUDENT'].map((r) => (
                    <label
                      key={r}
                      // Highlight the selected role card with an indigo border and background.
                      className={`flex cursor-pointer items-start gap-3 rounded-2xl border p-4 transition ${
                        form.role === r
                          ? 'border-indigo-300 bg-indigo-50 shadow-sm'
                          : 'border-slate-200 bg-white hover:border-indigo-200 hover:bg-indigo-50/50'
                      }`}
                    >
                      <input
                        type="radio"
                        name="role"
                        value={r}
                        checked={form.role === r}
                        onChange={handleChange}
                        className="mt-1 text-indigo-600 focus:ring-indigo-500"
                      />
                      <div>
                        <p className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                          {r === 'CLIENT' ? (
                            <><User className="h-4 w-4" strokeWidth={1.5} /> Cliente</>
                          ) : (
                            <><GraduationCap className="h-4 w-4" strokeWidth={1.5} /> Tutor / Studente</>
                          )}
                        </p>
                        <p className="mt-1 text-xs leading-5 text-slate-500">
                          {r === 'CLIENT'
                            ? 'Prenota lezioni e servizi'
                            : 'Offri lezioni e servizi'}
                        </p>
                      </div>
                    </label>
                  ))}
                </div>
              </div>

              <button type="submit" disabled={loading} className="primary-button w-full">
                {loading ? 'Creazione account…' : 'Crea Account'}
              </button>
            </form>

            <p className="mt-6 text-center text-sm text-slate-500">
              Hai già un account?{' '}
              <Link to="/login" className="font-semibold text-indigo-600 hover:text-indigo-700">
                Accedi
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
