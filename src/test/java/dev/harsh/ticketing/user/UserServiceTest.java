package dev.harsh.ticketing.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void registerNormalizesEmailAndHashesPassword() {
        when(userRepository.existsByEmail("attendee@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.register("  Attendee@Example.COM  ", "secret123", Role.ATTENDEE);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();

        assertThat(user).isSameAs(saved);
        assertThat(saved.getEmail()).isEqualTo("attendee@example.com");
        assertThat(saved.getRole()).isEqualTo(Role.ATTENDEE);
        assertThat(saved.getPasswordHash()).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", saved.getPasswordHash())).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void registerRejectsAdminWithBadRequest() {
        assertThatThrownBy(() -> userService.register("admin@example.com", "secret123", Role.ADMIN))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void registerRejectsDuplicateEmailWithConflict() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register("taken@example.com", "secret123", Role.ATTENDEE))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void registerMapsDataIntegrityViolationToConflict() {
        when(userRepository.existsByEmail("race@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> userService.register("race@example.com", "secret123", Role.ORGANIZER))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void authenticateReturnsUserForValidCredentials() {
        User user = new User("attendee@example.com", passwordEncoder.encode("secret123"), Role.ATTENDEE);
        when(userRepository.findByEmail("attendee@example.com")).thenReturn(Optional.of(user));

        User result = userService.authenticate(" Attendee@Example.com ", "secret123");

        assertThat(result).isSameAs(user);
    }

    @Test
    void authenticateRejectsWrongPasswordWithUnauthorized() {
        User user = new User("attendee@example.com", passwordEncoder.encode("secret123"), Role.ATTENDEE);
        when(userRepository.findByEmail("attendee@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.authenticate("attendee@example.com", "wrong-password"))
            .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(ex.getReason()).isEqualTo("Invalid email or password");
            });
    }

    @Test
    void authenticateRejectsUnknownEmailWithUnauthorized() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.authenticate("missing@example.com", "secret123"))
            .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(ex.getReason()).isEqualTo("Invalid email or password");
            });
    }

    @Test
    void authenticateMapsUnexpectedFailureToUnauthorized() {
        when(userRepository.findByEmail("broken@example.com")).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> userService.authenticate("broken@example.com", "secret123"))
            .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(ex.getReason()).isEqualTo("Invalid email or password");
            });
    }
}
