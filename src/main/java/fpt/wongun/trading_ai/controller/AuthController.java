package fpt.wongun.trading_ai.controller;

import fpt.wongun.trading_ai.dto.ApiResponse;
import fpt.wongun.trading_ai.dto.auth.LoginRequestDto;
import fpt.wongun.trading_ai.dto.auth.LoginResponseDto;
import fpt.wongun.trading_ai.dto.auth.RegisterRequestDto;
import fpt.wongun.trading_ai.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User authentication and registration")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and receive JWT token")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        log.info("Login request received for user: {}", request.getUsername());
        LoginResponseDto response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/register")
    @Operation(summary = "User registration", description = "Register new user and receive JWT token")
    public ResponseEntity<ApiResponse<LoginResponseDto>> register(@Valid @RequestBody RegisterRequestDto request) {
        log.info("Registration request received for user: {}", request.getUsername());
        LoginResponseDto response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Registration successful"));
    }
}
