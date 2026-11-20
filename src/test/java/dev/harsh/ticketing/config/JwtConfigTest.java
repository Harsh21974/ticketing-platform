package dev.harsh.ticketing.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigTest {

    private final JwtConfig jwtConfig = new JwtConfig();

    @Test
    void rejectsSecretShorterThan32Bytes() {
        assertThatThrownBy(() -> jwtConfig.jwtSecretKey("a".repeat(31)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("32");
    }

    @Test
    void acceptsSecretOfExactly32Bytes() {
        SecretKey key = jwtConfig.jwtSecretKey("0123456789abcdef0123456789abcdef");

        assertThat(key.getAlgorithm()).isEqualTo("HmacSHA256");
        assertThat(key.getEncoded()).hasSize(32);
    }

    @Test
    void encoderAndDecoderRoundTrip() {
        SecretKey key = jwtConfig.jwtSecretKey("0123456789abcdef0123456789abcdef");
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("test").subject("7").build();

        String token = jwtConfig.jwtEncoder(key)
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();

        Jwt decoded = jwtConfig.jwtDecoder(key).decode(token);

        assertThat(decoded.getSubject()).isEqualTo("7");
    }
}
