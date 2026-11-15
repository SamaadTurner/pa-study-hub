// The hosted demo runs on a free plan that puts the backend to sleep after 15
// minutes without visitors. The first request after that gets no answer (or a
// gateway error) while the server starts, which takes about a minute. Without
// this the user only saw "An unexpected error occurred."
export const SERVER_UNREACHABLE_MESSAGE =
  'The server is not answering. If nobody has used the app for a while it is starting up, ' +
  'which takes about a minute. Please try again shortly.'

// status is the HTTP status of the response, or undefined when there was no response
// at all (network error or timeout)
export function isServerUnreachable(status: number | undefined): boolean {
  if (status === undefined) {
    return true
  }
  // Bad gateway, service unavailable, gateway timeout: the request reached the
  // host's proxy but not our backend
  return status === 502 || status === 503 || status === 504
}
