import useProfileImage from '../../hooks/useProfileImage'

export default function UserAvatar({
  userId,
  username,
  sizeClassName = 'w-10 h-10',
  textClassName = 'text-sm',
}) {
  const imageUrl = useProfileImage(userId)
  const fallbackLabel = (username?.[0] ?? '?').toUpperCase()

  return (
    <div
      // Circular avatar container: show the uploaded image when available, otherwise the initial.
      className={`${sizeClassName} overflow-hidden shrink-0 flex items-center justify-center rounded-full border border-white bg-linear-to-br from-indigo-100 to-violet-100 text-indigo-600 shadow-sm ring-1 ring-slate-200/60`}
      aria-label={`Avatar ${username ?? 'utente'}`}
    >
      {imageUrl ? (
        <img src={imageUrl} alt={`Avatar ${username ?? 'utente'}`} className="w-full h-full object-cover" />
      ) : (
        <span className={`font-semibold ${textClassName}`}>{fallbackLabel}</span>
      )}
    </div>
  )
}
