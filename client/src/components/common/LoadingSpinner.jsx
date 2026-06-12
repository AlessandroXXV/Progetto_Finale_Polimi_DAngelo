export default function LoadingSpinner({ className = '' }) {
  return (
    // Full-width centered spinner used for page-level loading states.
    <div className={`flex items-center justify-center py-12 ${className}`} aria-label="Caricamento in corso">
      <div className="h-11 w-11 animate-spin rounded-full border-4 border-indigo-100 border-t-indigo-600" />
    </div>
  )
}
