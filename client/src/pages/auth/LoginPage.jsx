// Login page: collects username/email + password, calls authService, stores session via Zustand, then redirects.
import { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { authService } from '../../services/authService'
import useAuthStore from '../../store/authStore'
import toast from 'react-hot-toast'
import { Check } from 'lucide-react'

export default function LoginPage() {
  const [form, setForm] = useState({ usernameOrEmail: '', password: '' })
  const [loading, setLoading] = useState(false)
  const { login } = useAuthStore()
  const navigate = useNavigate()
  const location = useLocation()
  // Redirect back to the page the user was trying to access before being sent to login, defaulting to home.
  const from = location.state?.from?.pathname || '/'

  // Generic controlled-input handler: uses the input's name attribute as the state key.
  const handleChange = (e) =>
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    try {
      const { data } = await authService.login(form)
      // Persist the returned auth data (token + user info) in the global store.
      login(data)
      toast.success(`Benvenuto, ${data.username}!`)
      // replace: true avoids adding the login page to the browser history stack.
      navigate(from, { replace: true })
    } catch (err) {
      toast.error(err.response?.data?.message ?? 'Credenziali non valide')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen px-4 py-6 sm:px-6 lg:px-8">
      {/* Two-column layout on large screens: decorative brand panel on the left, form on the right. */}
      <div className="mx-auto grid min-h-[calc(100vh-3rem)] w-full max-w-6xl overflow-hidden rounded-4xl border border-white/70 bg-white/70 shadow-[0_25px_80px_rgba(15,23,42,0.12)] backdrop-blur lg:grid-cols-[1.05fr_0.95fr]">
        {/* Left decorative panel — hidden on mobile, visible only on large screens. */}
        <div className="hidden bg-linear-to-br from-[#5B4EE8] to-[#4a3fd4] p-8 text-white lg:flex lg:flex-col lg:justify-between">
          <div>
            <div className="mb-10 flex items-center gap-3">

              <Link to="/" className="flex items-center gap-3 font-bold tracking-tight text-slate-900">
                <img
                    src="/logo.png"
                    alt="Logo Eably"
                    className="h-10 w-10 rounded-2xl border border-slate-200 bg-white object-cover shadow-sm"
                />
                {/*<span className="text-lg">Eably</span>*/}
              </Link>
              {/*<span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-white/15 text-xl backdrop-blur">🎓</span>*/}
              <div>
                <p className="text-lg font-semibold">Eably</p>
                <p className="text-sm text-white/70">Learning, bookings & tutoring</p>
              </div>
            </div>
            <h1 className="max-w-md text-4xl font-bold tracking-tight">Bentornato su Eably.</h1>
            <p className="mt-4 max-w-md text-base leading-7 text-white/80">
              Accedi per gestire le tue prenotazioni, i pagamenti e i tuoi servizi.
            </p>
          </div>
          {/* Feature highlights displayed as pill badges at the bottom of the panel. */}
          <div className="grid gap-3 text-sm text-white/90">
            <div className="flex items-center gap-2 rounded-2xl border border-white/15 bg-white/10 px-4 py-3"><Check className="h-4 w-4" strokeWidth={1.5} /> Prenota lezioni e servizi</div>
            <div className="flex items-center gap-2 rounded-2xl border border-white/15 bg-white/10 px-4 py-3"><Check className="h-4 w-4" strokeWidth={1.5} /> Paga in sicurezza con Stripe</div>
            <div className="flex items-center gap-2 rounded-2xl border border-white/15 bg-white/10 px-4 py-3"><Check className="h-4 w-4" strokeWidth={1.5} /> Chatta in tempo reale con gli studenti</div>
          </div>
        </div>

        {/* Right panel: the actual login form. */}
        <div className="flex items-center justify-center p-5 sm:p-8">
          <div className="w-full max-w-md">
            <div className="mb-8 text-center lg:text-left">
              {/* Logo shown only on mobile since the left panel is hidden there. */}
              <div className="mb-4 inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-white lg:hidden">
                <img src="/logo.png" alt="Logo Eably" className="h-full w-full rounded-2xl object-cover shadow-lg shadow-indigo-500/20" />
              </div>
              <h1 className="text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">Bentornato</h1>
              <p className="mt-2 text-sm text-slate-500 sm:text-base">Accedi al tuo account Eably</p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-5">
              <div>
                <label className="field-label">Username o Email</label>
                <input
                  type="text"
                  name="usernameOrEmail"
                  value={form.usernameOrEmail}
                  onChange={handleChange}
                  required
                  className="field"
                  placeholder="mario_rossi"
                />
              </div>

              <div>
                <label className="field-label">Password</label>
                <input
                  type="password"
                  name="password"
                  value={form.password}
                  onChange={handleChange}
                  required
                  className="field"
                  placeholder="••••••••"
                />
              </div>

              <button type="submit" disabled={loading} className="primary-button w-full">
                {loading ? 'Accesso…' : 'Accedi'}
              </button>
            </form>

            <p className="mt-6 text-center text-sm text-slate-500">
              Non hai un account?{' '}
              <Link to="/register" className="font-semibold text-indigo-600 hover:text-indigo-700">
                Registrati
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
