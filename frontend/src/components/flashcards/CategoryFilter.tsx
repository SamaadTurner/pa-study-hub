import { getCategoryLabel } from '@/utils/categoryColors'

interface Props {
  selected: string | null
  onChange: (category: string | null) => void
  availableCategories: string[]
}

export default function CategoryFilter({ selected, onChange, availableCategories }: Props) {
  if (availableCategories.length === 0) return null

  return (
    <div className="flex flex-wrap gap-2">
      <button
        onClick={() => onChange(null)}
        className={`px-3 py-1.5 rounded-full text-sm font-medium transition-colors
          ${selected === null
            ? 'bg-primary-600 text-white'
            : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
          }`}
      >
        All
      </button>
      {availableCategories.map(cat => (
        <button
          key={cat}
          onClick={() => onChange(selected === cat ? null : cat)}
          className={`px-3 py-1.5 rounded-full text-sm font-medium transition-colors
            ${selected === cat
              ? 'bg-primary-600 text-white'
              : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
        >
          {getCategoryLabel(cat)}
        </button>
      ))}
    </div>
  )
}
