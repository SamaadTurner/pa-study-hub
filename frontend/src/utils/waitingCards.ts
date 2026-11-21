// Short cards shown while the server wakes up. A wait feels shorter when there is
// something to look at, and for PA students a quick review question is a good use
// of a minute. Only well-established facts; keep each one short enough to read in
// a few seconds.

export type WaitingCard =
  | { kind: 'fact'; text: string }
  | { kind: 'review'; question: string; answer: string }

export const WAITING_CARDS: WaitingCard[] = [
  { kind: 'fact', text: 'The PA profession began at Duke University in 1965, when Dr. Eugene Stead Jr. enrolled four former Navy hospital corpsmen.' },
  { kind: 'fact', text: "National PA Week runs October 6 to 12. October 6 is the day Duke's first PA class graduated, in 1967." },
  { kind: 'fact', text: 'The PANCE has 300 multiple-choice questions in five blocks of 60, with 60 minutes for each block.' },
  { kind: 'fact', text: 'Certified PAs log 100 hours of CME every two years and recertify every ten years.' },
  { kind: 'fact', text: "In 2021, AAPA voted to change the profession's title from \"physician assistant\" to \"physician associate\"." },
  { kind: 'fact', text: 'Recalling beats rereading: in a 2006 study by Roediger and Karpicke, students who practiced recall remembered more a week later than students who studied again.' },
  { kind: 'fact', text: 'Hermann Ebbinghaus charted the forgetting curve in 1885 by memorizing lists of nonsense syllables himself.' },
  { kind: 'fact', text: 'Spreading reviews out, the way this app schedules your cards, beats cramming the same study time into one session.' },
  { kind: 'review', question: "Beck's triad points to which emergency?", answer: 'Cardiac tamponade: low blood pressure, distended neck veins, muffled heart sounds.' },
  { kind: 'review', question: "What are the three parts of Virchow's triad?", answer: 'Venous stasis, endothelial injury and hypercoagulability.' },
  { kind: 'review', question: "Cushing's triad is a sign of what?", answer: 'Raised intracranial pressure: hypertension with a widened pulse pressure, bradycardia, irregular breathing.' },
  { kind: 'review', question: "Charcot's triad suggests which diagnosis?", answer: 'Ascending cholangitis: fever, right upper quadrant pain and jaundice.' },
  { kind: 'review', question: 'What does MUDPILES help you remember?', answer: 'Causes of an anion gap metabolic acidosis: methanol, uremia, DKA, propylene glycol, isoniazid or iron, lactic acidosis, ethylene glycol, salicylates.' },
  { kind: 'review', question: 'First-line drug for anaphylaxis?', answer: 'Intramuscular epinephrine.' },
]

// Where to start, so the same card doesn't greet everyone on every wake-up
export function randomStart(count = WAITING_CARDS.length): number {
  return Math.floor(Math.random() * count)
}

// How full the progress bar is after some seconds, given the usual wake-up time.
// It never reaches 100% on its own (that would promise something), it stops at 95%.
export function wakeProgress(seconds: number, usualSeconds = 95): number {
  return Math.min(95, Math.round((seconds / usualSeconds) * 100))
}
