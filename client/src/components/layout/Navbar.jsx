import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import useAuthStore from '../../store/authStore'
import Badge from '../common/Badge'
import UserAvatar from '../common/UserAvatar'

// Navigation links shown to authenticated clients (students seeking tutoring)
const clientLinks = [
  { to: '/browse', label: 'Cerca Servizi' },
  { to: '/bookings', label: 'Le mie Prenotazioni' },
  { to: '/reviews', label: 'Le mie Recensioni' },
]

// Navigation links shown to students (tutors offering services)
const studentLinks = [
  { to: '/student/dashboard', label: 'Dashboard' },
  { to: '/student/profiles', label: 'I miei Profili' },
  { to: '/student/availability', label: 'Disponibilità' },
  { to: '/student/bookings', label: 'Prenotazioni' },
  { to: '/student/stripe', label: 'Stripe / Pagamenti' },
]

// Navigation links shown to admin users
const adminLinks = [
  { to: '/admin', label: 'Admin Dashboard' },
]

// Navigation links shown to unauthenticated (guest) users
const guestLinks = [
  { to: '/browse', label: 'Cerca Servizi' },
]

export default function Navbar() {
  const { user, logout } = useAuthStore()
  const navigate = useNavigate()
  const location = useLocation()

  // drawerState tracks both open/close and the route key when the drawer was opened,
  // so the drawer auto-closes on navigation (location.key changes on every route change)
  const [drawerState, setDrawerState] = useState({ isOpen: false, locationKey: null })

  // Select the correct link set based on the authenticated user's role
  const links =
    user?.role === 'CLIENT' ? clientLinks :
    user?.role === 'STUDENT' ? studentLinks :
    user?.role === 'ADMIN' ? adminLinks : guestLinks

  // Drawer is only considered open if both the flag is set AND we're on the same route
  // where it was originally opened — this auto-closes it after any navigation
  const drawerOpen = drawerState.isOpen && drawerState.locationKey === location.key

  // Lock body scroll while the mobile drawer is open to prevent background scrolling
  useEffect(() => {
    document.body.classList.toggle('overflow-hidden', drawerOpen)
    return () => document.body.classList.remove('overflow-hidden')
  }, [drawerOpen])

  // Close the drawer when the user presses Escape (keyboard accessibility)
  useEffect(() => {
    if (!drawerOpen) return

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') setDrawerState((state) => ({ ...state, isOpen: false }))
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [drawerOpen])

  // Capture the current location key so the drawer auto-closes on route change
  const openDrawer = () => setDrawerState({ isOpen: true, locationKey: location.key })
  const closeDrawer = () => setDrawerState((state) => ({ ...state, isOpen: false }))

  const handleLogout = () => {
    closeDrawer()
    logout()
    navigate('/login')
  }

  // --- Mobile drawer (slide-in panel from the right) ---
  // Rendered via createPortal into document.body to escape any stacking context
  // constraints from parent elements. Only visible on mobile (md:hidden on the wrapper).
  const drawer = (
    <div
      // Full-screen overlay wrapper: pointer events disabled when closed to let
      // clicks pass through to the page content behind it
      className={`fixed inset-0 z-[100] md:hidden ${drawerOpen ? 'pointer-events-auto' : 'pointer-events-none'}`}
      aria-hidden={!drawerOpen}
    >
      {/* Semi-transparent backdrop — clicking it closes the drawer */}
      <button
        type="button"
        aria-label="Chiudi navigazione"
        tabIndex={drawerOpen ? 0 : -1}
        onClick={closeDrawer}
        className={`absolute inset-0 h-full w-full bg-slate-950/45 backdrop-blur-sm transition-opacity duration-300 ${
          drawerOpen ? 'opacity-100' : 'opacity-0'
        }`}
      />

      {/* Slide-in panel: translates off-screen (translateX(100%)) when closed */}
      <aside
        role="dialog"
        aria-modal="true"
        aria-labelledby="mobile-nav-title"
        className={`absolute inset-y-0 right-0 flex w-full max-w-[22rem] flex-col bg-white text-slate-800 shadow-2xl transition-transform duration-300 ease-out ${
          drawerOpen ? 'translate-x-0' : 'translate-x-full'
        }`}
      >
        <div className="flex min-h-0 flex-1 flex-col">
          {/* Drawer header: logo + role badge (or guest label) + close button */}
          <div className="flex shrink-0 items-center justify-between border-b border-slate-200 px-5 py-4 pt-[max(1rem,env(safe-area-inset-top))]">
            <Link to="/" onClick={closeDrawer} tabIndex={drawerOpen ? 0 : -1} className="flex min-w-0 items-center gap-3">
              <img
                src="/logo.png"
                alt="Logo Eably"
                className="h-10 w-10 rounded-lg border border-slate-200 bg-white object-cover shadow-sm"
              />
              <div className="min-w-0">
                <p id="mobile-nav-title" className="truncate text-base font-bold text-slate-900">Eably</p>
                {user ? (
                  <div className="mt-1">
                    <Badge value={user?.role} />
                  </div>
                ) : (
                  <p className="text-xs text-slate-500">Navigazione rapida</p>
                )}
              </div>
            </Link>
            <button
              type="button"
              onClick={closeDrawer}
              aria-label="Chiudi navigazione"
              tabIndex={drawerOpen ? 0 : -1}
              className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-slate-100 text-slate-600 transition hover:bg-slate-200"
            >
              <span aria-hidden="true" className="text-2xl leading-none">&times;</span>
            </button>
          </div>

          {/* User identity card — only shown when authenticated; links to account settings */}
          {user && (
            <Link
              to="/settings"
              onClick={closeDrawer}
              tabIndex={drawerOpen ? 0 : -1}
              className="mx-4 mt-4 flex shrink-0 items-center gap-3 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-left transition hover:border-indigo-200 hover:bg-indigo-50/60"
            >
              <UserAvatar userId={user?.userId} username={user?.username} sizeClassName="w-10 h-10" textClassName="text-xs" />
              <div className="min-w-0">
                <span className="block truncate text-sm font-semibold text-slate-900">{user?.username ?? 'Account'}</span>
                <span className="block text-xs text-slate-500">Impostazioni account</span>
              </div>
            </Link>
          )}

          {/* Scrollable navigation link list — active link highlighted in indigo */}
          <div className="min-h-0 flex-1 overflow-y-auto px-4 py-4">
            <div className="space-y-2">
              {links.map((link) => {
                const active = location.pathname.startsWith(link.to)
                return (
                  <Link
                    key={link.to}
                    to={link.to}
                    onClick={closeDrawer}
                    tabIndex={drawerOpen ? 0 : -1}
                    className={`flex min-h-12 items-center justify-between gap-3 rounded-xl border px-4 py-3 text-sm font-medium transition ${
                      active
                        ? 'border-indigo-200 bg-indigo-50 text-indigo-700 shadow-sm'
                        : 'border-slate-200 bg-white text-slate-700 hover:border-indigo-200 hover:bg-indigo-50/60'
                    }`}
                  >
                    <span className="min-w-0 break-words">{link.label}</span>
                    <span aria-hidden="true" className="shrink-0 text-lg text-slate-400">&rarr;</span>
                  </Link>
                )
              })}
            </div>
          </div>

          {/* Drawer footer: logout for authenticated users, login/register for guests.
              Bottom padding respects iOS safe-area-inset to avoid the home indicator. */}
          <div className="shrink-0 border-t border-slate-200 px-4 py-4 pb-[max(1rem,env(safe-area-inset-bottom))]">
            {user ? (
              <button
                onClick={handleLogout}
                tabIndex={drawerOpen ? 0 : -1}
                className="w-full rounded-xl bg-slate-900 px-4 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
              >
                Esci
              </button>
            ) : (
              <div className="grid grid-cols-2 gap-3">
                <Link
                  to="/login"
                  onClick={closeDrawer}
                  tabIndex={drawerOpen ? 0 : -1}
                  className="inline-flex min-h-11 items-center justify-center rounded-xl border border-slate-200 px-4 py-3 text-center text-sm font-semibold text-slate-700 transition hover:border-indigo-200 hover:bg-indigo-50/60"
                >
                  Accedi
                </Link>
                <Link
                  to="/register"
                  onClick={closeDrawer}
                  tabIndex={drawerOpen ? 0 : -1}
                  className="inline-flex min-h-11 items-center justify-center rounded-xl bg-slate-900 px-4 py-3 text-center text-sm font-semibold text-white transition hover:bg-slate-800"
                >
                  Registrati
                </Link>
              </div>
            )}
          </div>
        </div>
      </aside>
    </div>
  )

  return (
    <>
      {/* Sticky top navbar with frosted-glass effect (backdrop-blur + semi-transparent bg) */}
      <nav className="sticky top-0 z-50 border-b border-white/70 bg-white/80 text-slate-900 shadow-[0_10px_30px_rgba(15,23,42,0.06)] backdrop-blur-xl">
        <div className="page-container flex h-16 items-center justify-between gap-3 py-0">

          {/* Brand logo — always visible */}
          <Link to="/" className="flex min-w-0 items-center gap-2 font-bold tracking-tight text-slate-900 md:gap-3">
            <img
              src="/logo.png"
              alt="Logo Eably"
              className="h-9 w-9 shrink-0 rounded-lg border border-slate-200 bg-white object-cover shadow-sm md:h-10 md:w-10 md:rounded-2xl"
            />
            <span className="truncate text-base md:text-lg">Eably</span>
          </Link>

          {/* Desktop navigation links — hidden on mobile (hidden md:flex) */}
          <div className="hidden items-center gap-2 md:flex">
            {links.map((l) => (
              <Link
                key={l.to}
                to={l.to}
                className={`rounded-full px-4 py-2 text-sm font-medium transition ${
                  location.pathname.startsWith(l.to)
                    ? 'bg-indigo-600 text-white shadow-md shadow-indigo-500/20'
                    : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
                }`}
              >
                {l.label}
              </Link>
            ))}
          </div>

          {/* Mobile controls: avatar shortcut + hamburger button — hidden on desktop */}
          <div className="flex items-center gap-2 md:hidden">
            {user && (
              <Link to="/settings" aria-label="Impostazioni account" className="rounded-full">
                <UserAvatar userId={user?.userId} username={user?.username} sizeClassName="w-10 h-10" textClassName="text-xs" />
              </Link>
            )}
            {/* Hamburger button — three horizontal bars rendered with spans */}
            <button
              type="button"
              onClick={openDrawer}
              aria-label="Apri navigazione"
              aria-expanded={drawerOpen}
              className="inline-flex h-10 w-10 items-center justify-center rounded-full border border-slate-200 bg-white text-slate-700 transition hover:border-indigo-200 hover:bg-indigo-50"
            >
              <span aria-hidden="true" className="flex flex-col gap-1">
                <span className="block h-0.5 w-4 rounded-full bg-current" />
                <span className="block h-0.5 w-4 rounded-full bg-current" />
                <span className="block h-0.5 w-4 rounded-full bg-current" />
              </span>
            </button>
          </div>

          {/* Desktop auth section — hidden on mobile (hidden md:flex).
              Shows user avatar + role badge + logout when authenticated,
              or login/register links for guests. */}
          <div className="hidden items-center gap-3 md:flex">
            {user ? (
              <>
                <Link to="/settings" className="flex items-center gap-3 rounded-full border border-slate-200 bg-white px-3 py-2 transition hover:border-indigo-200 hover:bg-indigo-50">
                  <UserAvatar userId={user?.userId} username={user?.username} sizeClassName="w-9 h-9" textClassName="text-xs" />
                  <div className="text-left">
                    <p className="text-sm font-semibold text-slate-900">{user?.username}</p>
                    <div className="mt-1">
                      <Badge value={user?.role} />
                    </div>
                  </div>
                </Link>
                <button
                  onClick={handleLogout}
                  className="rounded-full bg-slate-900 px-4 py-2 text-sm font-semibold text-white transition hover:bg-slate-800"
                >
                  Esci
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 transition hover:border-indigo-200 hover:bg-indigo-50">
                  Accedi
                </Link>
                <Link to="/register" className="rounded-full bg-slate-900 px-4 py-2 text-sm font-semibold text-white transition hover:bg-slate-800">
                  Registrati
                </Link>
              </>
            )}
          </div>
        </div>
      </nav>

      {/* Render the mobile drawer outside the navbar DOM hierarchy via a portal,
          so it can overlay the full page without z-index or overflow clipping issues */}
      {createPortal(drawer, document.body)}
    </>
  )
}
