// k6 load test of the pages a student opens most, against a LOCAL backend that
// perf/seed.mjs has filled. Never point it at the live site: the free server is
// small and shared, and this sends hundreds of requests a second.
//
//   k6 run perf/main-pages.js
//   k6 run -e BASE_URL=http://localhost:8080 perf/main-pages.js
//
// Each virtual user does what the app does on the way to studying: the deck list
// (all 100 decks, like the My Decks page), the dashboard, one deck, its cards and
// its study queue.
import http from 'k6/http'
import { check } from 'k6'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'
const API = `${BASE_URL}/api/v1`

export const options = {
  scenarios: {
    students: { executor: 'constant-vus', vus: 20, duration: '30s' },
  },
  // One tag per page, so the summary shows each page's times on its own line
  thresholds: {
    'http_req_duration{page:decks}': ['p(95)>=0'],
    'http_req_duration{page:dashboard}': ['p(95)>=0'],
    'http_req_duration{page:deck}': ['p(95)>=0'],
    'http_req_duration{page:cards}': ['p(95)>=0'],
    'http_req_duration{page:review}': ['p(95)>=0'],
  },
  summaryTrendStats: ['avg', 'med', 'p(95)', 'max'],
}

// Signs in once; every virtual user shares the token and the deck ids
export function setup() {
  const login = http.post(`${API}/auth/login`,
    JSON.stringify({ email: 'perf@example.com', password: 'Perf!Test123' }),
    { headers: { 'Content-Type': 'application/json' } })
  const token = login.json('accessToken')
  const decks = http.get(`${API}/decks?size=100`, { headers: { Authorization: `Bearer ${token}` } })
  return { token, deckIds: decks.json('content').map(deck => deck.id) }
}

export default function (data) {
  const params = (page) => ({ headers: { Authorization: `Bearer ${data.token}` }, tags: { page } })
  const deckId = data.deckIds[Math.floor(Math.random() * data.deckIds.length)]

  const decks = http.get(`${API}/decks?size=100`, params('decks'))
  check(decks, { 'deck list 200': r => r.status === 200 })
  check(http.get(`${API}/progress/dashboard`, params('dashboard')), { 'dashboard 200': r => r.status === 200 })
  check(http.get(`${API}/decks/${deckId}`, params('deck')), { 'deck 200': r => r.status === 200 })
  check(http.get(`${API}/decks/${deckId}/cards?size=100`, params('cards')), { 'cards 200': r => r.status === 200 })
  check(http.get(`${API}/decks/${deckId}/review`, params('review')), { 'review 200': r => r.status === 200 })
}
