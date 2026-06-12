import Skeleton from '../common/Skeleton'

// Skeleton placeholder for a single stat card in the stats grid
function StatCardSkeleton() {
  return (
    <div className="rounded-2xl border border-slate-100 bg-white p-4 shadow-sm">
      <div className="mb-3 flex items-start justify-between">
        <Skeleton variant="rect" className="h-10 w-10 rounded-xl" />
        <Skeleton variant="text" className="h-3 w-8" />
      </div>
      <Skeleton variant="text" className="mb-1.5 h-7 w-1/2" />
      <Skeleton variant="text" className="h-3.5 w-3/4" />
    </div>
  )
}

// Skeleton placeholder for a single row in the performance metrics panel
function PerformanceRowSkeleton() {
  return (
    <div className="rounded-2xl border border-slate-100 bg-slate-50 p-4">
      <div className="flex items-center justify-between gap-3">
        <Skeleton variant="text" className="h-4 w-32" />
        <Skeleton variant="text" className="h-6 w-10" />
      </div>
    </div>
  )
}

// Full-page loading skeleton for the coach/admin dashboard.
// Mirrors the real dashboard layout (hero banner → stats grid → quick actions + performance)
// so the page dimensions stay stable while data is being fetched.
// aria-hidden prevents screen readers from announcing meaningless placeholder content.
export default function DashboardStatsSkeleton() {
  return (
    <div className="space-y-6" aria-hidden="true">
      {/* Hero banner skeleton */}
      <div className="page-surface overflow-hidden">
        <div className="bg-linear-to-br from-[#5B4EE8] to-[#4a3fd4] px-5 py-6 sm:px-6 sm:py-8">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div className="max-w-2xl space-y-3">
              <Skeleton className="h-3 w-36 opacity-40" />
              <Skeleton className="h-9 w-64 opacity-40" />
              <Skeleton className="h-4 w-96 opacity-30" />
            </div>
            <div className="grid grid-cols-2 gap-3 sm:max-w-sm">
              <div className="rounded-2xl border border-white/15 bg-white/10 px-4 py-3">
                <Skeleton className="mb-2 h-3 w-20 opacity-30" />
                <Skeleton className="h-8 w-8 opacity-40" />
              </div>
              <div className="rounded-2xl border border-white/15 bg-white/10 px-4 py-3">
                <Skeleton className="mb-2 h-3 w-16 opacity-30" />
                <Skeleton className="h-8 w-8 opacity-40" />
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Stats grid */}
      <section>
        <div className="mb-4 space-y-1.5">
          <Skeleton variant="text" className="h-6 w-44" />
          <Skeleton variant="text" className="h-4 w-64" />
        </div>
        <div className="grid grid-cols-2 gap-4 md:grid-cols-3 xl:grid-cols-6">
          {Array.from({ length: 6 }, (_, i) => (
            <StatCardSkeleton key={i} />
          ))}
        </div>
      </section>

      {/* Bottom two-col layout */}
      <section className="grid gap-6 lg:grid-cols-[1.15fr_0.85fr]">
        {/* Quick actions skeleton */}
        <div className="rounded-3xl border border-slate-200 bg-white p-5 sm:p-6">
          <div className="mb-4 flex items-start justify-between">
            <div className="space-y-2">
              <Skeleton variant="text" className="h-6 w-36" />
              <Skeleton variant="text" className="h-4 w-56" />
            </div>
            <Skeleton variant="pill" className="h-6 w-24" />
          </div>
          <div className="grid gap-3 sm:grid-cols-3">
            {Array.from({ length: 3 }, (_, i) => (
              <div key={i} className="rounded-2xl border border-slate-100 bg-slate-50 px-4 py-4 text-center">
                <Skeleton className="mx-auto mb-3 h-10 w-10 rounded-xl" />
                <Skeleton variant="text" className="mx-auto mb-1.5 h-4 w-3/4" />
                <Skeleton variant="text" className="mx-auto h-3 w-1/2" />
              </div>
            ))}
          </div>
        </div>

        {/* Performance rows skeleton */}
        <div className="rounded-3xl border border-slate-200 bg-white p-5 sm:p-6">
          <div className="mb-5 space-y-2">
            <Skeleton variant="text" className="h-6 w-44" />
            <Skeleton variant="text" className="h-4 w-48" />
          </div>
          <div className="space-y-3">
            {Array.from({ length: 4 }, (_, i) => (
              <PerformanceRowSkeleton key={i} />
            ))}
          </div>
        </div>
      </section>
    </div>
  )
}
