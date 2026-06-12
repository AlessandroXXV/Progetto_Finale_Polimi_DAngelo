import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import Navbar from './Navbar'
import useAuthStore from '../../store/authStore'
import { userService } from '../../services/userService'

// Top-level page shell: renders the Navbar, wraps children in the standard
// page container, and shows a persistent verification banner for unverified users.
export default function Layout({ children }) {
  const { user, setVerified } = useAuthStore()

  // Fetch the user's verification status once after login and keep the store in sync.
  // A cancellation flag prevents a stale setState call if the component unmounts
  // before the request resolves (e.g., immediate logout).
  useEffect(() => {
    if (!user?.userId) return

    let cancelled = false
    userService
      .getVerificationStatus()
      .then(({ data }) => {
        if (!cancelled) {
          setVerified(Boolean(data?.verified))
        }
      })
      .catch(() => {
        // Intentionally silent: auth interceptor already handles auth failures.
      })

    return () => {
      cancelled = true
    }
  }, [user?.userId, setVerified])

  return (
    <div className="app-shell flex min-h-screen flex-col">
      <Navbar />
      <main className="page-container flex-1">
        {/* Verification banner: shown until the user completes identity verification */}
        {user?.isVerified === false && (
          <div className="mb-6 rounded-2xl border border-amber-200/80 bg-amber-50/90 px-4 py-4 text-amber-950 shadow-sm backdrop-blur">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <p className="text-sm font-medium leading-relaxed">
                Il tuo account non è verificato: completa la verifica identità per sbloccare tutte le funzionalità.
              </p>
              <Link
                to="/settings"
                className="inline-flex items-center justify-center rounded-xl bg-amber-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-amber-700"
              >
                Verificami ora
              </Link>
            </div>
          </div>
        )}
        <div className="page-surface overflow-hidden">
          <div className="p-4 sm:p-6 lg:p-8">{children}</div>
        </div>
      </main>
    </div>
  )
}
