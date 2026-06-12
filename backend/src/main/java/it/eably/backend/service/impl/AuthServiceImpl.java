package it.eably.backend.service.impl;

import it.eably.backend.dto.auth.response.AuthResponseDTO;
import it.eably.backend.dto.auth.request.LoginRequestDTO;
import it.eably.backend.dto.auth.request.RegisterRequestDTO;
import it.eably.backend.exception.ValidationException;
import it.eably.backend.model.User;
import it.eably.backend.model.UserRole;
import it.eably.backend.repository.UserRepository;
import it.eably.backend.security.JwtTokenProvider;
import it.eably.backend.service.def.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Service implementation for authentication operations (register, login).
 *
 * <p>This service handles:</p>
 * <ul>
 *   <li>User registration with BCrypt password hashing</li>
 *   <li>User authentication with JWT token generation</li>
 *   <li>Email and username uniqueness validation</li>
 * </ul>
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Service
public class AuthServiceImpl implements AuthService {

    /** Logger for auth service events. */
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    /** Repository for user data. */
    private final UserRepository userRepository;
    /** Encoder for user passwords. */
    private final PasswordEncoder passwordEncoder;
    /** JWT token provider. */
    private final JwtTokenProvider jwtTokenProvider;
    /** Spring Security authentication manager. */
    private final AuthenticationManager authenticationManager;

    /**
     * Builds the authentication service with required dependencies.
     *
     * @param userRepository repository for users
     * @param passwordEncoder encoder for passwords
     * @param jwtTokenProvider JWT token provider
     * @param authenticationManager authentication manager
     */
    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Registers a new user.
     *
     * <p>Process:</p>
     * <ol>
     *   <li>Validate email and username uniqueness</li>
     *   <li>Hash password with BCrypt</li>
     *   <li>Create User entity with CLIENT role (default)</li>
     *   <li>Save to database</li>
     *   <li>Generate JWT tokens</li>
     *   <li>Return AuthResponseDTO</li>
     * </ol>
     *
     * @param request registration request DTO
     * @return authentication response with JWT tokens
     * @throws ValidationException if email or username already exists or role is invalid
     */
    @Override
    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        log.info("Registering new user with username: {}", request.username());

        // Enforce unique email and username across existing users.
        List<User> existing = userRepository.findByUsernameOrEmail(request.username(), request.email());
        if (existing.stream().anyMatch(u -> u.getEmail().equals(request.email()))) {
            log.warn("Registration failed: email already exists: {}", request.email());
            throw new ValidationException("Email already exists: " + request.email());
        }
        if (existing.stream().anyMatch(u -> u.getUsername().equals(request.username()))) {
            log.warn("Registration failed: username already exists: {}", request.username());
            throw new ValidationException("Username already exists: " + request.username());
        }

        // Only allow CLIENT or STUDENT role on self-registration.
        if (request.role() != UserRole.CLIENT && request.role() != UserRole.STUDENT && request.role() != null) {
            log.warn("Registration failed: cannot register with role: {}", request.role());
            throw new ValidationException("Cannot register with role: " + request.role());
        }

        // Hash password before persisting.
        String passwordHash = passwordEncoder.encode(request.password());

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordHash);
        user.setRole(request.role() != null ? request.role() : UserRole.CLIENT);
        user.setIsActive(true);
        user.setIsVerified(false);
        user.setStripeConnected(false);
        user.setFullName(request.firstName() + " " + request.lastName());

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        // Issue access and refresh tokens for the new account.
        String token = jwtTokenProvider.generateTokenFromUsername(
                savedUser.getUsername(),
                Collections.singletonList(savedUser.getRole().getAuthority())
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.getUsername());

        return new AuthResponseDTO(
                token,
                refreshToken,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                savedUser.getStripeConnected(),
                savedUser.getIsVerified()
        );
    }

    /**
     * Authenticates a user and generates JWT tokens.
     *
     * <p>Process:</p>
     * <ol>
     *   <li>Authenticate with Spring Security AuthenticationManager</li>
     *   <li>Load user from database</li>
     *   <li>Generate JWT tokens</li>
     *   <li>Return AuthResponseDTO</li>
     * </ol>
     *
     * @param request login request DTO
     * @return authentication response with JWT tokens
     * @throws ValidationException if authentication fails
     */
    @Override
    public AuthResponseDTO login(LoginRequestDTO request) {
        log.info("Authenticating user: {}", request.usernameOrEmail());

        // Authenticate credentials via Spring Security.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.usernameOrEmail(),
                        request.password()
                )
        );

        // Resolve user by username or email for response fields.
        User user = userRepository.findByUsername(request.usernameOrEmail())
                .orElseGet(() -> userRepository.findByEmail(request.usernameOrEmail())
                        .orElseThrow(() -> new ValidationException("User not found")));

        log.info("User authenticated successfully: {}", user.getUsername());

        // Issue access and refresh tokens.
        String token = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

        return new AuthResponseDTO(
                token,
                refreshToken,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getStripeConnected(),
                user.getIsVerified()
        );
    }
}
