import { getCategoryLabel } from '@/utils/categoryColors'

interface Props {
  selected: string | null
  onChange: (category: string | null) => void
  availableCategories: string[]
}

// Topic filter: a row of buttons on wider screens, a dropdown on a phone, where
// the 17 buttons took 8 rows before the first deck.
export default function CategoryFilter({ selected, onChange, availableCategories }: Props) {
  if (availableCategories.length === 0) return null

  return (
    <>
      <div className="sm:hidden">
        <label htmlFor="category-filter" className="sr-only">Topic</label>
        <select
          id="category-filter"
          value={selected ?? ''}
          onChange={e => onChange(e.target.value || null)}
          className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white
            focus:outline-none focus:ring-2 focus:ring-primary-500"
        >
          <option value="">All topics</option>
          {availableCategories.map(cat => (
            <option key={cat} value={cat}>{getCategoryLabel(cat)}</option>
          ))}
        </select>
      </div>

      <div className="hidden sm:flex flex-wrap gap-2" role="group" aria-label="Topic">
        <button
          onClick={() => onChange(null)}
          aria-pressed={selected === null}
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
            aria-pressed={selected === cat}
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
    </>
  )
}
