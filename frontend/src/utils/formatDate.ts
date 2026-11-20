import { format, formatDistanceToNow, isToday, isTomorrow, isYesterday, parseISO } from 'date-fns'

/**
 * Reads a date from the server. A time without a zone ("2026-11-20T18:43:23") is
 * the server's clock, which is UTC, so it is read as UTC and shown in local time.
 * A plain date ("2026-11-20") is a calendar day and stays as it is.
 */
export function parseServerDate(iso: string): Date {
  const hasTime = iso.includes('T')
  const hasZone = /(Z|[+-]\d{2}:?\d{2})$/.test(iso)
  return parseISO(hasTime && !hasZone ? `${iso}Z` : iso)
}

/**
 * Format an ISO date string as "Feb 9, 2026"
 */
export function formatDate(iso: string): string {
  return format(parseServerDate(iso), 'MMM d, yyyy')
}

/**
 * Format an ISO date string as "Feb 9"
 */
export function formatShortDate(iso: string): string {
  return format(parseServerDate(iso), 'MMM d')
}

/**
 * Format an ISO date string as "Feb 9, 2026 at 2:30 PM"
 */
export function formatDateTime(iso: string): string {
  return format(parseServerDate(iso), "MMM d, yyyy 'at' h:mm a")
}

/**
 * Returns a human-friendly relative label: "Today", "Yesterday", "Tomorrow",
 * or a formatted date for anything else.
 */
export function formatRelativeDate(iso: string): string {
  const date = parseServerDate(iso)
  if (isToday(date))     return 'Today'
  if (isTomorrow(date))  return 'Tomorrow'
  if (isYesterday(date)) return 'Yesterday'
  return format(date, 'MMM d, yyyy')
}

/**
 * Returns "2 hours ago", "3 days ago", etc.
 */
export function timeAgo(iso: string): string {
  return formatDistanceToNow(parseServerDate(iso), { addSuffix: true })
}

/**
 * Get greeting based on hour of day.
 */
export function getGreeting(): string {
  const hour = new Date().getHours()
  if (hour < 12) return 'Good morning'
  if (hour < 17) return 'Good afternoon'
  return 'Good evening'
}
