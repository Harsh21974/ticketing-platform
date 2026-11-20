package dev.harsh.ticketing.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import dev.harsh.ticketing.user.Role;
import dev.harsh.ticketing.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtServiceTest {

    private static final SecretKey KEY = new SecretKeySpec(
        "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    private JwtService jwtService;
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new NimbusJwtEncoder(new ImmutableSecret<>(KEY)), 60);
        jwtDecoder = NimbusJwtDecoder.withSecretKey(KEY).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Test
    void createTokenCarriesUserClaims() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(42L);
        when(user.getEmail()).thenReturn("attendee@example.com");
        when(user.getRole()).thenReturn(Role.ORGANIZER);

        Jwt jwt = jwtDecoder.decode(jwtService.createToken(user));

        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("ticketing-platform");
        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("attendee@example.com");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ORGANIZER");
        assertThat(jwt.getIssuedAt()).isNotNull();
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).toSeconds()).isEqualTo(3600);
    }

    @Test
    void expirySecondsReflectsConfiguredMinutes() {
        assertThat(jwtService.expirySeconds()).isEqualTo(3600);
    }
}
