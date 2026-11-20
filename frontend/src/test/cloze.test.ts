import { describe, it, expect } from 'vitest'
import { clozeNumbers, clozeProblem, nextClozeNumber, parseCloze, wrapAsBlank } from '@/utils/cloze'

const TEXT = '{{c2::Metoprolol}} is a {{c1::beta blocker::drug class}} that lowers {{c2::heart rate}}'

describe('parseCloze', () => {
  it('splits the text into plain parts and blanks, with hints', () => {
    expect(parseCloze('A {{c1::B::hint}} C')).toEqual([
      { kind: 'text', text: 'A ' },
      { kind: 'blank', index: 1, answer: 'B', hint: 'hint' },
      { kind: 'text', text: ' C' },
    ])
  })

  it('leaves text without blanks as one plain part', () => {
    expect(parseCloze('No blanks')).toEqual([{ kind: 'text', text: 'No blanks' }])
  })
})

describe('clozeNumbers and clozeProblem', () => {
  it('lists each number once, smallest first', () => {
    expect(clozeNumbers(TEXT)).toEqual([1, 2])
  })

  it('says what is wrong, with the same words as the server', () => {
    expect(clozeProblem('No blanks')).toBe('Mark at least one blank like this: {{c1::the answer}}')
    expect(clozeProblem('{{c21::x}}')).toBe('Blank numbers go from c1 to c20')
    expect(clozeProblem(TEXT)).toBeNull()
  })
})

describe('making blanks', () => {
  it('picks the next number', () => {
    expect(nextClozeNumber('plain')).toBe(1)
    expect(nextClozeNumber(TEXT)).toBe(3)
  })

  it('wraps the selection, and does nothing without one', () => {
    expect(wrapAsBlank('Aspirin inhibits COX', 17, 20, 1)).toBe('Aspirin inhibits {{c1::COX}}')
    expect(wrapAsBlank('Aspirin', 3, 3, 1)).toBe('Aspirin')
  })
})
