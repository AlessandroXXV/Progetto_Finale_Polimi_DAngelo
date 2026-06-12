// Date utilities for the weekly calendar UI.
// All functions treat Monday as day 0, following ISO week convention (JS Date uses Sunday = 0).
const WEEKDAY_LABELS = ['Lunedì', 'Martedì', 'Mercoledì', 'Giovedì', 'Venerdì', 'Sabato', 'Domenica']
const WEEKDAY_SHORT_LABELS = ['Lun', 'Mar', 'Mer', 'Gio', 'Ven', 'Sab', 'Dom']

// Returns Monday 00:00:00 local time of the ISO week that contains `date`.
// The +6 offset shifts Sunday (0) to 6 and Monday (1) to 0, making Monday the anchor.
export function getWeekStart(date = new Date()) {
  const result = new Date(date)
  const day = (result.getDay() + 6) % 7
  result.setHours(0, 0, 0, 0)
  result.setDate(result.getDate() - day)
  return result
}

export function addDays(date, days) {
  const result = new Date(date)
  result.setDate(result.getDate() + days)
  return result
}

// Formats a Date to YYYY-MM-DD for safe use as an <input type="date"> value.
export function toDateInputValue(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function isSameIsoDate(left, right) {
  return toDateInputValue(left) === toDateInputValue(right)
}

export function formatWeekRange(weekStart) {
  const weekEnd = addDays(weekStart, 6)
  const formatOptions = { day: '2-digit', month: 'short' }
  return `${weekStart.toLocaleDateString('it-IT', formatOptions)} - ${weekEnd.toLocaleDateString('it-IT', formatOptions)}`
}

// Builds the 7 DayDescriptor objects consumed by WeeklyCalendar, one per day starting Monday.
export function getWeekDays(weekStart) {
  return WEEKDAY_LABELS.map((weekdayLabel, index) => {
    const date = addDays(weekStart, index)
    return {
      index,
      weekdayLabel,
      weekdayShortLabel: WEEKDAY_SHORT_LABELS[index],
      date,
      isoDate: toDateInputValue(date),
      formattedDate: date.toLocaleDateString('it-IT', {
        day: '2-digit',
        month: 'short',
      }),
      fullDateLabel: date.toLocaleDateString('it-IT', {
        weekday: 'long',
        day: '2-digit',
        month: 'long',
      }),
    }
  })
}

