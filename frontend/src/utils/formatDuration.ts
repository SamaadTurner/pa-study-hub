/**
 * Format seconds into "2m 5s", "1h 0m", "45s", etc.
 */
export function formatDuration(totalSeconds: number): string {
  if (totalSeconds < 0) return '0s'

  const hours   = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = Math.floor(totalSeconds % 60)

  if (hours > 0) {
    return `${hours}h ${minutes}m`
  }
  if (minutes > 0) {
    return `${minutes}m ${seconds}s`
  }
  return `${seconds}s`
}

/**
 * Format minutes into a human-readable string: "30 min", "1 hr 15 min"
 */
export function formatMinutes(totalMinutes: number): string {
  if (totalMinutes < 0) return '0 min'

  const hours   = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60

  if (hours > 0 && minutes > 0) return `${hours} hr ${minutes} min`
  if (hours > 0)                 return `${hours} hr`
  return `${minutes} min`
}

/**
 * Format a countdown (in seconds) as MM:SS
 */
export function formatCountdown(totalSeconds: number): string {
  const minutes = Math.floor(Math.max(0, totalSeconds) / 60)
  const seconds = Math.floor(Math.max(0, totalSeconds) % 60)
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}
