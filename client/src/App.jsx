import { lazy, Suspense } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { Toaster } from 'react-hot-toast'
import useAuthStore from './store/authStore'
import Layout from './components/layout/Layout'
import ProtectedRoute from './components/common/ProtectedRoute'
import LoadingSpinner from './components/common/LoadingSpinner'

// Authentication pages
const LoginPage = lazy(() => import('./pages/auth/LoginPage'))
const RegisterPage = lazy(() => import('./pages/auth/RegisterPage'))

// Shared pages
const AccountSettingsPage = lazy(() => import('./pages/common/AccountSettingsPage'))

// Client-facing routes
const BrowseProfilesPage = lazy(() => import('./pages/client/BrowseProfilesPage'))
const ProfileDetailPage = lazy(() => import('./pages/client/ProfileDetailPageWeekly'))
const ProfileDetailGuestPage = lazy(() => import('./pages/client/ProfileDetailGuestPage'))
const MyBookingsPage = lazy(() => import('./pages/client/MyBookingsPage'))
const BookingDetailPage = lazy(() => import('./pages/client/BookingDetailPage'))
const MyReviewsPage = lazy(() => import('./pages/client/MyReviewsPage'))

// Student routes
const StudentDashboardPage = lazy(() => import('./pages/student/StudentDashboardPage'))
const ManageProfilesPage = lazy(() => import('./pages/student/ManageProfilesPage'))
const CreateEditProfilePage = lazy(() => import('./pages/student/CreateEditProfilePage'))
const ManageAvailabilityPage = lazy(() => import('./pages/student/ManageAvailabilityPage'))
const StudentBookingsPage = lazy(() => import('./pages/student/StudentBookingsPage'))
const StudentBookingDetailPage = lazy(() => import('./pages/student/StudentBookingDetailPage'))
const StripeDashboardPage = lazy(() => import('./pages/student/StripeDashboardPage'))

// Admin routes
const AdminDashboardPage = lazy(() => import('./pages/admin/AdminDashboardPage'))

// Stripe callback pages
const StripeReturnPage = lazy(() => import('./pages/stripe/StripeReturnPage'))
const StripeRefreshPage = lazy(() => import('./pages/stripe/StripeRefreshPage'))

function ProfileDetailRouter() {
  const { user } = useAuthStore()
  return user ? <ProfileDetailPage /> : <ProfileDetailGuestPage />
}

function HomeRedirect() {
  const { user } = useAuthStore()
  // Guests land on the public browse page; authenticated users are routed by role.
  if (!user) return <Navigate to="/browse" replace />
  if (user.role === 'STUDENT') return <Navigate to="/student/dashboard" replace />
  if (user.role === 'ADMIN') return <Navigate to="/admin" replace />
  return <Navigate to="/browse" replace />
}

export default function App() {
  return (
    <BrowserRouter>
      <Toaster position="top-right" toastOptions={{ duration: 4000 }} />
      <Suspense fallback={<LoadingSpinner className="min-h-screen" />}>
        <Routes>
          {/* Public routes */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/* Stripe callbacks render without the main layout */}
          <Route path="/student/stripe/return" element={<StripeReturnPage />} />
          <Route path="/student/stripe/refresh" element={<StripeRefreshPage />} />

          {/* Root redirect uses the authenticated role when available */}
          <Route path="/" element={<Layout><HomeRedirect /></Layout>} />

          <Route path="/settings" element={
            <ProtectedRoute><Layout><AccountSettingsPage /></Layout></ProtectedRoute>
          } />

          {/* Client routes */}
          <Route path="/browse" element={<Layout><BrowseProfilesPage /></Layout>} />
          <Route path="/profile/:id" element={<Layout><ProfileDetailRouter /></Layout>} />
          <Route path="/bookings" element={
            <ProtectedRoute><Layout><MyBookingsPage /></Layout></ProtectedRoute>
          } />
          <Route path="/booking/:id" element={
            <ProtectedRoute><Layout><BookingDetailPage /></Layout></ProtectedRoute>
          } />
          <Route path="/reviews" element={
            <ProtectedRoute roles={['CLIENT']}><Layout><MyReviewsPage /></Layout></ProtectedRoute>
          } />

        {/* Student routes */}
        <Route path="/student/dashboard" element={
          <ProtectedRoute roles={['STUDENT']}><Layout><StudentDashboardPage /></Layout></ProtectedRoute>
        } />
        <Route path="/student/profiles" element={
          <ProtectedRoute roles={['STUDENT']}><Layout><ManageProfilesPage /></Layout></ProtectedRoute>
        } />
        <Route path="/student/profiles/new" element={
          <ProtectedRoute roles={['STUDENT']}><Layout><CreateEditProfilePage /></Layout></ProtectedRoute>
        } />
        <Route path="/student/profiles/:id/edit" element={
          <ProtectedRoute roles={['STUDENT']}><Layout><CreateEditProfilePage /></Layout></ProtectedRoute>
        } />
        <Route path="/student/availability" element={
          <ProtectedRoute roles={['STUDENT']}><Layout><ManageAvailabilityPage /></Layout></ProtectedRoute>
        } />
        <Route path="/student/bookings" element={
          <ProtectedRoute roles={['STUDENT']}><Layout><StudentBookingsPage /></Layout></ProtectedRoute>
        } />
        <Route path="/student/booking/:id" element={
          <ProtectedRoute roles={['STUDENT']}><Layout><StudentBookingDetailPage /></Layout></ProtectedRoute>
        } />
        <Route path="/student/stripe" element={
          <ProtectedRoute roles={['STUDENT']}><Layout><StripeDashboardPage /></Layout></ProtectedRoute>
        } />

          {/* Admin routes */}
          <Route path="/admin" element={
            <ProtectedRoute roles={['ADMIN']}><Layout><AdminDashboardPage /></Layout></ProtectedRoute>
          } />

          {/* Catch-all redirect */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  )
}
