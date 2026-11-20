// Fills a local backend with data for the load test: one user with 100 decks of
// 20 cards each, and the first card of every deck studied once.
//
//   node perf/seed.mjs                      (backend at http://localhost:8080)
//   BASE_URL=http://localhost:8080 node perf/seed.mjs
//
// Only for a local database. The user is perf@example.com with the password below;
// running it again adds nothing if the user already has its decks.
const BASE_URL = process.env.BASE_URL ?? 'http://localhost:8080'
const API = `${BASE_URL}/api/v1`
export const EMAIL = 'perf@example.com'
export const PASSWORD = 'Perf!Test123'
const DECKS = 100
const CARDS_PER_DECK = 20

async function call(method, path, body, token) {
  const res = await fetch(API + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  })
  const text = await res.text()
  return { status: res.status, json: text ? JSON.parse(text) : null }
}

let auth = await call('POST', '/auth/register', { email: EMAIL, password: PASSWORD, firstName: 'Perf', lastName: 'Test' })
if (auth.status === 409) {
  auth = await call('POST', '/auth/login', { email: EMAIL, password: PASSWORD })
}
if (!auth.json?.accessToken) throw new Error(`Could not sign in: ${auth.status}`)
const token = auth.json.accessToken

const existing = await call('GET', '/decks?size=1', null, token)
if (existing.json.totalElements >= DECKS) {
  console.log(`${EMAIL} already has ${existing.json.totalElements} decks, nothing to do`)
  process.exit(0)
}

for (let d = 1; d <= DECKS; d++) {
  const deck = await call('POST', '/decks', { title: `Load test deck ${d}`, description: '', category: 'CARDIOLOGY', isPublic: false }, token)
  let firstCardId = null
  for (let c = 1; c <= CARDS_PER_DECK; c++) {
    const card = await call('POST', `/decks/${deck.json.id}/cards`, { front: `Question ${d}.${c}`, back: `Answer ${d}.${c}`, tags: [] }, token)
    firstCardId ??= card.json.id
  }
  await call('POST', `/cards/${firstCardId}/review`, { quality: 4, timeSpentSeconds: 5 }, token)
  if (d % 10 === 0) console.log(`${d} decks`)
}
console.log(`Done: ${DECKS} decks of ${CARDS_PER_DECK} cards for ${EMAIL}`)
