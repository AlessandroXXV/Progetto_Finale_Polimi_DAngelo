package it.eably.backend.config;

import it.eably.backend.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * WebSocket configuration for real-time chat functionality.
 * 
 * TECHNOLOGY: Spring WebSocket with STOMP protocol
 * 
 * ARCHITECTURE:
 * - STOMP (Simple Text Oriented Messaging Protocol) over WebSocket
 * - SockJS fallback for browsers without WebSocket support
 * - Message broker for pub/sub messaging pattern
 * 
 * MESSAGE DESTINATIONS:
 * - /app/* : Application destination prefix (messages sent TO server)
 * - /topic/* : Broadcast messages (one-to-many)
 * - /queue/* : Private messages (one-to-one)
 * 
 * USE CASE:
 * - Real-time chat between client and provider for booking discussions
 * - Each booking has its own chat room: /topic/booking/{bookingId}
 * - Messages sent to /app/chat/{bookingId} are broadcast to /topic/booking/{bookingId}
 * 
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    
    @Autowired
    public WebSocketConfig(JwtTokenProvider jwtTokenProvider, UserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    List<String> authorization = accessor.getNativeHeader("Authorization");
                    if (authorization != null && !authorization.isEmpty()) {
                        String bearerToken = authorization.get(0);
                        if (bearerToken.startsWith("Bearer ")) {
                            String token = bearerToken.substring(7);
                            if (jwtTokenProvider.validateToken(token)) {
                                String username = jwtTokenProvider.getUsernameFromToken(token);
                                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                                accessor.setUser(auth);
                            }
                        }
                    }
                }
                return message;
            }
        });
    }

    /**
     * Configures the message broker.
     * 
     * BROKER CONFIGURATION:
     * - Simple broker: In-memory broker for development
     * - /topic: For broadcast messages (multiple subscribers)
     * - /queue: For private messages (single subscriber)
     * 
     * APPLICATION DESTINATION PREFIX:
     * - /app: Prefix for messages sent TO the server
     * - Example: Client sends to /app/chat/123, server receives at @MessageMapping("/chat/123")
     * 
     * PRODUCTION NOTE:
     * - For production, replace simple broker with external broker (RabbitMQ, ActiveMQ)
     * - Use .enableStompBrokerRelay() instead of .enableSimpleBroker()
     * 
     * @param registry message broker registry
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple in-memory broker for /topic and /queue destinations
        registry.enableSimpleBroker("/topic", "/queue");
        
        // Set application destination prefix for messages sent TO server
        registry.setApplicationDestinationPrefixes("/app");
        
        // Optional: Set user destination prefix for private messages
        registry.setUserDestinationPrefix("/user");
    }
    
    /**
     * Registers STOMP endpoints.
     * 
     * ENDPOINT CONFIGURATION:
     * - /ws-eably: WebSocket endpoint for client connections
     * - SockJS: Fallback for browsers without WebSocket support
     * - CORS: Allow all origins (configure properly for production)
     * 
     * CLIENT CONNECTION:
     * - JavaScript: new SockJS('http://localhost:8080/ws-eably')
     * - Then wrap with STOMP client: Stomp.over(socket)
     * 
     * PRODUCTION NOTE:
     * - Replace setAllowedOrigins("*") with specific frontend domains
     * - Example: setAllowedOrigins("https://eably.com", "https://app.eably.com")
     * 
     * @param registry STOMP endpoint registry
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-eably")
                .setAllowedOriginPatterns("*") // TODO: Configure specific origins for production
                .withSockJS(); // Enable SockJS fallback
    }
}
