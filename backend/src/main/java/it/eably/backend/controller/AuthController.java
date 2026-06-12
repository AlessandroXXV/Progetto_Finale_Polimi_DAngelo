package it.eably.backend.controller;

import it.eably.backend.dto.auth.response.AuthResponseDTO;
import it.eably.backend.dto.auth.request.LoginRequestDTO;
import it.eably.backend.dto.auth.request.RegisterRequestDTO;
import it.eably.backend.service.def.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations.
 * <p>
 * Endpoints:
 * <ul>
 * <li>POST /api/v1/auth/register - Register new user</li>
 * <li>POST /api/v1/auth/login - Authenticate user</li>
 * </ul>
 *
 * <p>
 * Security:
 * - All endpoints are public (no authentication required)
 * - Returns JWT tokens for authenticated sessions
 * <p>
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registers a new user.
     * <p>
     * Validates request data using Jakarta Bean Validation.
     * Returns JWT tokens for immediate authentication.
     *
     * @param request registration request DTO
     * @return authentication response with JWT tokens
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        log.info("Registration request for username: {}", request.username());

        AuthResponseDTO response = authService.register(request);

        log.info("User registered successfully: {}", response.username());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticates a user.
     * <p>
     * Accepts username or email with password.
     * Returns JWT tokens on successful authentication.
     *
     * @param request login request DTO
     * @return authentication response with JWT tokens
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        log.info("Login request for: {}", request.usernameOrEmail());

        AuthResponseDTO response = authService.login(request);

        log.info("User logged in successfully: {}", response.username());
        return ResponseEntity.ok(response);
    }
}
