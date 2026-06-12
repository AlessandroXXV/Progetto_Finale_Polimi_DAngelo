package it.eably.backend.config;

import it.eably.backend.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Beans that extend Spring Security defaults.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Configuration
public class CustomConfig {

    private final UserRepository userRepository;

    public CustomConfig(UserRepository userRepository)
    {
        this.userRepository = userRepository;
    }

    /**
     * Allows authentication with either username or email.
     * Username lookup is tried first; email is the fallback so that both login forms
     * (username-based and email-based) go through the same filter chain.
     */
    @Bean
    protected UserDetailsService userDetailsService() {
        return (usernameOrEmail)->userRepository.findByUsername(usernameOrEmail)
                .orElseGet(() -> userRepository.findByEmail(usernameOrEmail)
                        .orElseThrow(() -> new UsernameNotFoundException(
                                "User not found with username or email: " + usernameOrEmail)));
    }
}
