// Page that allows any authenticated user to update their account information,
// upload/remove a profile picture, and request identity verification.
import { useState, useRef } from 'react'
import { userService } from '../../services/userService'
import useAuthStore from '../../store/authStore'
import toast from 'react-hot-toast'
import Badge from '../../components/common/Badge'
import IdentityVerificationModal from '../../components/common/IdentityVerificationModal'
import useProfileImage, { invalidateProfileImageCache } from '../../hooks/useProfileImage'
import { User } from 'lucide-react'

export default function AccountSettingsPage() {
  const { user, login, token, bumpProfileImageRevision } = useAuthStore()
  // Only fields that are actually editable are pre-filled; fullName/gender/password start empty
  // because they are optional overrides rather than read from the stored profile.
  const [form, setForm] = useState({
    username: user?.username ?? '',
    email: user?.email ?? '',
    fullName: '',
    gender: '',
    password: '',
  })
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [verificationModalOpen, setVerificationModalOpen] = useState(false)
  // Hidden file input triggered programmatically via this ref
  const fileRef = useRef()
  const imageUrl = useProfileImage(user?.userId)

  const handleChange = (e) =>
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleSave = async (e) => {
    e.preventDefault()
    setSaving(true)
    // Only include fields that actually changed to avoid overwriting with empty values
    const payload = {}
    if (form.username !== user?.username) payload.username = form.username
    if (form.email !== user?.email) payload.email = form.email
    if (form.password) payload.password = form.password
    if (form.fullName) payload.fullName = form.fullName
    if (form.gender) payload.gender = form.gender

    try {
      const { data } = await userService.updateMe(payload)
      // Refresh auth store with new values
      login({
        token,
        refreshToken: useAuthStore.getState().refreshToken,
        userId: data.id,
        username: data.username,
        email: data.email,
        role: data.role,
        stripeConnected: data.stripeConnected,
        isVerified: data.isVerified,
      })
      toast.success('Profilo aggiornato!')
      setForm((f) => ({ ...f, password: '' }))
    } catch (err) {
      toast.error(err.response?.data?.message ?? 'Errore aggiornamento')
    } finally {
      setSaving(false)
    }
  }

  const handleImageUpload = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    const maxMB = 3
    if (file.size > maxMB * 1024 * 1024) {
      toast.error(`Immagine troppo grande (max ${maxMB}MB)`)
      return
    }
    setUploading(true)
    try {
      await userService.uploadProfileImage(file)
      // Invalidate the in-memory cache so the new image is re-fetched everywhere
      invalidateProfileImageCache(user?.userId)
      bumpProfileImageRevision()
      toast.success('Immagine aggiornata!')
    } catch {
      toast.error('Errore caricamento immagine')
    } finally {
      // Reset the file input so the same file can be selected again if needed
      if (fileRef.current) fileRef.current.value = ''
      setUploading(false)
    }
  }

  const handleDeleteImage = async () => {
    try {
      await userService.deleteProfileImage()
      invalidateProfileImageCache(user?.userId)
      bumpProfileImageRevision()
      toast.success('Immagine rimossa')
    } catch {
      toast.error('Errore rimozione immagine')
    } finally {
      if (fileRef.current) fileRef.current.value = ''
    }
  }

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Impostazioni Account</h1>

      {/* Profile Image */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 mb-6">
        <h2 className="text-lg font-semibold text-gray-700 mb-4">Foto Profilo</h2>
        <div className="flex items-center gap-6">
          <div className="w-20 h-20 rounded-full bg-indigo-100 flex items-center justify-center overflow-hidden shrink-0">
            {imageUrl ? (
              <img src={imageUrl} alt="Profile" className="w-full h-full object-cover" />
            ) : (
              <User className="h-9 w-9 text-indigo-400" strokeWidth={1.5} />
            )}
          </div>
          <div className="flex gap-3 flex-wrap">
            {/* Clicking this button programmatically triggers the hidden file input */}
            <button
              onClick={() => fileRef.current?.click()}
              disabled={uploading}
              className="text-sm bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-lg disabled:opacity-60"
            >
              {uploading ? 'Caricamento…' : 'Carica Foto'}
            </button>
            <button
              onClick={handleDeleteImage}
              className="text-sm bg-red-50 hover:bg-red-100 text-red-600 px-4 py-2 rounded-lg border border-red-200"
            >
              Rimuovi
            </button>
          </div>
          <input
            ref={fileRef}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            className="hidden"
            onChange={handleImageUpload}
          />
        </div>
        <p className="text-xs text-gray-400 mt-3">JPG, PNG, WEBP • Max 5MB</p>
      </div>

      {/* Account Info */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 mb-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-gray-700">Dati Account</h2>
          <Badge value={user?.role} />
        </div>
        <form onSubmit={handleSave} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Username</label>
              <input
                type="text"
                name="username"
                value={form.username}
                onChange={handleChange}
                minLength={3}
                maxLength={20}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
              <input
                type="email"
                name="email"
                value={form.email}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Nome Completo</label>
              <input
                type="text"
                name="fullName"
                value={form.fullName}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                placeholder="Mario Rossi"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Genere</label>
              <select
                name="gender"
                value={form.gender}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                <option value="">— seleziona —</option>
                <option value="M">Maschile</option>
                <option value="F">Femminile</option>
                <option value="Other">Altro</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Nuova Password <span className="text-gray-400">(lascia vuoto per non cambiare)</span>
            </label>
            <input
              type="password"
              name="password"
              value={form.password}
              onChange={handleChange}
              minLength={8}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="••••••••"
            />
          </div>

          <button
            type="submit"
            disabled={saving}
            className="bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60 text-white font-semibold px-6 py-2.5 rounded-lg transition-colors"
          >
            {saving ? 'Salvataggio…' : 'Salva Modifiche'}
          </button>
        </form>
      </div>

      {/* Document verification — opens a modal to upload ID documents as PDFs */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <div className="flex items-center justify-between gap-4">
          <div>
            <h2 className="text-lg font-semibold text-gray-700">Verifica identità</h2>
            <p className="text-sm text-gray-500">Carica fronte e retro del documento in PDF per inviare la richiesta via email.</p>
          </div>
          <button
            type="button"
            onClick={() => setVerificationModalOpen(true)}
            className="bg-indigo-600 hover:bg-indigo-700 text-white font-semibold px-4 py-2 rounded-lg transition-colors"
          >
            Apri finestra
          </button>
        </div>
      </div>

      <IdentityVerificationModal
        isOpen={verificationModalOpen}
        onClose={() => setVerificationModalOpen(false)}
        onSuccess={() => toast.success('Richiesta documenti inviata')}
      />
    </div>
  )
}
