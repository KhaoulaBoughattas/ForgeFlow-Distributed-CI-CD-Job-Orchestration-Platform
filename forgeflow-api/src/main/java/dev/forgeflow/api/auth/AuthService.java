package dev.forgeflow.api.auth;

import dev.forgeflow.api.common.security.JwtService;
import dev.forgeflow.api.user.User;
import dev.forgeflow.api.user.UserRepository;
import dev.forgeflow.api.user.UserSummary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(normalizedEmail);
        }

        User user = User.register(normalizedEmail, passwordEncoder.encode(request.password()), request.displayName().trim());
        userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, UserSummary.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, UserSummary.from(user));
    }
}
