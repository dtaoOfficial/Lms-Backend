package com.dtao.lms.controller;

import com.dtao.lms.model.User;
import com.dtao.lms.payload.LoginRequest;
import com.dtao.lms.payload.RegisterRequest;
import com.dtao.lms.security.JwtTokenProvider;
import com.dtao.lms.service.CourseService;
import com.dtao.lms.service.EnrollmentService;
import com.dtao.lms.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(
        origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}",
        allowCredentials = "true",
        allowedHeaders = "*"
)
public class UserController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final EnrollmentService enrollmentService;
    private final CourseService courseService;

    public UserController(UserService userService,
                          AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider,
                          EnrollmentService enrollmentService,
                          CourseService courseService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.enrollmentService = enrollmentService;
        this.courseService = courseService;
    }

    // ============================
    // REGISTER NEW USER
    // ============================
    @PostMapping("")
    public ResponseEntity<?> createUser(@RequestBody RegisterRequest req) {
        try {
            if (req == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Payload required"));

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
                    "id", saved.getId(),
                    "email", saved.getEmail(),
                    "message", "Registered successfully. Please verify via OTP sent to your email."
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error"));
        }
    }

    // ============================
    // LOGIN USER
    // ============================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        try {
            if (loginRequest == null || loginRequest.getEmail() == null || loginRequest.getPassword() == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Email and password required"));

            try {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                loginRequest.getEmail().trim().toLowerCase(),
                                loginRequest.getPassword()
                        );
                authenticationManager.authenticate(authToken);
            } catch (AuthenticationException ae) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
            }

            String jwt = jwtTokenProvider.generateToken(
                    loginRequest.getEmail().trim().toLowerCase(), null, null, loginRequest.isRememberMe()
            );
            long expiresIn = jwtTokenProvider.getExpiresInSeconds(loginRequest.isRememberMe());

            Cookie jwtCookie = new Cookie("JWT", jwt);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge((int) expiresIn);
            response.addCookie(jwtCookie);

            return ResponseEntity.ok(Map.of("token", jwt, "expiresIn", expiresIn));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Login failed"));
        }
    }

    // ============================
    // GET CURRENT USER DETAILS
    // ============================
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName() == null)
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

            Optional<User> maybe = userService.getUserByEmail(auth.getName());
            if (maybe.isEmpty())
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));

            User u = maybe.get();
            Map<String, Object> resp = new HashMap<>();
            resp.put("id", u.getId());
            resp.put("name", u.getName());
            resp.put("department", u.getDepartment());
            resp.put("email", u.getEmail());
            resp.put("phone", u.getPhone());
            resp.put("role", u.getRole());
            resp.put("createdBy", u.getCreatedBy());
            resp.put("createdAt", u.getCreatedAt());
            resp.put("updatedAt", u.getUpdatedAt());
            resp.put("active", u.isActive());
            resp.put("isVerified", u.isVerified());

            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error"));
        }
    }


    // ============================
    // GET MY COURSES
    // ============================
    @GetMapping("/me/courses")
    public ResponseEntity<?> getMyCourses() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName() == null)
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

            String email = auth.getName();
            var enrollments = enrollmentService.getEnrollmentsByEmail(email);
            if (enrollments == null) enrollments = Collections.emptyList();

            List<Map<String, Object>> result = new ArrayList<>();
            for (var e : enrollments) {
                Map<String, Object> obj = new HashMap<>();
                obj.put("enrollment", e);
                var cOpt = courseService.getCourseById(e.getCourseId());
                cOpt.ifPresentOrElse(c -> {
                    obj.put("course", c);
                    obj.put("courseTitle", c.getTitle());
                    obj.put("courseSummary", c.getDescription());
                }, () -> {
                    obj.put("course", null);
                    obj.put("courseTitle", null);
                    obj.put("courseSummary", null);
                });
                result.add(obj);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error"));
        }
    }

    // ============================
    // UPDATE PROFILE (CURRENT USER)
    // ============================
    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfileForCurrent(@RequestBody Map<String, Object> payload) {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName() == null)
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

            String email = auth.getName();
            var maybe = userService.getUserByEmail(email);
            if (maybe.isEmpty())
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));

            User current = maybe.get();
            User fake = new User();
            if (payload.containsKey("name")) fake.setName((String) payload.get("name"));
            if (payload.containsKey("phone")) fake.setPhone(Objects.toString(payload.get("phone"), null));
            if (payload.containsKey("department")) fake.setDepartment((String) payload.get("department"));

            var updatedOpt = userService.updateUser(current.getId(), fake);
            if (updatedOpt.isEmpty())
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));

            User updated = updatedOpt.get();
            return ResponseEntity.ok(Map.of(
                    "message", "Profile updated successfully",
                    "user", updated
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error"));
        }
    }

    // ============================
    // CHANGE PASSWORD
    // ============================
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body) {
        try {
            if (body == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Payload required"));

            String oldPassword = body.get("oldPassword");
            String newPassword = body.get("newPassword");
            if (oldPassword == null || newPassword == null)
                return ResponseEntity.badRequest().body(Map.of("error", "oldPassword and newPassword are required"));

            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName() == null)
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

            userService.changePassword(auth.getName(), oldPassword, newPassword);
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error"));
        }
    }

    // ============================
    // ADMIN: GET ALL USERS
    // ============================
    @GetMapping({"", "/"})
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> all = userService.getAllUsers();
            List<Map<String, Object>> resp = all.stream().map(u -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", u.getId());
                map.put("name", u.getName());
                map.put("email", u.getEmail());
                map.put("department", u.getDepartment());
                map.put("role", u.getRole());
                map.put("phone", u.getPhone());
                map.put("createdAt", u.getCreatedAt());
                map.put("updatedAt", u.getUpdatedAt());
                map.put("active", u.isActive());
                map.put("isVerified", u.isVerified());
                return map;
            }).toList();

            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch users"));
        }
    }

    // ============================
    // ADMIN: GET USER BY ID
    // ============================
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable String id) {
        var maybe = userService.getUserById(id);
        if (maybe.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        return ResponseEntity.ok(maybe.get());
    }

    // ============================
    // ADMIN: DELETE USER
    // ============================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to delete user"));
        }
    }

    // ============================
    // ADMIN: UPDATE USER
    // ============================
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable String id, @RequestBody Map<String, Object> payload) {
        try {
            User fake = new User();
            if (payload.containsKey("name")) fake.setName((String) payload.get("name"));
            if (payload.containsKey("phone")) fake.setPhone(Objects.toString(payload.get("phone"), null));
            if (payload.containsKey("department")) fake.setDepartment((String) payload.get("department"));
            if (payload.containsKey("role")) fake.setRole((String) payload.get("role"));
            if (payload.containsKey("passwordHash")) fake.setPasswordHash((String) payload.get("passwordHash"));

            var updatedOpt = userService.updateUser(id, fake);
            if (updatedOpt.isEmpty())
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));

            return ResponseEntity.ok(Map.of("message", "User updated", "user", updatedOpt.get()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Update failed"));
        }
    }
}
