import { Navigate, useLocation } from 'react-router-dom'
import useAuthStore from '../../store/authStore'

export default function ProtectedRoute({ children, roles }) {
  const { token, user } = useAuthStore()
  const location = useLocation()

  // Redirect unauthenticated users to login and preserve the attempted route.
  if (!token) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  // When roles are provided, block users who do not match the allowed set.
  if (roles && !roles.includes(user?.role)) {
    return <Navigate to="/" replace />
  }

  return children
}
