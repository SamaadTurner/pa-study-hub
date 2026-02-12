interface Props {
  value: number   // 0–100
  size?: number   // SVG size in px
  strokeWidth?: number
  label?: string
  sublabel?: string
}

function getColor(pct: number): string {
  if (pct >= 100) return '#22c55e'  // green-500
  if (pct >= 60)  return '#eab308'  // yellow-500
  return '#ef4444'                   // red-500
}

export default function CircularProgress({
  value,
  size = 96,
  strokeWidth = 8,
  label,
  sublabel,
}: Props) {
  const pct     = Math.min(100, Math.max(0, value))
  const radius  = (size - strokeWidth) / 2
  const circumference = 2 * Math.PI * radius
  const offset  = circumference - (pct / 100) * circumference
  const color   = getColor(pct)

  return (
    <div className="flex flex-col items-center gap-1">
      <div className="relative" style={{ width: size, height: size }}>
        <svg width={size} height={size} className="-rotate-90">
          {/* Track */}
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            fill="none"
            stroke="#e5e7eb"
            strokeWidth={strokeWidth}
          />
          {/* Progress */}
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            fill="none"
            stroke={color}
            strokeWidth={strokeWidth}
            strokeDasharray={circumference}
            strokeDashoffset={offset}
            strokeLinecap="round"
            style={{ transition: 'stroke-dashoffset 0.6s ease-out' }}
          />
        </svg>
        {/* Center text */}
        <div className="absolute inset-0 flex items-center justify-center">
          <span className="text-lg font-bold text-gray-900">{Math.round(pct)}%</span>
        </div>
      </div>
      {label && <p className="text-sm font-medium text-gray-700 text-center">{label}</p>}
      {sublabel && <p className="text-xs text-gray-500 text-center">{sublabel}</p>}
    </div>
  )
}
