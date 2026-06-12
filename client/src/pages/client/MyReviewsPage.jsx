// Page that lists all reviews left by the authenticated client, with rating and comment.
import { useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { reviewService } from '../../services/reviewService'
import useAuthStore from '../../store/authStore'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import StarRating from '../../components/common/StarRating'
import { Star } from 'lucide-react'
import toast from 'react-hot-toast'

export default function MyReviewsPage() {
  const { user } = useAuthStore()

  const { data: reviews = [], isLoading: loading, error } = useQuery({
    queryKey: ['reviews', 'my', user.userId],
    queryFn: () => reviewService.getByClientDetails(user.userId).then((r) => r.data),
  })

  useEffect(() => {
    if (error) toast.error('Errore caricamento recensioni')
  }, [error])

  if (loading) return <LoadingSpinner />

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-800 mb-2">Le mie Recensioni</h1>
        <p className="text-gray-500">Tutte le recensioni che hai lasciato</p>
      </div>

      {reviews.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <Star className="mx-auto mb-4 h-12 w-12" strokeWidth={1.5} />
          <p className="text-lg">Nessuna recensione ancora</p>
          <p className="text-sm mt-2">Completa una sessione per lasciare una recensione</p>
        </div>
      ) : (
        <div className="space-y-4">
          {reviews.map((r) => (
            <div key={r.id} className="bg-white rounded-xl border border-gray-200 p-6">
              <div className="flex items-start justify-between mb-3">
                <div>
                  <h3 className="font-semibold text-gray-800">{r.serviceTitle}</h3>
                  <p className="text-sm text-gray-500">Tutor: @{r.studentUsername}</p>
                </div>
                <div className="flex flex-col items-end gap-1">
                  <StarRating rating={r.rating} />
                  <span className="text-xs text-gray-400">
                    {new Date(r.reviewedAt).toLocaleDateString('it-IT')}
                  </span>
                </div>
              </div>
              {r.comment && <p className="text-sm text-gray-600 italic">"{r.comment}"</p>}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
