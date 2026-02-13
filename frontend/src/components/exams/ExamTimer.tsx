import { useEffect, useState, useRef } from 'react'
import { formatCountdown } from '@/utils/formatDuration'

interface Props {
  initialSeconds: number
  onExpire: () => void
  paused?: boolean
}

export default function ExamTimer({ initialSeconds, onExpire, paused = false }: Props) {
  const [remaining, setRemaining] = useState(initialSeconds)
  const onExpireRef = useRef(onExpire)
  onExpireRef.current = onExpire

  useEffect(() => {
    if (paused || remaining <= 0) return
    const id = setInterval(() => {
      setRemaining(prev => {
        if (prev <= 1) {
          clearInterval(id)
          onExpireRef.current()
          return 0
        }
        return prev - 1
      })
    }, 1000)
    return () => clearInterval(id)
  }, [paused, remaining])

  const pct = initialSeconds > 0 ? (remaining / initialSeconds) * 100 : 0
  const isWarning  = pct <= 25 && pct > 10
  const isDanger   = pct <= 10

  const colorClass = isDanger
    ? 'text-red-600 bg-red-50 border-red-200'
    : isWarning
    ? 'text-yellow-600 bg-yellow-50 border-yellow-200'
    : 'text-gray-700 bg-gray-50 border-gray-200'

  return (
    <div className={`flex items-center gap-2 px-3 py-1.5 rounded-lg border font-mono
      text-sm font-semibold transition-colors ${colorClass}`}>
      <span>⏱</span>
      <span>{formatCountdown(remaining)}</span>
    </div>
  )
}
