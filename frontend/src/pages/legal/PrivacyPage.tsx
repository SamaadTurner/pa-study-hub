import { Link } from 'react-router-dom'

// Where people can write about their data
const CONTACT_EMAIL = 'samaad.turner@gmail.com'

// Keep this page true to what the app does. When a feature changes what is
// stored or sent anywhere, change this page in the same commit.
export default function PrivacyPage() {
  return (
    <div className="min-h-screen bg-gray-50 py-10 px-4">
      <article className="max-w-2xl mx-auto bg-white border border-gray-200 rounded-2xl p-8 space-y-6 text-sm text-gray-700 leading-relaxed">
        <div>
          <Link to="/login" className="text-primary-600 hover:underline">← Back to sign in</Link>
          <h1 className="text-2xl font-bold text-gray-900 mt-3">Privacy</h1>
          <p className="text-xs text-gray-400 mt-1">Last updated November 19, 2026</p>
        </div>

        <p>
          PA Study Hub is a free study app built by one developer as a portfolio project. It has no
          ads, no analytics or tracking scripts, and nothing is sold or shared for marketing.
        </p>

        <section className="space-y-2">
          <h2 className="text-base font-semibold text-gray-900">What is stored</h2>
          <ul className="list-disc pl-5 space-y-1">
            <li>Your account: email, first and last name, and the optional school and graduation year.</li>
            <li>
              Your password, only as a one-way hash (BCrypt), never the password itself. An account made
              with Google has no password.
            </li>
            <li>
              If you use Google sign-in: Google&apos;s id for your account, your email and your name. The app
              asks Google for nothing else (no contacts, no files, no calendar).
            </li>
            <li>
              Your study data: decks, cards, every review (rating, time and date), practice exams and
              answers, questions you write, and daily activity for streaks and charts.
            </li>
            <li>
              In your browser: the sign-in tokens, in local storage, until you sign out.
            </li>
          </ul>
        </section>

        <section className="space-y-2">
          <h2 className="text-base font-semibold text-gray-900">Who else sees it</h2>
          <ul className="list-disc pl-5 space-y-1">
            <li>
              Decks you make public and questions you share can be seen and used by other users. Shared
              questions do not show who wrote them.
            </li>
            <li>
              The optional AI features send what you give them to Groq, an AI provider: the notes you paste
              for suggested cards, or the exam question and your answer when you ask for an explanation.
              Nothing is sent unless you press one of those buttons.
            </li>
            <li>
              The app runs on Render and the database on Neon, both in the US (Oregon).
            </li>
          </ul>
        </section>

        <section className="space-y-2">
          <h2 className="text-base font-semibold text-gray-900">Closing your account</h2>
          <p>
            Closing an account turns off sign-in, but your data stays in the database. There is no button
            for this in the app yet. To have your account closed or your data deleted completely, or for
            any other question about your data, email{' '}
            <a href={`mailto:${CONTACT_EMAIL}`} className="text-primary-600 hover:underline">
              {CONTACT_EMAIL}
            </a>.
          </p>
        </section>

        <section className="space-y-2">
          <h2 className="text-base font-semibold text-gray-900">Security</h2>
          <p>
            Connections are encrypted (HTTPS). Failed sign-ins are counted per email for 15 minutes to slow
            down password guessing. This is a student project without a security team, so please do not
            store anything here that you would not want to lose or have seen.
          </p>
        </section>
      </article>
    </div>
  )
}
