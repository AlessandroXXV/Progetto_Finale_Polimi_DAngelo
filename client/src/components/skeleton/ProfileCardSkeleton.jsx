import Skeleton from '../common/Skeleton'

// Loading placeholder that mirrors the shape of a coach profile card.
// Each Skeleton block corresponds to a real UI element so the layout
// does not shift when the actual data loads in.
// aria-hidden prevents screen readers from announcing the placeholder.
export default function ProfileCardSkeleton() {
  return (
    <div
      aria-hidden="true"
      className="panel overflow-hidden p-5"
    >
      {/* Header: avatar + title block */}
      <div className="mb-4 flex items-start gap-3">
        <Skeleton variant="circle" className="h-12 w-12 shrink-0" />
        <div className="flex-1 space-y-2 pt-0.5">
          <Skeleton variant="text" className="h-5 w-3/4" />
          <Skeleton variant="text" className="h-3.5 w-1/3" />
        </div>
        {/* Badge placeholder */}
        <Skeleton variant="pill" className="h-6 w-16 shrink-0" />
      </div>

      {/* Bio lines */}
      <div className="mb-4 space-y-2">
        <Skeleton variant="text" className="h-3.5 w-full" />
        <Skeleton variant="text" className="h-3.5 w-full" />
        {/* Shorter last line simulates a natural paragraph ending */}
        <Skeleton variant="text" className="h-3.5 w-2/3" />
      </div>

      {/* Tags row */}
      <div className="mb-4 flex gap-2">
        <Skeleton variant="pill" className="h-6 w-24" />
        <Skeleton variant="pill" className="h-6 w-20" />
      </div>

      {/* Footer: price + CTA */}
      <div className="flex items-end justify-between border-t border-slate-100 pt-4">
        <div className="space-y-1.5">
          <Skeleton variant="text" className="h-3 w-20" />
          <Skeleton variant="text" className="h-7 w-16" />
        </div>
        <Skeleton variant="pill" className="h-8 w-28" />
      </div>
    </div>
  )
}
