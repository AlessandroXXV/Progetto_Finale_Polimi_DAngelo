const statusColors = {
  PAYMENT_PENDING: 'bg-blue-100 text-blue-800',
  CONFIRMED: 'bg-green-100 text-green-800',
  COMPLETED: 'bg-indigo-100 text-indigo-800',
  CANCELLED: 'bg-red-100 text-red-800',
  AVAILABLE: 'bg-green-100 text-green-800',
  BOOKED: 'bg-orange-100 text-orange-800',
  CLIENT: 'bg-sky-100 text-sky-800',
  STUDENT: 'bg-purple-100 text-purple-800',
  ADMIN: 'bg-red-100 text-red-800',
  ONLINE: 'bg-teal-100 text-teal-800',
  IN_PERSON: 'bg-amber-100 text-amber-800',
  HYBRID: 'bg-violet-100 text-violet-800',
}

const statusLabels = {
  PAYMENT_PENDING: 'In attesa di pagamento',
  CONFIRMED: 'Confermata',
  COMPLETED: 'Completata',
  CANCELLED: 'Annullata',
  AVAILABLE: 'Disponibile',
  BOOKED: 'Prenotato',
  CLIENT: 'Cliente',
  STUDENT: 'Studente',
  ADMIN: 'Admin',
  ONLINE: 'Online',
  IN_PERSON: 'In Presenza',
  HYBRID: 'Ibrido',
}

export default function Badge({ value }) {
  // Map known status keys to readable labels and color tokens.
  const cls = statusColors[value] ?? 'bg-gray-100 text-gray-700'
  const label = statusLabels[value] ?? value
  return (
    <span className={`inline-flex max-w-full items-center rounded-full px-3 py-1 text-center text-[11px] leading-tight font-semibold uppercase tracking-wide whitespace-normal wrap-break-word ${cls}`}>
      {label}
    </span>
  )
}
