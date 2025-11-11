package com.dtao.lms.controller;

import com.dtao.lms.model.AuthToken;
import com.dtao.lms.model.Session;
import com.dtao.lms.model.User;
import com.dtao.lms.payload.AuthResponse;
import com.dtao.lms.payload.LoginRequest;
import com.dtao.lms.payload.RegisterRequest;
import com.dtao.lms.payload.VerifyEmailRequest;
import com.dtao.lms.repo.AuthTokenRepository;
import com.dtao.lms.security.JwtTokenProvider;
import com.dtao.lms.service.AuthService;
import com.dtao.lms.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(
        origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}",
        allowCredentials = "true",
        allowedHeaders = "*"
)
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthTokenRepository authTokenRepository;

    public AuthController(UserService userService,
                          AuthService authService,
                          AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider,
                          AuthTokenRepository authTokenRepository) {
        this.userService = userService;
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authTokenRepository = authTokenRepository;
    }

    // ---------------------------
    // REGISTER
    // ---------------------------
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            if (req == null) return ResponseEntity.badRequest().body(Map.of("error", "Payload required"));

            User user = new User();
            user.setName(req.getName());
            user.setEmail(req.getEmail());
            user.setPhone(req.getPhone());
            user.setDepartment(req.getDepartment());
            user.setRole(req.getRole());
            user.setPasswordHash(req.getPassword());
            user.setCreatedBy("system");

            User saved = userService.registerUser(user);
            return ResponseEntity.status(201).body(Map.of(
                    "message", "User registered. Please verify your email via OTP.",
                    "email", saved.getEmail()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error"));
        }
    }

    // ---------------------------
    // VERIFY EMAIL
    // ---------------------------
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody VerifyEmailRequest req) {
        try {
            if (req == null || req.getEmail() == null || req.getOtp() == null)
                return ResponseEntity.badRequest().body(Map.of("error", "email and otp required"));
            userService.verifyEmail(req.getEmail().trim().toLowerCase(), req.getOtp().trim());
            return ResponseEntity.ok(Map.of("message", "Email verified"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error"));
        }
    }

    // ---------------------------
    // LOGIN
    // ---------------------------
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        try {
            if (req == null || req.getEmail() == null || req.getPassword() == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Email and password required"));

            String email = req.getEmail().trim().toLowerCase();
            Optional<User> optUser = userService.getUserByEmail(email);
            if (optUser.isEmpty())
                return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));

            User user = optUser.get();
            if (!user.isVerified())
                return ResponseEntity.status(403).body(Map.of("error", "Email not verified. Please verify."));

            try {
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(email, req.getPassword())
                );
            } catch (AuthenticationException ex) {
                authService.handleFailedAuth(email);
                return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
            }

            authService.successfulLoginCleanup(email);

            String userAgent = request.getHeader("User-Agent");
            String ip = request.getRemoteAddr();
            Session session = authService.createSession(email, userAgent, ip);
            AuthToken refresh = authService.createRefreshToken(email, session.getSessionId());

            boolean remember = req.isRememberMe();
            String accessJwt = jwtTokenProvider.generateToken(user.getEmail(), user.getRole(), session.getSessionId(), remember);
            long expiresIn = jwtTokenProvider.getExpiresInSeconds(remember);

            // ---- Set cookies ----
            Cookie refreshCookie = buildCookie("REFRESH_TOKEN", refresh.getToken(), (int)
                    (refresh.getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond()));
            refreshCookie.setPath("/api/auth");
            response.addCookie(refreshCookie);

            // ❌ Removed JWT cookie creation — access token is returned in response and stored client-side.

            AuthResponse resp = new AuthResponse();
            resp.setAccessToken(accessJwt);
            resp.setAccessTokenExpiresIn(expiresIn);
            resp.setSessionId(session.getSessionId());
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("name", user.getName());
            userInfo.put("email", user.getEmail());
            userInfo.put("role", user.getRole());
            resp.setUser(userInfo);

            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error during login"));
        }
    }

    // ---------------------------
    // REFRESH TOKEN
    // ---------------------------
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        try {
            String refreshToken = null;
            if (request.getCookies() != null) {
                for (Cookie c : request.getCookies()) {
                    if ("REFRESH_TOKEN".equals(c.getName())) {
                        refreshToken = c.getValue();
                        break;
                    }
                }
            }
            if (refreshToken == null)
                return ResponseEntity.status(401).body(Map.of("error", "Refresh token required"));

            Optional<AuthToken> opt = authService.findValidRefresh(refreshToken);
            if (opt.isEmpty())
                return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired refresh token"));

            AuthToken old = opt.get();
            authService.revokeRefreshToken(old.getToken());
            AuthToken newRefresh = authService.createRefreshToken(old.getEmail(), old.getSessionId());

            User user = userService.getUserByEmail(old.getEmail()).orElse(null);
            if (user == null)
                return ResponseEntity.status(401).body(Map.of("error", "User not found"));

            String newAccess = jwtTokenProvider.generateToken(
                    user.getEmail(), user.getRole(), old.getSessionId(), false);
            long expiresIn = jwtTokenProvider.getExpiresInSeconds(false);

            Cookie refreshCookie = buildCookie("REFRESH_TOKEN", newRefresh.getToken(),
                    (int) (newRefresh.getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond()));
            refreshCookie.setPath("/api/auth");
            response.addCookie(refreshCookie);

            // ❌ Removed JWT cookie creation — access token returned in body only

            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccess,
                    "accessTokenExpiresIn", expiresIn,
                    "sessionId", old.getSessionId()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error during refresh"));
        }
    }

    // ---------------------------
    // LOGOUT
    // ---------------------------
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam(required = false) String sessionId,
                                    HttpServletResponse response) {
        try {
            // clear cookies
            Cookie jwt = buildCookie("JWT", "", 0);
            jwt.setPath("/");
            response.addCookie(jwt);

            Cookie refresh = buildCookie("REFRESH_TOKEN", "", 0);
            refresh.setPath("/api/auth");
            response.addCookie(refresh);

            if (sessionId != null && !sessionId.isBlank())
                authService.logoutSession(sessionId);

            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Logout failed"));
        }
    }

    // ---------------------------
    // Helper: cookie builder
    // ---------------------------
    private Cookie buildCookie(String name, String value, int maxAgeSeconds) {
        Cookie c = new Cookie(name, value);
        c.setHttpOnly(true);
        c.setMaxAge(maxAgeSeconds);
        // for localhost dev: SameSite=None & Secure=false
        // If using HTTPS, set secure=true
        c.setSecure(false);
        return c;
    }
}
