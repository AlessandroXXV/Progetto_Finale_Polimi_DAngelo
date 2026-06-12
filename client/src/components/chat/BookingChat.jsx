import { useState, useRef, useEffect, useCallback } from 'react'
import { MessageCircle } from 'lucide-react'
import useBookingChat from '../../hooks/useBookingChat'
import useAuthStore from '../../store/authStore'
import { bookingService } from '../../services/bookingService'
import UserAvatar from '../common/UserAvatar'

// Number of messages fetched per page (both initial load and "load older" requests)
const PAGE_SIZE = 30

// Real-time chat panel scoped to a single booking.
// Combines REST-based history loading with WebSocket delivery via useBookingChat.
// Deduplication is required because the sender receives their own message both
// through the WebSocket push and the initial REST history fetch.
export default function BookingChat({ bookingId }) {
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(true)
  const [hasMore, setHasMore] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)
  // Invisible sentinel element at the bottom of the list; used to auto-scroll on new messages
  const bottomRef = useRef()
  // Tracks message IDs already in state to prevent duplicates from WebSocket + REST overlap
  const seenIds = useRef(new Set())
  const { user } = useAuthStore()

  // Load persisted history on mount
  useEffect(() => {
    if (!bookingId) return
    setLoading(true)
    bookingService.getChatMessages(bookingId, { page: 0, size: PAGE_SIZE })
      .then(({ data }) => {
        const validMessages = data.filter((m) => m.id != null)
        seenIds.current = new Set(validMessages.map((m) => m.id))
        setMessages(validMessages)
        // If a full page was returned, there are likely older messages to fetch
        setHasMore(data.length === PAGE_SIZE)
      })
      .catch(() => { /* history unavailable, start empty */ })
      .finally(() => setLoading(false))
  }, [bookingId])

  // Deduplicate: WebSocket may deliver a message the sender already has via REST
  const handleMessage = useCallback((msg) => {
    if (!msg?.id) return
    if (seenIds.current.has(msg.id)) return
    seenIds.current.add(msg.id)
    setMessages((prev) => [...prev, msg])
  }, [])

  // Fetches the page of messages older than the current oldest, prepending them to the list
  const handleLoadOlder = async () => {
    if (!hasMore || loadingMore) return
    const oldestId = messages[0]?.id
    if (!oldestId) return

    setLoadingMore(true)
    try {
      const { data } = await bookingService.getChatMessages(bookingId, { beforeId: oldestId, limit: PAGE_SIZE })
      const older = data.filter((m) => m.id != null && !seenIds.current.has(m.id))
      older.forEach((m) => seenIds.current.add(m.id))
      setMessages((prev) => [...older, ...prev])
      setHasMore(data.length === PAGE_SIZE)
    } catch {
      // keep current history if older page loading fails
    } finally {
      setLoadingMore(false)
    }
  }

  // Subscribe to WebSocket for the booking room; sendMessage publishes to the topic
  const { sendMessage } = useBookingChat(bookingId, handleMessage)

  // Scroll to the bottom whenever the message list grows
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const handleSend = (e) => {
    e.preventDefault()
    if (!input.trim()) return
    sendMessage(input.trim())
    setInput('')
  }

  return (
    <div className="flex h-105 min-h-90 flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm sm:h-115">
      <div className="border-b border-slate-200 bg-slate-50 px-4 py-3">
        <h3 className="flex items-center gap-2 text-sm font-semibold text-slate-700">
          <MessageCircle className="h-4 w-4" strokeWidth={1.5} />
          Chat prenotazione
        </h3>
      </div>

      <div className="flex-1 space-y-3 overflow-y-auto p-4">
        {/* "Load older" button is shown only after the initial load and when more pages exist */}
        {!loading && hasMore && (
          <div className="text-center">
            <button
              type="button"
              onClick={handleLoadOlder}
              disabled={loadingMore}
              className="text-xs font-medium text-indigo-600 hover:underline disabled:opacity-60"
            >
              {loadingMore ? 'Caricamento...' : 'Carica messaggi precedenti'}
            </button>
          </div>
        )}
        {loading && (
          <p className="py-8 text-center text-sm text-slate-400">Caricamento messaggi…</p>
        )}
        {!loading && messages.length === 0 && (
          <p className="py-8 text-center text-sm text-slate-400">
            Nessun messaggio. Inizia la conversazione!
          </p>
        )}
        {messages.map((m) => {
          // Align own messages to the right, other participants' messages to the left
          const isMe = m.senderId === user?.userId
          return (
            <div key={m.id} className={`flex ${isMe ? 'justify-end' : 'justify-start'}`}>
              <div className={`flex items-end gap-2 ${isMe ? 'flex-row-reverse' : ''}`}>
                <UserAvatar
                  userId={m.senderId}
                  username={m.senderUsername}
                  sizeClassName="w-8 h-8"
                  textClassName="text-xs"
                />
                <div
                  className={`max-w-[80%] rounded-2xl px-4 py-2.5 text-sm shadow-sm ${
                    isMe
                      ? 'rounded-br-none bg-indigo-600 text-white'
                      : 'rounded-bl-none border border-slate-200 bg-white text-slate-800'
                  }`}
                >
                  {/* Show username label only for incoming messages */}
                  {!isMe && (
                    <p className="mb-1 text-xs font-semibold text-indigo-600">{m.senderUsername}</p>
                  )}
                  <p className="leading-6">{m.content}</p>
                  <p className={`mt-1 text-xs ${isMe ? 'text-indigo-200' : 'text-slate-400'}`}>
                    {new Date(m.timestamp).toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' })}
                  </p>
                </div>
              </div>
            </div>
          )
        })}
        <div ref={bottomRef} />
      </div>

      <form onSubmit={handleSend} className="flex gap-2 border-t border-slate-200 bg-slate-50 p-3">
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Scrivi un messaggio…"
          className="field flex-1 bg-white py-2.5"
        />
        <button
          type="submit"
          className="primary-button min-w-21 px-4 py-2"
        >
          Invia
        </button>
      </form>
    </div>
  )
}
