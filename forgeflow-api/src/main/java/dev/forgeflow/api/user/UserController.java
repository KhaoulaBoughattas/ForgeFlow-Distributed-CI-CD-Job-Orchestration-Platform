package dev.forgeflow.api.user;

import dev.forgeflow.api.common.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public UserSummary me(@AuthenticationPrincipal AuthenticatedUser principal) {
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new UserNotFoundException(principal.id()));
        return UserSummary.from(user);
    }
}
