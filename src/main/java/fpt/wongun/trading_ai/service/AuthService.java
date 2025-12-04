package fpt.wongun.trading_ai.service;

import fpt.wongun.trading_ai.domain.entity.Role;
import fpt.wongun.trading_ai.domain.entity.User;
import fpt.wongun.trading_ai.dto.auth.LoginRequestDto;
import fpt.wongun.trading_ai.dto.auth.LoginResponseDto;
import fpt.wongun.trading_ai.dto.auth.RegisterRequestDto;
import fpt.wongun.trading_ai.exception.ResourceAlreadyExistsException;
import fpt.wongun.trading_ai.repository.RoleRepository;
import fpt.wongun.trading_ai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for authentication operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    /**
     * Authenticate user and generate JWT token.
     */
    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto request) {
        log.info("User attempting login: {}", request.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = tokenService.generateToken(authentication);

        User user = userRepository.findByUsernameWithRoles(request.getUsername())
                .orElseThrow();

        LoginResponseDto.UserInfoDto userInfo = LoginResponseDto.UserInfoDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .build();

        log.info("User logged in successfully: {}", request.getUsername());

        return LoginResponseDto.builder()
                .token(token)
                .expiresIn(tokenService.getExpirationMs())
                .user(userInfo)
                .build();
    }

    /**
     * Register new user with TRADER role by default.
     */
    @Transactional
    public LoginResponseDto register(RegisterRequestDto request) {
        log.info("New user registration: {}", request.getUsername());

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResourceAlreadyExistsException("Username already exists: " + request.getUsername());
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        // Get TRADER role (default role for new users)
        Role traderRole = roleRepository.findByName("ROLE_TRADER")
                .orElseThrow(() -> new RuntimeException("ROLE_TRADER not found. Run data initialization."));

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .roles(Set.of(traderRole))
                .build();

        userRepository.save(user);

        log.info("User registered successfully: {}", request.getUsername());

        // Auto login after registration
        LoginRequestDto loginRequest = LoginRequestDto.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .build();

        return login(loginRequest);
    }
}
