import type { BadgeVariant } from '@/components/common/Badge'
import { CATEGORY_BADGE_VARIANT } from '@/components/common/Badge'

// Re-export the badge variant mapping from Badge component for use in other files
export { CATEGORY_BADGE_VARIANT }

// Hex colors for Recharts — needs actual color values, not Tailwind classes
export const CATEGORY_HEX_COLORS: Record<string, string> = {
  CARDIOLOGY:          '#ef4444',  // red-500
  PULMONOLOGY:         '#3b82f6',  // blue-500
  GASTROENTEROLOGY:    '#f97316',  // orange-500
  MUSCULOSKELETAL:     '#eab308',  // yellow-500
  NEUROLOGY:           '#a855f7',  // purple-500
  PSYCHIATRY:          '#ec4899',  // pink-500
  DERMATOLOGY:         '#14b8a6',  // teal-500
  EENT:                '#6366f1',  // indigo-500
  ENDOCRINOLOGY:       '#22c55e',  // green-500
  HEMATOLOGY:          '#dc2626',  // red-600
  INFECTIOUS_DISEASE:  '#ea580c',  // orange-600
  NEPHROLOGY:          '#2563eb',  // blue-600
  REPRODUCTIVE:        '#db2777',  // pink-600
  PEDIATRICS:          '#16a34a',  // green-600
  EMERGENCY_MEDICINE:  '#b91c1c',  // red-700
  PHARMACOLOGY:        '#4f46e5',  // indigo-600
  ANATOMY:             '#6b7280',  // gray-500
}

// Human-readable display names for categories
export const CATEGORY_LABELS: Record<string, string> = {
  CARDIOLOGY:          'Cardiology',
  PULMONOLOGY:         'Pulmonology',
  GASTROENTEROLOGY:    'Gastroenterology',
  MUSCULOSKELETAL:     'Musculoskeletal',
  NEUROLOGY:           'Neurology',
  PSYCHIATRY:          'Psychiatry',
  DERMATOLOGY:         'Dermatology',
  EENT:                'EENT',
  ENDOCRINOLOGY:       'Endocrinology',
  HEMATOLOGY:          'Hematology',
  INFECTIOUS_DISEASE:  'Infectious Disease',
  NEPHROLOGY:          'Nephrology',
  REPRODUCTIVE:        'Reproductive',
  PEDIATRICS:          'Pediatrics',
  EMERGENCY_MEDICINE:  'Emergency Medicine',
  PHARMACOLOGY:        'Pharmacology',
  ANATOMY:             'Anatomy',
}

export function getCategoryColor(category: string): string {
  return CATEGORY_HEX_COLORS[category] ?? '#6b7280'
}

export function getCategoryLabel(category: string): string {
  return CATEGORY_LABELS[category] ?? category
}

export function getCategoryBadgeVariant(category: string): BadgeVariant {
  return CATEGORY_BADGE_VARIANT[category] ?? 'gray'
}

export const ALL_CATEGORIES = Object.keys(CATEGORY_LABELS)
