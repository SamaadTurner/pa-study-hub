import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import Badge from '@/components/common/Badge'
import StreakWidget from '@/components/dashboard/StreakWidget'
import ReviewRatingButtons from '@/components/flashcards/ReviewRatingButtons'
import CircularProgress from '@/components/common/CircularProgress'
import DeckCard from '@/components/flashcards/DeckCard'
import type { Deck } from '@/types'

// Mock react-router-dom's navigate so DeckCard tests don't need full routing
const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return { ...actual, useNavigate: () => mockNavigate }
})

// ── Badge ──────────────────────────────────────────────────────────────────
describe('Badge', () => {
  it('renders with correct text', () => {
    render(<Badge variant="red">Cardiology</Badge>)
    expect(screen.getByText('Cardiology')).toBeInTheDocument()
  })

  it('renders with blue variant class', () => {
    const { container } = render(<Badge variant="blue">Pulmonology</Badge>)
    expect(container.firstChild).toHaveClass('bg-blue-100')
  })

  it('renders with gray variant by default', () => {
    const { container } = render(<Badge>Default</Badge>)
    expect(container.firstChild).toHaveClass('bg-gray-100')
  })
})

// ── StreakWidget ───────────────────────────────────────────────────────────
describe('StreakWidget', () => {
  it('displays fire emoji and streak number', () => {
    render(<StreakWidget currentStreak={12} longestStreak={20} totalStudyDays={45} />)
    expect(screen.getByText('🔥')).toBeInTheDocument()
    expect(screen.getByText('12')).toBeInTheDocument()
  })

  it('displays longest streak', () => {
    render(<StreakWidget currentStreak={5} longestStreak={30} totalStudyDays={60} />)
    expect(screen.getByText('30 days')).toBeInTheDocument()
  })

  it('displays total study days', () => {
    render(<StreakWidget currentStreak={1} longestStreak={1} totalStudyDays={7} />)
    expect(screen.getByText(/7 total study/)).toBeInTheDocument()
  })
})

// ── CircularProgress ───────────────────────────────────────────────────────
describe('CircularProgress', () => {
  it('shows correct percentage', () => {
    render(<CircularProgress value={73} label="Minutes" />)
    expect(screen.getByText('73%')).toBeInTheDocument()
  })

  it('shows label', () => {
    render(<CircularProgress value={50} label="Cards" />)
    expect(screen.getByText('Cards')).toBeInTheDocument()
  })

  it('caps at 100%', () => {
    render(<CircularProgress value={150} />)
    expect(screen.getByText('100%')).toBeInTheDocument()
  })
})

// ── ReviewRatingButtons ────────────────────────────────────────────────────
describe('ReviewRatingButtons', () => {
  it('renders all four rating buttons', () => {
    render(<ReviewRatingButtons onRate={vi.fn()} />)
    expect(screen.getByText('Again')).toBeInTheDocument()
    expect(screen.getByText('Hard')).toBeInTheDocument()
    expect(screen.getByText('Good')).toBeInTheDocument()
    expect(screen.getByText('Easy')).toBeInTheDocument()
  })

  it('calls onRate with quality 1 when Again is clicked', () => {
    const onRate = vi.fn()
    render(<ReviewRatingButtons onRate={onRate} />)
    fireEvent.click(screen.getByText('Again'))
    expect(onRate).toHaveBeenCalledWith(1)
  })

  it('calls onRate with quality 2 when Hard is clicked', () => {
    const onRate = vi.fn()
    render(<ReviewRatingButtons onRate={onRate} />)
    fireEvent.click(screen.getByText('Hard'))
    expect(onRate).toHaveBeenCalledWith(2)
  })

  it('calls onRate with quality 4 when Good is clicked', () => {
    const onRate = vi.fn()
    render(<ReviewRatingButtons onRate={onRate} />)
    fireEvent.click(screen.getByText('Good'))
    expect(onRate).toHaveBeenCalledWith(4)
  })

  it('calls onRate with quality 5 when Easy is clicked', () => {
    const onRate = vi.fn()
    render(<ReviewRatingButtons onRate={onRate} />)
    fireEvent.click(screen.getByText('Easy'))
    expect(onRate).toHaveBeenCalledWith(5)
  })

  it('buttons are disabled when disabled prop is true', () => {
    render(<ReviewRatingButtons onRate={vi.fn()} disabled />)
    const buttons = screen.getAllByRole('button')
    buttons.forEach(btn => expect(btn).toBeDisabled())
  })
})

// ── DeckCard ───────────────────────────────────────────────────────────────
const mockDeck: Deck = {
  id: 'deck-1',
  title: 'Cardiology Essentials',
  description: 'Heart failure, arrhythmias, and more',
  category: 'CARDIOLOGY',
  isPublic: false,
  cardCount: 24,
  dueCardCount: 5,
  avgEaseFactor: 2.5,
  createdAt: '2026-01-01',
  updatedAt: '2026-02-01',
}

describe('DeckCard', () => {
  it('renders deck title', () => {
    render(
      <MemoryRouter>
        <DeckCard deck={mockDeck} onDelete={vi.fn()} onEdit={vi.fn()} />
      </MemoryRouter>
    )
    expect(screen.getByText('Cardiology Essentials')).toBeInTheDocument()
  })

  it('renders card count', () => {
    render(
      <MemoryRouter>
        <DeckCard deck={mockDeck} onDelete={vi.fn()} onEdit={vi.fn()} />
      </MemoryRouter>
    )
    expect(screen.getByText(/24 cards/)).toBeInTheDocument()
  })

  it('renders due card count when non-zero', () => {
    render(
      <MemoryRouter>
        <DeckCard deck={mockDeck} onDelete={vi.fn()} onEdit={vi.fn()} />
      </MemoryRouter>
    )
    expect(screen.getByText(/5 due/)).toBeInTheDocument()
  })

  it('renders category badge', () => {
    render(
      <MemoryRouter>
        <DeckCard deck={mockDeck} onDelete={vi.fn()} onEdit={vi.fn()} />
      </MemoryRouter>
    )
    expect(screen.getByText('Cardiology')).toBeInTheDocument()
  })

  it('calls onEdit when Edit button is clicked', () => {
    const onEdit = vi.fn()
    render(
      <MemoryRouter>
        <DeckCard deck={mockDeck} onDelete={vi.fn()} onEdit={onEdit} />
      </MemoryRouter>
    )
    fireEvent.click(screen.getByText('Edit'))
    expect(onEdit).toHaveBeenCalledWith(mockDeck)
  })
})
