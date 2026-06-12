package it.eably.backend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import it.eably.backend.security.JwtTokenProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

    @Mock
    private MessageBrokerRegistry messageBrokerRegistry;

    @Mock
    private StompEndpointRegistry stompEndpointRegistry;

    @Mock
    private StompWebSocketEndpointRegistration endpointRegistration;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @Mock
    private UserDetailsService userDetailsService;

    private WebSocketConfig webSocketConfig;

    @BeforeEach
    void setUp() {
        webSocketConfig = new WebSocketConfig(jwtTokenProvider, userDetailsService);
    }

    @Test
    void configureMessageBroker_EnablesSimpleBroker() {
        when(messageBrokerRegistry.enableSimpleBroker(any(String[].class)))
                .thenReturn(null);
        when(messageBrokerRegistry.setApplicationDestinationPrefixes(any(String[].class)))
                .thenReturn(messageBrokerRegistry);
        when(messageBrokerRegistry.setUserDestinationPrefix(anyString()))
                .thenReturn(messageBrokerRegistry);

        webSocketConfig.configureMessageBroker(messageBrokerRegistry);

        verify(messageBrokerRegistry).enableSimpleBroker("/topic", "/queue");
        verify(messageBrokerRegistry).setApplicationDestinationPrefixes("/app");
        verify(messageBrokerRegistry).setUserDestinationPrefix("/user");
    }

    @Test
    void registerStompEndpoints_RegistersWsEndpoint() {
        when(stompEndpointRegistry.addEndpoint("/ws-eably"))
                .thenReturn(endpointRegistration);
        when(endpointRegistration.setAllowedOriginPatterns("*"))
                .thenReturn(endpointRegistration);

        webSocketConfig.registerStompEndpoints(stompEndpointRegistry);

        verify(stompEndpointRegistry).addEndpoint("/ws-eably");
        verify(endpointRegistration).setAllowedOriginPatterns("*");
        verify(endpointRegistration).withSockJS();
    }

    @Test
    void webSocketConfig_CanBeInstantiated() {
        WebSocketConfig config = new WebSocketConfig(jwtTokenProvider, userDetailsService);
        assertNotNull(config);
    }
}
