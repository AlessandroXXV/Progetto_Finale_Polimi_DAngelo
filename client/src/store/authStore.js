// Global authentication store built with Zustand.
// The `persist` middleware serialises the entire state to localStorage under the key
// 'eably-auth', so the session survives page reloads without a server round-trip.
import { create } from 'zustand'
import { persist } from 'zustand/middleware'

const useAuthStore = create(
  persist(
    (set, get) => ({
      token: null,
      refreshToken: null,
      // Incremented each time the profile image is updated; consumed by useProfileImage
      // to invalidate its module-level cache without requiring a full page reload.
      profileImageRevision: 0,
      user: null, // { userId, username, email, role, stripeConnected, isVerified }

      // Populates the store from the server's auth response after a successful login.
      login: (authResponse) => {
        set({
          token: authResponse.token,
          refreshToken: authResponse.refreshToken,
          user: {
            userId: authResponse.userId,
            username: authResponse.username,
            email: authResponse.email,
            role: authResponse.role,
            stripeConnected: authResponse.stripeConnected ?? false,
            isVerified: authResponse.isVerified ?? null,
          },
        })
      },

      // Partial updates for fields that can change independently after login.
      setStripeConnected: (stripeConnected) =>
        set((state) => ({
          user: state.user ? { ...state.user, stripeConnected } : state.user,
        })),

      setVerified: (isVerified) =>
        set((state) => ({
          user: state.user ? { ...state.user, isVerified } : state.user,
        })),

      // Signals to useProfileImage that a new image has been uploaded and caches must refresh.
      bumpProfileImageRevision: () =>
        set((state) => ({
          profileImageRevision: state.profileImageRevision + 1,
        })),

      logout: () => {
        set({ token: null, refreshToken: null, profileImageRevision: 0, user: null })
      },

      // Convenience selectors — use get() to read state outside of React renders.
      isAuthenticated: () => !!get().token,
      isClient: () => get().user?.role === 'CLIENT',
      isStudent: () => get().user?.role === 'STUDENT',
      isAdmin: () => get().user?.role === 'ADMIN',
    }),
    { name: 'eably-auth' }
  )
)

export default useAuthStore
