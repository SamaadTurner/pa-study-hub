import { useNavigate } from 'react-router-dom'
import { getCategoryLabel } from '@/utils/categoryColors'

interface CategoryAccuracy {
  category: string
  accuracy: number
}

interface Props {
  categoryAccuracy: Record<string, number>
}

const WEAK_THRESHOLD = 70

export default function WeakAreasAlert({ categoryAccuracy }: Props) {
  const navigate = useNavigate()

  const weakAreas: CategoryAccuracy[] = Object.entries(categoryAccuracy)
    .filter(([, acc]) => acc < WEAK_THRESHOLD && acc > 0)
    .sort(([, a], [, b]) => a - b)
    .slice(0, 3)
    .map(([category, accuracy]) => ({ category, accuracy }))

  if (weakAreas.length === 0) return null

  return (
    <div className="bg-yellow-50 border border-yellow-200 rounded-xl p-5">
      <div className="flex items-center gap-2 mb-3">
        <span className="text-lg">⚠️</span>
        <h3 className="text-sm font-semibold text-yellow-900">Focus Areas</h3>
      </div>
      <div className="space-y-2">
        {weakAreas.map(({ category, accuracy }) => (
          <div key={category} className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div className="w-2 h-2 rounded-full bg-yellow-500" />
              <span className="text-sm text-gray-800">{getCategoryLabel(category)}</span>
              <span className="text-xs text-red-600 font-medium">{Math.round(accuracy)}%</span>
            </div>
            <button
              onClick={() => navigate(`/decks?category=${category}`)}
              className="text-xs text-primary-600 hover:text-primary-800 font-medium"
            >
              Review →
            </button>
          </div>
        ))}
      </div>
    </div>
  )
}
