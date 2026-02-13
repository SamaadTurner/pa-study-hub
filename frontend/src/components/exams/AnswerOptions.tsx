import type { AnswerOption } from '@/types'

interface Props {
  options: AnswerOption[]
  selectedId: string | null
  correctId: string | null    // only set after submission
  onSelect: (optionId: string) => void
  disabled?: boolean
}

const OPTION_LETTERS = ['A', 'B', 'C', 'D']

function getOptionStyle(
  option: AnswerOption,
  selectedId: string | null,
  correctId: string | null,
): string {
  const isSelected = option.id === selectedId
  const isCorrect  = option.id === correctId

  if (correctId !== null) {
    // Post-submission state
    if (isCorrect)  return 'border-green-400 bg-green-50 text-green-900'
    if (isSelected && !isCorrect) return 'border-red-400 bg-red-50 text-red-900'
    return 'border-gray-200 bg-white text-gray-500 opacity-60'
  }

  // Pre-submission: just highlight the selection
  if (isSelected) return 'border-primary-500 bg-primary-50 text-primary-900'
  return 'border-gray-200 bg-white text-gray-800 hover:border-primary-300 hover:bg-primary-50/40'
}

export default function AnswerOptions({
  options,
  selectedId,
  correctId,
  onSelect,
  disabled = false,
}: Props) {
  return (
    <div className="space-y-3" role="radiogroup">
      {options.map((option, idx) => {
        const letter = OPTION_LETTERS[idx] ?? String(idx + 1)
        const style  = getOptionStyle(option, selectedId, correctId)
        const isSelected = option.id === selectedId
        const isCorrect  = option.id === correctId
        const isAnswered = correctId !== null

        return (
          <button
            key={option.id}
            role="radio"
            aria-checked={isSelected}
            disabled={disabled || isAnswered}
            onClick={() => onSelect(option.id)}
            className={`w-full flex items-start gap-4 px-5 py-4 rounded-xl border-2
              transition-all text-left disabled:cursor-default ${style}`}
          >
            {/* Letter bubble */}
            <span className={`flex-shrink-0 w-7 h-7 rounded-full flex items-center
              justify-center text-sm font-bold border-2
              ${isAnswered && isCorrect  ? 'border-green-500 bg-green-500 text-white' :
                isAnswered && isSelected ? 'border-red-500 bg-red-500 text-white' :
                isSelected               ? 'border-primary-500 bg-primary-500 text-white' :
                                           'border-current bg-transparent'}`}>
              {isAnswered && isCorrect  ? '✓' :
               isAnswered && isSelected ? '✕' :
               letter}
            </span>

            <span className="text-sm leading-relaxed flex-1">{option.text}</span>
          </button>
        )
      })}
    </div>
  )
}
