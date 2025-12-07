package fpt.wongun.trading_ai.config;

import fpt.wongun.trading_ai.domain.entity.Role;
import fpt.wongun.trading_ai.domain.entity.User;
import fpt.wongun.trading_ai.repository.RoleRepository;
import fpt.wongun.trading_ai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Initializing application data...");
        
        initializeRoles();
        initializeAdminUser();
        
        log.info("Data initialization completed");
    }

    private void initializeRoles() {
        log.info("Initializing roles...");
        
        // Create ADMIN role
        if (!roleRepository.existsByName("ROLE_ADMIN")) {
            Role adminRole = Role.builder()
                    .name("ROLE_ADMIN")
                    .description("Administrator with full system access")
                    .build();
            roleRepository.save(adminRole);
            log.info("Created ROLE_ADMIN");
        }

        // Create TRADER role
        if (!roleRepository.existsByName("ROLE_TRADER")) {
            Role traderRole = Role.builder()
                    .name("ROLE_TRADER")
                    .description("Trader with access to create and manage own signals and positions")
                    .build();
            roleRepository.save(traderRole);
            log.info("Created ROLE_TRADER");
        }

        // Create VIEWER role
        if (!roleRepository.existsByName("ROLE_VIEWER")) {
            Role viewerRole = Role.builder()
                    .name("ROLE_VIEWER")
                    .description("Viewer with read-only access")
                    .build();
            roleRepository.save(viewerRole);
            log.info("Created ROLE_VIEWER");
        }
    }

    private void initializeAdminUser() {
        log.info("Initializing admin user...");
        
        if (!userRepository.existsByUsername("admin")) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));

            User admin = User.builder()
                    .username("admin")
                    .email("admin@trading-ai.com")
                    .fullName("System Administrator")
                    .password(passwordEncoder.encode("admin123"))
                    .enabled(true)
                    .roles(Set.of(adminRole))
                    .build();

            userRepository.save(admin);
            log.info("Created admin user - Username: admin, Password: admin123");
            log.warn("SECURITY WARNING: Please change the default admin password in production!");
        }
    }
}
