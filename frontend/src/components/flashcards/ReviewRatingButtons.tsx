interface RatingButton {
  label: string
  quality: 1 | 2 | 4 | 5
  description: string
  colorClasses: string
}

const BUTTONS: RatingButton[] = [
  {
    label: 'Again',
    quality: 1,
    description: 'Forgot completely',
    colorClasses: 'bg-red-50 border-red-300 text-red-700 hover:bg-red-100',
  },
  {
    label: 'Hard',
    quality: 2,
    description: 'Difficult recall',
    colorClasses: 'bg-orange-50 border-orange-300 text-orange-700 hover:bg-orange-100',
  },
  {
    label: 'Good',
    quality: 4,
    description: 'Recalled with effort',
    colorClasses: 'bg-green-50 border-green-300 text-green-700 hover:bg-green-100',
  },
  {
    label: 'Easy',
    quality: 5,
    description: 'Perfect recall',
    colorClasses: 'bg-blue-50 border-blue-300 text-blue-700 hover:bg-blue-100',
  },
]

interface Props {
  onRate: (quality: 1 | 2 | 4 | 5) => void
  disabled?: boolean
}

export default function ReviewRatingButtons({ onRate, disabled = false }: Props) {
  return (
    <div className="w-full max-w-2xl mx-auto">
      <p className="text-center text-sm text-gray-500 mb-3">How well did you remember?</p>
      <div className="grid grid-cols-4 gap-2">
        {BUTTONS.map(btn => (
          <button
            key={btn.quality}
            onClick={() => onRate(btn.quality)}
            disabled={disabled}
            className={`flex flex-col items-center gap-1 px-2 py-3 rounded-xl border-2
              font-semibold text-sm transition-all disabled:opacity-40
              disabled:cursor-not-allowed active:scale-95 ${btn.colorClasses}`}
          >
            <span>{btn.label}</span>
            <span className="text-xs font-normal opacity-70">{btn.description}</span>
          </button>
        ))}
      </div>
    </div>
  )
}
