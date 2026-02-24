import { useState, useEffect, useRef, useCallback } from 'react'
import { Link } from 'react-router-dom'
import { sendChatMessage } from '@/api/ai'
import type { ChatMessage } from '@/types'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import { useNotification } from '@/contexts/NotificationContext'
import type { ApiError } from '@/types'

const SUGGESTION_PROMPTS = [
  'Explain the pathophysiology of heart failure',
  'What are the key differences between Type 1 and Type 2 diabetes?',
  'How do I approach a patient with chest pain?',
  'What are the common causes of acute kidney injury?',
  'Describe the ABCDE approach to trauma assessment',
]

interface DisplayMessage extends ChatMessage {
  id: string
}

export default function AiChatPage() {
  const { notify } = useNotification()
  const [messages, setMessages]     = useState<DisplayMessage[]>([])
  const [input, setInput]           = useState('')
  const [sending, setSending]       = useState(false)
  const [sessionId, setSessionId]   = useState<string | undefined>(undefined)
  const bottomRef                   = useRef<HTMLDivElement>(null)
  const textareaRef                 = useRef<HTMLTextAreaElement>(null)

  // Scroll to bottom whenever messages update
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, sending])

  const handleSend = useCallback(async (text: string) => {
    const trimmed = text.trim()
    if (!trimmed || sending) return

    const userMsg: DisplayMessage = {
      id: `u-${Date.now()}`,
      role: 'user',
      content: trimmed,
    }
    setMessages(prev => [...prev, userMsg])
    setInput('')
    setSending(true)

    try {
      const resp = await sendChatMessage({ message: trimmed, sessionId })
      setSessionId(resp.sessionId)
      const assistantMsg: DisplayMessage = {
        id: `a-${resp.turnNumber}-${Date.now()}`,
        role: 'assistant',
        content: resp.assistantReply,
      }
      setMessages(prev => [...prev, assistantMsg])
    } catch (err) {
      notify('error', (err as ApiError)?.detail ?? 'Failed to send message')
      // Remove the optimistic user message on error
      setMessages(prev => prev.filter(m => m.id !== userMsg.id))
      setInput(trimmed)
    } finally {
      setSending(false)
      // Refocus the textarea
      setTimeout(() => textareaRef.current?.focus(), 50)
    }
  }, [sending, sessionId, notify])

  function handleKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend(input)
    }
  }

  function handleNewChat() {
    setMessages([])
    setSessionId(undefined)
    setInput('')
    setTimeout(() => textareaRef.current?.focus(), 50)
  }

  return (
    <div className="max-w-2xl mx-auto flex flex-col h-[calc(100vh-8rem)]">

      {/* Header */}
      <div className="pb-4 border-b border-gray-200 flex-shrink-0 space-y-3">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-xl font-bold text-gray-900">AI Tutor</h1>
            <p className="text-xs text-gray-400 mt-0.5">
              Powered by Claude · Ask anything about PA medicine
            </p>
          </div>
          {messages.length > 0 && (
            <button
              onClick={handleNewChat}
              className="text-xs text-gray-400 hover:text-gray-600 transition-colors px-3 py-1.5
                border border-gray-200 rounded-lg hover:border-gray-300"
            >
              New chat
            </button>
          )}
        </div>

        {/* AI Tools Navigation */}
        <div className="flex gap-2">
          <Link
            to="/ai"
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium
              bg-primary-50 text-primary-700 border border-primary-200"
          >
            💬 Chat
          </Link>
          <Link
            to="/ai/generate"
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium
              text-gray-600 hover:bg-gray-100 border border-gray-200"
          >
            🃏 Generate Flashcards
          </Link>
          <Link
            to="/ai/study-plan"
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium
              text-gray-600 hover:bg-gray-100 border border-gray-200"
          >
            📅 Study Plan
          </Link>
        </div>
      </div>

      {/* Message list */}
      <div className="flex-1 overflow-y-auto py-4 space-y-4 min-h-0">
        {messages.length === 0 ? (
          <div className="space-y-6 pt-4">
            <div className="text-center">
              <p className="text-4xl mb-2">🤖</p>
              <p className="text-sm font-medium text-gray-700">
                Your AI study partner is ready
              </p>
              <p className="text-xs text-gray-400 mt-1">
                Ask about any PA medicine topic, explain concepts, or quiz yourself
              </p>
            </div>
            <div className="space-y-2">
              <p className="text-xs font-medium text-gray-500 text-center">Try asking...</p>
              {SUGGESTION_PROMPTS.map((prompt, i) => (
                <button
                  key={i}
                  onClick={() => handleSend(prompt)}
                  disabled={sending}
                  className="w-full text-left px-4 py-3 rounded-xl border border-gray-200
                    text-sm text-gray-700 hover:border-primary-300 hover:bg-primary-50/40
                    transition-colors disabled:opacity-50"
                >
                  {prompt}
                </button>
              ))}
            </div>
          </div>
        ) : (
          messages.map(msg => (
            <div
              key={msg.id}
              className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}
            >
              <div
                className={`max-w-[85%] rounded-2xl px-4 py-3 text-sm leading-relaxed
                  ${msg.role === 'user'
                    ? 'bg-primary-600 text-white rounded-br-sm'
                    : 'bg-white border border-gray-200 text-gray-800 rounded-bl-sm shadow-sm'
                  }`}
              >
                {msg.role === 'assistant' ? (
                  <AssistantMessage content={msg.content} />
                ) : (
                  msg.content
                )}
              </div>
            </div>
          ))
        )}

        {/* Typing indicator */}
        {sending && (
          <div className="flex justify-start">
            <div className="bg-white border border-gray-200 rounded-2xl rounded-bl-sm
              px-4 py-3 shadow-sm">
              <LoadingSpinner size="sm" />
            </div>
          </div>
        )}

        <div ref={bottomRef} />
      </div>

      {/* Input area */}
      <div className="flex-shrink-0 pt-3 border-t border-gray-200">
        <div className="flex items-end gap-3 bg-white border border-gray-200 rounded-xl
          px-4 py-3 focus-within:border-primary-400 focus-within:ring-1
          focus-within:ring-primary-400 transition-all">
          <textarea
            ref={textareaRef}
            rows={1}
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ask anything about PA medicine..."
            disabled={sending}
            className="flex-1 resize-none text-sm text-gray-900 placeholder-gray-400
              focus:outline-none bg-transparent max-h-32 overflow-y-auto
              disabled:opacity-60"
            style={{ fieldSizing: 'content' } as React.CSSProperties}
          />
          <button
            onClick={() => handleSend(input)}
            disabled={!input.trim() || sending}
            className="flex-shrink-0 w-8 h-8 rounded-lg bg-primary-600 text-white
              flex items-center justify-center hover:bg-primary-700 transition-colors
              disabled:opacity-40 disabled:cursor-not-allowed"
            aria-label="Send message"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
            </svg>
          </button>
        </div>
        <p className="text-xs text-gray-400 text-center mt-2">
          Press Enter to send · Shift+Enter for new line
        </p>
      </div>
    </div>
  )
}

// ── Simple markdown-lite renderer for assistant messages ───────────────────
function AssistantMessage({ content }: { content: string }) {
  // Split on double-newlines to get paragraphs/blocks
  const blocks = content.split(/\n\n+/)
  return (
    <div className="space-y-2">
      {blocks.map((block, i) => {
        // Numbered list
        if (/^\d+\.\s/.test(block)) {
          const items = block.split('\n').filter(Boolean)
          return (
            <ol key={i} className="list-decimal list-inside space-y-1">
              {items.map((item, j) => (
                <li key={j}>{item.replace(/^\d+\.\s*/, '')}</li>
              ))}
            </ol>
          )
        }
        // Bullet list
        if (/^[-•]\s/.test(block)) {
          const items = block.split('\n').filter(Boolean)
          return (
            <ul key={i} className="list-disc list-inside space-y-1">
              {items.map((item, j) => (
                <li key={j}>{item.replace(/^[-•]\s*/, '')}</li>
              ))}
            </ul>
          )
        }
        // Heading (### or **)
        if (block.startsWith('###')) {
          return (
            <p key={i} className="font-semibold text-gray-900">
              {block.replace(/^###\s*/, '')}
            </p>
          )
        }
        // Plain paragraph
        return <p key={i}>{block}</p>
      })}
    </div>
  )
}
