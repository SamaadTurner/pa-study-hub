import type { TaskAreaScore } from '@/types'
import { getTaskAreaLabel } from '@/utils/taskAreas'

// Same colours as the score bands on the results page: 80+ green, 60+ amber, else red
function barColor(percent: number): string {
  if (percent >= 80) return 'bg-green-500'
  if (percent >= 60) return 'bg-amber-500'
  return 'bg-red-500'
}

// One row per task area: name, right out of total, and a bar for the percentage.
// A plain list instead of a chart, so the numbers can be read without hovering.
export default function TaskAreaScores({ scores }: { scores: TaskAreaScore[] }) {
  return (
    <ul className="space-y-3">
      {scores.map(score => (
        <li key={score.taskArea} aria-label={`${getTaskAreaLabel(score.taskArea)}: ${score.correct} of ${score.total} right`}>
          <div className="flex items-baseline justify-between gap-3 text-sm">
            <span className="text-gray-800">{getTaskAreaLabel(score.taskArea)}</span>
            <span className="text-gray-500 tabular-nums whitespace-nowrap">
              {score.correct}/{score.total} · {Math.round(score.percent)}%
            </span>
          </div>
          <div className="mt-1 h-2 rounded-full bg-gray-100 overflow-hidden">
            <div
              className={`h-full rounded-full ${barColor(score.percent)}`}
              style={{ width: `${score.percent}%` }}
            />
          </div>
        </li>
      ))}
    </ul>
  )
}
