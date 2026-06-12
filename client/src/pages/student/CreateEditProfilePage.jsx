// Page used both to create a new tutor profile and to edit an existing one.
// The presence of an :id URL param determines which mode is active.
import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { profileService } from '../../services/profileService'
import useAuthStore from '../../store/authStore'
import toast from 'react-hot-toast'
import { ArrowLeft, CreditCard } from 'lucide-react'

// Default shape for the controlled form; reused as the reset value after submission.
const emptyForm = {
  title: '',
  description: '',
  hourlyRate: '',
  deliveryMode: 'ONLINE',
  address: '',
}

export default function CreateEditProfilePage() {
  const { id } = useParams()
  const isEdit = !!id
  const navigate = useNavigate()
  const { user } = useAuthStore()
  const [form, setForm] = useState(emptyForm)
  // Start in loading state only when editing, so the form waits for fetched data.
  const [loading, setLoading] = useState(isEdit)
  const [saving, setSaving] = useState(false)

  // In edit mode, pre-populate the form with the existing profile data.
  useEffect(() => {
    if (!isEdit) return
    profileService.getById(id)
      .then(({ data }) => {
        setForm({
          title: data.title,
          description: data.description ?? '',
          // Stored as a number; convert to string for the controlled input.
          hourlyRate: data.hourlyRate.toString(),
          deliveryMode: data.deliveryMode,
          address: data.address ?? '',
        })
      })
      .catch(() => toast.error('Errore caricamento profilo'))
      .finally(() => setLoading(false))
  }, [id, isEdit])

  // Generic handler for all text/select inputs; uses the element's name attribute as the key.
  const handleChange = (e) =>
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    // Address is required only for in-person or hybrid delivery modes.
    const needsAddress = ['IN_PERSON', 'HYBRID'].includes(form.deliveryMode)
    if (needsAddress && !form.address.trim()) {
      toast.error("L'indirizzo è obbligatorio per In Presenza / Ibrido")
      return
    }
    setSaving(true)
    const payload = {
      title: form.title,
      description: form.description || undefined,
      hourlyRate: parseFloat(form.hourlyRate),
      deliveryMode: form.deliveryMode,
      // Omit address entirely for online-only profiles.
      address: needsAddress ? form.address : undefined,
    }
    try {
      if (isEdit) {
        await profileService.update(id, payload)
        toast.success('Profilo aggiornato!')
      } else {
        await profileService.create(payload)
        toast.success('Profilo creato!')
      }
      navigate('/student/profiles')
    } catch (err) {
      toast.error(err.response?.data?.message ?? 'Errore salvataggio')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <div className="text-center py-12 text-gray-400">Caricamento…</div>

  // Guard: Stripe Connect must be active before a new profile can be published.
  if (!isEdit && !user?.stripeConnected) {
    return (
      <div className="max-w-2xl mx-auto text-center py-16">
        <div className="mb-6 flex justify-center text-indigo-600"><CreditCard className="h-14 w-14" strokeWidth={1.5} /></div>
        <h1 className="text-2xl font-bold text-gray-800 mb-3">Completa Stripe prima di creare un profilo</h1>
        <p className="text-gray-500 mb-8 leading-relaxed">
          Per pubblicare nuovi servizi devi prima attivare Stripe Connect.
        </p>
        <button
          type="button"
          onClick={() => navigate('/student/stripe')}
          className="bg-indigo-600 hover:bg-indigo-700 text-white font-semibold px-8 py-3 rounded-xl transition-colors text-sm"
        >
          Vai a Stripe
        </button>
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto">
      <button onClick={() => navigate(-1)} className="inline-flex items-center gap-1 text-sm text-indigo-600 hover:underline mb-6">
        <ArrowLeft className="h-4 w-4" strokeWidth={1.5} /> Indietro
      </button>
      <h1 className="text-2xl font-bold text-gray-800 mb-8">
        {isEdit ? 'Modifica Profilo' : 'Crea Nuovo Profilo'}
      </h1>

      <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-8">
        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Titolo Servizio *</label>
            <input
              type="text"
              name="title"
              value={form.title}
              onChange={handleChange}
              required
              maxLength={255}
              className="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="es. Lezioni di Matematica per Liceo"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Descrizione</label>
            <textarea
              name="description"
              value={form.description}
              onChange={handleChange}
              rows={4}
              className="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 resize-none"
              placeholder="Descrivi il tuo servizio, la tua esperienza, il metodo di insegnamento…"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Tariffa Oraria (€) *
              </label>
              <input
                type="number"
                name="hourlyRate"
                value={form.hourlyRate}
                onChange={handleChange}
                required
                min="0.01"
                step="0.01"
                className="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                placeholder="25.00"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Modalità *</label>
              <select
                name="deliveryMode"
                value={form.deliveryMode}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                <option value="ONLINE">Online</option>
                <option value="IN_PERSON">In Presenza</option>
                <option value="HYBRID">Ibrido</option>
              </select>
            </div>
          </div>

          {/* Address field is conditionally rendered based on delivery mode. */}
          {['IN_PERSON', 'HYBRID'].includes(form.deliveryMode) && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Indirizzo *
              </label>
              <input
                type="text"
                name="address"
                value={form.address}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                placeholder="Via Roma 1, Milano"
              />
            </div>
          )}

          <div className="flex gap-3 pt-2">
            <button
              type="submit"
              disabled={saving}
              className="flex-1 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60 text-white font-semibold py-3 rounded-xl transition-colors"
            >
              {saving ? 'Salvataggio…' : isEdit ? 'Aggiorna Profilo' : 'Crea Profilo'}
            </button>
            <button
              type="button"
              onClick={() => navigate('/student/profiles')}
              className="px-6 bg-gray-100 hover:bg-gray-200 text-gray-700 font-semibold py-3 rounded-xl transition-colors"
            >
              Annulla
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
