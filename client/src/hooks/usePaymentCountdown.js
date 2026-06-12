// Hook that tracks the remaining time for a booking awaiting payment.
// It ticks every second only while the booking is in PAYMENT_PENDING status,
// and exposes derived values (progress percentage, formatted string, expiry flag)
// so the caller does not need to perform any time arithmetic.
import { useEffect, useMemo, useState } from 'react'

const DEFAULT_TIMEOUT_MINUTES = 10

// Converts milliseconds to a zero-padded "MM:SS" string, clamping at "00:00".
const formatMmSs = (ms) => {
  const totalSeconds = Math.max(0, Math.ceil(ms / 1000))
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

// Safely parses any date-like value to a Unix timestamp in milliseconds.
// Returns 0 for invalid or missing values so callers can treat 0 as "no deadline".
const toMillis = (value) => {
  if (!value) return 0
  const parsed = new Date(value).getTime()
  return Number.isFinite(parsed) ? parsed : 0
}

export default function usePaymentCountdown(booking, timeoutMinutes = DEFAULT_TIMEOUT_MINUTES) {
  const [now, setNow] = useState(() => Date.now())

  const isPaymentPending = booking?.status === 'PAYMENT_PENDING'
  const bookedAtMs = toMillis(booking?.bookedAt)
  const totalMs = timeoutMinutes * 60 * 1000
  // Deadline is the booking creation time plus the allowed payment window.
  const deadlineMs = bookedAtMs > 0 ? bookedAtMs + totalMs : 0

  // Start the 1-second ticker only when there is an active payment deadline.
  useEffect(() => {
    if (!isPaymentPending || bookedAtMs <= 0) return undefined

    const timer = setInterval(() => {
      setNow(Date.now())
    }, 1000)

    return () => clearInterval(timer)
  }, [isPaymentPending, bookedAtMs])

  // Recompute derived values on every tick; memoised to avoid unnecessary re-renders
  // in consumers that only care about specific fields.
  return useMemo(() => {
    if (!isPaymentPending || bookedAtMs <= 0) {
      return {
        isVisible: false,
        isExpired: false,
        remainingMs: 0,
        progressPct: 0,
        formatted: '00:00',
      }
    }

    const remainingMs = Math.max(0, deadlineMs - now)
    // progressPct represents elapsed time (0 = just started, 100 = fully elapsed).
    const elapsed = Math.max(0, totalMs - remainingMs)
    const progressPct = Math.min(100, Math.max(0, (elapsed / totalMs) * 100))

    return {
      isVisible: true,
      isExpired: remainingMs <= 0,
      remainingMs,
      progressPct,
      formatted: formatMmSs(remainingMs),
    }
  }, [isPaymentPending, bookedAtMs, deadlineMs, now, totalMs])
}

