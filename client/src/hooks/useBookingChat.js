// Hook that establishes a STOMP-over-SockJS WebSocket connection for a specific booking's chat.
// Reconnects automatically whenever bookingId or the auth token changes.
import { useEffect, useRef } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import useAuthStore from '../store/authStore'

export default function useBookingChat(bookingId, onMessage) {
  const clientRef = useRef(null)
  // Store the latest onMessage callback in a ref so the STOMP subscription closure
  // always calls the current version without needing to reconnect on every render.
  const onMessageRef = useRef(onMessage)
  const { token } = useAuthStore()

  useEffect(() => {
    onMessageRef.current = onMessage
  }, [onMessage])

  useEffect(() => {
    if (!bookingId || !token) return

    const stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws-eably'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        // Subscribe to the booking-specific topic; parse each incoming frame as JSON.
        stompClient.subscribe(`/topic/booking/${bookingId}`, (frame) => {
          try {
            const msg = JSON.parse(frame.body)
            onMessageRef.current?.(msg)
          } catch { /* ignore malformed frames */ }
        })
      },
    })

    stompClient.activate()
    clientRef.current = stompClient

    // Cleanly disconnect when the booking changes or the component unmounts.
    return () => {
      stompClient.deactivate()
    }
  }, [bookingId, token])

  // Publish a chat message to the server-side message broker endpoint.
  const sendMessage = (content) => {
    if (clientRef.current?.connected) {
      clientRef.current.publish({
        destination: `/app/chat/${bookingId}`,
        body: JSON.stringify({ content }),
      })
    }
  }

  return { sendMessage }
}
