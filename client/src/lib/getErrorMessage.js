// Converts an Axios error into a localised Italian message for toast notifications.
// Resolution order: explicit error code in the payload → message-pattern matching → HTTP status code.
const DEFAULT_ERROR_MESSAGE = 'Qualcosa non ha funzionato. Riprova tra un attimo.'

const HTTP_STATUS_MESSAGES = {
  400: 'Controlla i dati inseriti e riprova.',
  401: 'La tua sessione è scaduta. Accedi di nuovo per continuare.',
  403: 'Non hai i permessi per completare questa operazione.',
  404: 'La risorsa che stai cercando non è disponibile.',
  409: "C'è già un conflitto con i dati correnti. Aggiorna e riprova.",
  413: 'Il file è troppo grande. Carica un file fino a 5MB.',
  429: 'Stai andando troppo veloce. Riprova tra qualche secondo.',
  500: 'I nostri server stanno facendo i capricci. Riprova tra poco.',
  502: 'Il servizio non risponde. Riprova tra poco.',
  503: 'Siamo momentaneamente in manutenzione. Torna tra qualche minuto.',
  504: 'La richiesta ha impiegato troppo tempo. Riprova.',
}

const CUSTOM_ERROR_MESSAGES = {
  SLOT_OVERLAP: 'Hai già uno slot in questo orario.',
  SLOT_ALREADY_BOOKED: 'Questa fascia è già stata prenotata. Scegline un\'altra.',
  SLOT_HAS_ACTIVE_BOOKINGS: 'Non puoi eliminare questo slot: ci sono prenotazioni ancora attive.',
  BOOKING_DATE_IN_PAST: 'Non puoi selezionare una data nel passato.',
  BOOKING_LOCKED_AFTER_TIMEOUT: 'Quella fascia è stata bloccata. Scegli un altro orario.',
  ACCOUNT_NOT_VERIFIED: 'Per prenotare, verifica prima il tuo account.',
  STRIPE_ONBOARDING_REQUIRED: 'Completa il collegamento a Stripe per continuare.',
  PROFILE_OWNERSHIP_MISMATCH: 'Questo slot non appartiene al servizio selezionato.',
  USERNAME_TAKEN: 'Questo username è già in uso. Scegline un altro.',
  EMAIL_TAKEN: 'Questa email è già registrata. Accedi o usa un indirizzo diverso.',
  INVALID_EMAIL: 'Inserisci un indirizzo email valido.',
  INVALID_IMAGE_FORMAT: 'Formato immagine non supportato. Usa JPG, PNG o WEBP.',
  IMAGE_TOO_LARGE: "L'immagine supera i 5MB consentiti. Comprimi il file e riprova.",
  REVIEW_ALREADY_EXISTS: 'Hai già lasciato una recensione per questa prenotazione.',
  RATING_OUT_OF_RANGE: 'Il voto deve essere compreso tra 1 e 5.',
  PAYMENT_NOT_PENDING: 'Questa prenotazione non è più in attesa di pagamento.',
  PAYMENT_CLIENT_ONLY: 'Solo chi ha effettuato la prenotazione può pagare.',
  PAYMENT_CAPTURE_NOT_ALLOWED: 'Il pagamento non è pronto per essere confermato.',
  STRIPE_ACCOUNT_NOT_CONNECTED: 'Collega Stripe per usare questa funzione.',
}

const MESSAGE_CODE_MATCHERS = [
  { pattern: /slot overlaps with existing availability slot/i, code: 'SLOT_OVERLAP' },
  { pattern: /availability slot is already booked/i, code: 'SLOT_ALREADY_BOOKED' },
  { pattern: /cannot delete slot with active booking/i, code: 'SLOT_HAS_ACTIVE_BOOKINGS' },
  { pattern: /booking date cannot be in the past/i, code: 'BOOKING_DATE_IN_PAST' },
  { pattern: /prenotazione bloccata in modo permanente/i, code: 'BOOKING_LOCKED_AFTER_TIMEOUT' },
  { pattern: /account is not verified/i, code: 'ACCOUNT_NOT_VERIFIED' },
  { pattern: /complete stripe onboarding/i, code: 'STRIPE_ONBOARDING_REQUIRED' },
  { pattern: /does not belong to the student/i, code: 'PROFILE_OWNERSHIP_MISMATCH' },
  { pattern: /username already taken/i, code: 'USERNAME_TAKEN' },
  { pattern: /email already taken/i, code: 'EMAIL_TAKEN' },
  { pattern: /invalid email/i, code: 'INVALID_EMAIL' },
  { pattern: /invalid image format/i, code: 'INVALID_IMAGE_FORMAT' },
  { pattern: /image size exceeds maximum allowed size of 5mb/i, code: 'IMAGE_TOO_LARGE' },
  { pattern: /review already exists for this booking/i, code: 'REVIEW_ALREADY_EXISTS' },
  { pattern: /rating must be between 1 and 5/i, code: 'RATING_OUT_OF_RANGE' },
  { pattern: /booking is not awaiting payment/i, code: 'PAYMENT_NOT_PENDING' },
  { pattern: /only the booking client can create a payment intent/i, code: 'PAYMENT_CLIENT_ONLY' },
  { pattern: /payment intent is not authorized for manual capture/i, code: 'PAYMENT_CAPTURE_NOT_ALLOWED' },
  { pattern: /stripe account not connected/i, code: 'STRIPE_ACCOUNT_NOT_CONNECTED' },
]

// Normalises a raw error code value to a consistent UPPER_SNAKE_CASE key for map lookup.
const normalizeCode = (value) =>
  String(value ?? '')
    .trim()
    .replace(/[^a-zA-Z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')
    .toUpperCase()

// Scans a raw error message string against known patterns to infer a semantic code.
const getCodeFromMessage = (message) => {
  if (!message || typeof message !== 'string') return ''
  const match = MESSAGE_CODE_MATCHERS.find(({ pattern }) => pattern.test(message))
  return match?.code ?? ''
}

const readResponsePayload = (error) => {
  const payload = error?.response?.data
  return payload && typeof payload === 'object' ? payload : {}
}

export function getErrorMessage(error) {
  if (!error) return DEFAULT_ERROR_MESSAGE

  if (error?.code === 'ERR_NETWORK') {
    return 'Sembra un problema di connessione. Controlla la rete e riprova.'
  }

  if (error?.code === 'ECONNABORTED') {
    return 'La richiesta sta impiegando troppo tempo. Riprova tra poco.'
  }

  const payload = readResponsePayload(error)
  const status = Number(error?.response?.status ?? payload?.status ?? error?.status)

  const explicitCodeCandidates = [payload?.errorCode, payload?.code, payload?.error_code, payload?.type]
  for (const candidate of explicitCodeCandidates) {
    const normalized = normalizeCode(candidate)
    if (normalized && CUSTOM_ERROR_MESSAGES[normalized]) {
      return CUSTOM_ERROR_MESSAGES[normalized]
    }
  }

  const messageCandidates = [
    payload?.message,
    payload?.error,
    payload?.detail,
    Array.isArray(payload?.errors) ? payload.errors.join(', ') : null,
    error?.message,
  ]
  for (const rawMessage of messageCandidates) {
    const inferredCode = getCodeFromMessage(rawMessage)
    if (inferredCode && CUSTOM_ERROR_MESSAGES[inferredCode]) {
      return CUSTOM_ERROR_MESSAGES[inferredCode]
    }
  }

  if (status && HTTP_STATUS_MESSAGES[status]) {
    return HTTP_STATUS_MESSAGES[status]
  }

  if (status >= 500) {
    return HTTP_STATUS_MESSAGES[500]
  }

  return DEFAULT_ERROR_MESSAGE
}
