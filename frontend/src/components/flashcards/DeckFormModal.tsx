import { useState, useEffect, FormEvent } from 'react'
import Modal from '@/components/common/Modal'
import Button from '@/components/common/Button'
import type { Deck } from '@/types'
import { ALL_CATEGORIES, getCategoryLabel } from '@/utils/categoryColors'

interface FormState {
  title: string
  description: string
  category: string
  isPublic: boolean
}

interface Props {
  isOpen: boolean
  onClose: () => void
  onSubmit: (data: FormState) => Promise<void>
  editDeck?: Deck | null
}

const DEFAULT_FORM: FormState = {
  title: '',
  description: '',
  category: 'CARDIOLOGY',
  isPublic: false,
}

export default function DeckFormModal({ isOpen, onClose, onSubmit, editDeck }: Props) {
  const [form, setForm]       = useState<FormState>(DEFAULT_FORM)
  const [errors, setErrors]   = useState<Partial<FormState>>({})
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (editDeck) {
      setForm({
        title: editDeck.title,
        description: editDeck.description ?? '',
        category: editDeck.category,
        isPublic: editDeck.isPublic,
      })
    } else {
      setForm(DEFAULT_FORM)
    }
    setErrors({})
  }, [editDeck, isOpen])

  function validate(): boolean {
    const next: Partial<FormState> = {}
    if (!form.title.trim()) next.title = 'Title is required'
    else if (form.title.trim().length > 100) next.title = 'Title must be 100 characters or less'
    if (form.description.length > 500) next.description = 'Description must be 500 characters or less'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!validate()) return
    setLoading(true)
    try {
      await onSubmit({ ...form, title: form.title.trim(), description: form.description.trim() })
      onClose()
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={editDeck ? 'Edit Deck' : 'Create New Deck'}
      actions={
        <>
          <Button variant="secondary" onClick={onClose} disabled={loading}>Cancel</Button>
          <Button onClick={handleSubmit} loading={loading}>
            {editDeck ? 'Save Changes' : 'Create Deck'}
          </Button>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Title <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={form.title}
            onChange={e => setForm(f => ({ ...f, title: e.target.value }))}
            className={`w-full px-3 py-2 border rounded-lg text-sm focus:outline-none
              focus:ring-2 focus:ring-primary-500
              ${errors.title ? 'border-red-400 bg-red-50' : 'border-gray-300'}`}
            placeholder="e.g. Cardiology — Heart Failure"
            maxLength={100}
          />
          {errors.title && <p className="mt-1 text-xs text-red-600">{errors.title}</p>}
          <p className="mt-1 text-xs text-gray-400">{form.title.length}/100</p>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
          <textarea
            value={form.description}
            onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
            rows={2}
            className={`w-full px-3 py-2 border rounded-lg text-sm focus:outline-none
              focus:ring-2 focus:ring-primary-500 resize-none
              ${errors.description ? 'border-red-400 bg-red-50' : 'border-gray-300'}`}
            placeholder="Optional description..."
            maxLength={500}
          />
          {errors.description && <p className="mt-1 text-xs text-red-600">{errors.description}</p>}
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Category <span className="text-red-500">*</span>
          </label>
          <select
            value={form.category}
            onChange={e => setForm(f => ({ ...f, category: e.target.value }))}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm
              focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
          >
            {ALL_CATEGORIES.map(cat => (
              <option key={cat} value={cat}>{getCategoryLabel(cat)}</option>
            ))}
          </select>
        </div>

        <div className="flex items-center gap-2">
          <input
            id="isPublic"
            type="checkbox"
            checked={form.isPublic}
            onChange={e => setForm(f => ({ ...f, isPublic: e.target.checked }))}
            className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
          />
          <label htmlFor="isPublic" className="text-sm text-gray-700">
            Make this deck public (other students can clone it)
          </label>
        </div>
      </form>
    </Modal>
  )
}
