export type BadgeVariant = 'blue' | 'green' | 'yellow' | 'red' | 'purple' | 'orange' | 'gray' | 'indigo' | 'pink' | 'teal'

interface Props {
  children: React.ReactNode
  variant?: BadgeVariant
  className?: string
}

const VARIANT_CLASSES: Record<BadgeVariant, string> = {
  blue:   'bg-blue-100 text-blue-800',
  green:  'bg-green-100 text-green-800',
  yellow: 'bg-yellow-100 text-yellow-800',
  red:    'bg-red-100 text-red-800',
  purple: 'bg-purple-100 text-purple-800',
  orange: 'bg-orange-100 text-orange-800',
  gray:   'bg-gray-100 text-gray-700',
  indigo: 'bg-indigo-100 text-indigo-800',
  pink:   'bg-pink-100 text-pink-800',
  teal:   'bg-teal-100 text-teal-800',
}

// Map NCCPA medical categories to badge colors
export const CATEGORY_BADGE_VARIANT: Record<string, BadgeVariant> = {
  CARDIOLOGY:          'red',
  PULMONOLOGY:         'blue',
  GASTROENTEROLOGY:    'orange',
  MUSCULOSKELETAL:     'yellow',
  NEUROLOGY:           'purple',
  PSYCHIATRY:          'pink',
  DERMATOLOGY:         'teal',
  EENT:                'indigo',
  ENDOCRINOLOGY:       'green',
  HEMATOLOGY:          'red',
  INFECTIOUS_DISEASE:  'orange',
  NEPHROLOGY:          'blue',
  REPRODUCTIVE:        'pink',
  PEDIATRICS:          'green',
  EMERGENCY_MEDICINE:  'red',
  PHARMACOLOGY:        'indigo',
  ANATOMY:             'gray',
}

export default function Badge({ children, variant = 'gray', className = '' }: Props) {
  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium
        ${VARIANT_CLASSES[variant]} ${className}`}
    >
      {children}
    </span>
  )
}
