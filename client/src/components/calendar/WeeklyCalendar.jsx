import { formatWeekRange, getWeekDays } from '../../lib/weekCalendar'
import { ArrowLeft, ArrowRight } from 'lucide-react'

// Generic weekly calendar shell. It handles navigation and layout; the caller
// is responsible for rendering each day's content through render props.
// Props:
//   weekStart    – Date object representing Monday of the displayed week
//   onPrevWeek / onNextWeek – callbacks to shift the week back or forward
//   title / subtitle         – optional header text above the navigation bar
//   renderDay    – (day: DayDescriptor) => ReactNode — renders the body of each day column
//   headerRight  – (day: DayDescriptor) => ReactNode — optional node in the top-right of each day header
//   prevDisabled / nextDisabled – disable the respective navigation arrow
export default function WeeklyCalendar({
  weekStart,
  onPrevWeek,
  onNextWeek,
  title,
  subtitle,
  renderDay,
  headerRight,
  prevDisabled = false,
  nextDisabled = false,
}) {
  // Derive the seven DayDescriptor objects for the current week from the anchor date
  const weekDays = getWeekDays(weekStart)

  return (
    <section className="space-y-4">
      <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
        <div>
          {title && <h2 className="text-xl font-semibold tracking-tight text-slate-900">{title}</h2>}
          {subtitle && <p className="mt-1 text-sm text-slate-500">{subtitle}</p>}
        </div>

        {/* Week navigation: prev arrow – formatted range label – next arrow */}
        <div className="flex w-full items-center justify-between gap-2 sm:w-auto sm:justify-end">
          <button
            type="button"
            onClick={onPrevWeek}
            disabled={prevDisabled}
            aria-label="Settimana precedente"
            className="inline-flex h-10 w-10 items-center justify-center rounded-full border border-slate-200 bg-white text-slate-700 transition hover:border-indigo-300 hover:text-indigo-700 disabled:cursor-not-allowed disabled:opacity-40"
          >
            <ArrowLeft className="h-4 w-4" strokeWidth={1.5} />
          </button>
          <div className="min-w-0 flex-1 rounded-2xl bg-indigo-50 px-3 py-2 text-center text-xs leading-tight font-semibold text-indigo-700 shadow-sm sm:flex-none sm:px-4 sm:text-sm">
            {formatWeekRange(weekStart)}
          </div>
          <button
            type="button"
            onClick={onNextWeek}
            aria-label="Settimana successiva"
            className="inline-flex h-10 w-10 items-center justify-center rounded-full border border-slate-200 bg-white text-slate-700 transition hover:border-indigo-300 hover:text-indigo-700 disabled:cursor-not-allowed disabled:opacity-40"
            disabled={nextDisabled}
          >
            <ArrowRight className="h-4 w-4" strokeWidth={1.5} />
          </button>
        </div>
      </div>

      {/* On mobile: horizontal scroll with fixed-width columns; on md+: 7-column grid */}
      <div className="overflow-x-auto pb-2 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        <div className="grid grid-flow-col auto-cols-[minmax(15.5rem,1fr)] gap-3 md:grid-flow-row md:auto-cols-auto md:grid-cols-7">
          {weekDays.map((day) => (
            <div key={day.isoDate} className="flex min-h-60 snap-start flex-col rounded-2xl border border-slate-200 bg-white shadow-sm">
              <div className="border-b border-slate-100 px-4 py-3">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">{day.weekdayShortLabel}</p>
                    {/*<h3 className="text-sm leading-tight font-semibold text-slate-900">{day.weekdayLabel}</h3>*/}
                    <p className="text-xs text-slate-500">{day.formattedDate}</p>
                  </div>
                  {/* Optional per-day action or indicator injected by the parent */}
                  {headerRight ? <div className="shrink-0">{headerRight(day)}</div> : null}
                </div>
              </div>

              {/* Day body delegated entirely to the caller via renderDay */}
              <div className="min-w-0 flex-1 space-y-3 px-3 py-3">{renderDay(day)}</div>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
