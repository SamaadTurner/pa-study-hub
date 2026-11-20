// Fill-in-the-blank ("cloze") text in Anki's syntax: {{c1::answer}} or, with a
// hint, {{c1::answer::hint}}. Every number becomes its own card; a card hides
// only the blanks with its own number. Same rules as ClozeText.java.

export const MAX_CLOZE_NUMBER = 20

export type ClozePart =
  | { kind: 'text'; text: string }
  | { kind: 'blank'; index: number; answer: string; hint: string | null }

// A new RegExp each call: a shared one with the g flag remembers where it stopped
function blankPattern(): RegExp {
  return /\{\{c(\d{1,2})::([\s\S]+?)(?:::([\s\S]*?))?\}\}/g
}

export function parseCloze(text: string): ClozePart[] {
  const parts: ClozePart[] = []
  const pattern = blankPattern()
  let last = 0
  let match: RegExpExecArray | null
  while ((match = pattern.exec(text)) !== null) {
    if (match.index > last) parts.push({ kind: 'text', text: text.slice(last, match.index) })
    parts.push({ kind: 'blank', index: Number(match[1]), answer: match[2], hint: match[3] ? match[3] : null })
    last = match.index + match[0].length
  }
  if (last < text.length) parts.push({ kind: 'text', text: text.slice(last) })
  return parts
}

// The blank numbers in the text, smallest first, each once
export function clozeNumbers(text: string): number[] {
  const numbers = new Set<number>()
  for (const part of parseCloze(text)) {
    if (part.kind === 'blank') numbers.add(part.index)
  }
  return [...numbers].sort((a, b) => a - b)
}

// What is wrong with the text as a cloze card, or null when it is fine
export function clozeProblem(text: string): string | null {
  const numbers = clozeNumbers(text)
  if (numbers.length === 0) return 'Mark at least one blank like this: {{c1::the answer}}'
  if (numbers[0] < 1 || numbers[numbers.length - 1] > MAX_CLOZE_NUMBER) {
    return `Blank numbers go from c1 to c${MAX_CLOZE_NUMBER}`
  }
  return null
}

// The number for a new blank: one more than the highest so far
export function nextClozeNumber(text: string): number {
  const numbers = clozeNumbers(text)
  return numbers.length === 0 ? 1 : numbers[numbers.length - 1] + 1
}

// Wraps the selected part of the text (start to end) in a blank with that number.
// Nothing selected: returns the text unchanged.
export function wrapAsBlank(text: string, start: number, end: number, number: number): string {
  if (start >= end) return text
  return `${text.slice(0, start)}{{c${number}::${text.slice(start, end)}}}${text.slice(end)}`
}
