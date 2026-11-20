import { parseCloze } from '@/utils/cloze'

interface Props {
  text: string
  // The blank this card asks for; null shows every answer (the card list)
  index: number | null
  revealed: boolean
}

/**
 * A cloze card's text. The card's own blank shows as [...] (or [hint]) until it is
 * revealed, then as the highlighted answer. Other blanks always show their answer.
 */
export default function ClozeView({ text, index, revealed }: Props) {
  return (
    <span className="whitespace-pre-wrap">
      {parseCloze(text).map((part, i) => {
        if (part.kind === 'text') return <span key={i}>{part.text}</span>
        const isAsked = index === null || part.index === index
        if (!isAsked) return <span key={i}>{part.answer}</span>
        if (!revealed) {
          return (
            <span key={i} className="px-1.5 rounded bg-primary-100 text-primary-700 font-semibold">
              [{part.hint ?? '...'}]
            </span>
          )
        }
        return (
          <mark key={i} className="px-1 rounded bg-yellow-200 text-gray-900 font-semibold">{part.answer}</mark>
        )
      })}
    </span>
  )
}
