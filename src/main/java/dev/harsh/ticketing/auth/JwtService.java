package dev.harsh.ticketing.auth;

import dev.harsh.ticketing.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class JwtService {

    private static final String ISSUER = "ticketing-platform";

    private final JwtEncoder jwtEncoder;
    private final Duration expiry;

    public JwtService(JwtEncoder jwtEncoder, @Value("${app.jwt.expiry-minutes}") long expiryMinutes) {
        this.jwtEncoder = jwtEncoder;
        this.expiry = Duration.ofMinutes(expiryMinutes);
    }

    public String createToken(User user) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(ISSUER)
            .subject(user.getId().toString())
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plus(expiry))
            .claim("email", user.getEmail())
            .claim("role", user.getRole().name())
            .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long expirySeconds() {
        return expiry.toSeconds();
    }
}
