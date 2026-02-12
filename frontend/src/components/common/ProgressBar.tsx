interface Props {
  value: number      // 0–100
  max?: number       // defaults to 100
  label?: string
  showPercent?: boolean
  color?: 'primary' | 'success' | 'warning' | 'danger'
  size?: 'sm' | 'md' | 'lg'
}

const COLOR_CLASSES = {
  primary: 'bg-primary-600',
  success: 'bg-green-500',
  warning: 'bg-yellow-500',
  danger:  'bg-red-500',
}

const SIZE_CLASSES = {
  sm: 'h-1.5',
  md: 'h-2.5',
  lg: 'h-4',
}

function getAutoColor(pct: number) {
  if (pct >= 80) return 'bg-green-500'
  if (pct >= 50) return 'bg-yellow-500'
  return 'bg-red-500'
}

export default function ProgressBar({
  value,
  max = 100,
  label,
  showPercent = false,
  color,
  size = 'md',
}: Props) {
  const pct = Math.min(100, Math.max(0, (value / max) * 100))
  const barColor = color ? COLOR_CLASSES[color] : getAutoColor(pct)

  return (
    <div className="w-full">
      {(label || showPercent) && (
        <div className="flex justify-between items-center mb-1">
          {label && <span className="text-sm text-gray-600">{label}</span>}
          {showPercent && (
            <span className="text-sm font-medium text-gray-700">{Math.round(pct)}%</span>
          )}
        </div>
      )}
      <div className={`w-full bg-gray-200 rounded-full overflow-hidden ${SIZE_CLASSES[size]}`}>
        <div
          className={`${barColor} ${SIZE_CLASSES[size]} rounded-full transition-all duration-500 ease-out`}
          style={{ width: `${pct}%` }}
          role="progressbar"
          aria-valuenow={value}
          aria-valuemin={0}
          aria-valuemax={max}
        />
      </div>
    </div>
  )
}
