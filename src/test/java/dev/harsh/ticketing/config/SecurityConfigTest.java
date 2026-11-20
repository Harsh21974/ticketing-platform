package dev.harsh.ticketing.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void mapsRoleClaimToPrefixedAuthority() {
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "HS256")
            .subject("1")
            .claim("role", "ORGANIZER")
            .build();

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .contains("ROLE_ORGANIZER");
    }

    @Test
    void ignoresClaimsOtherThanRole() {
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "HS256")
            .subject("1")
            .claim("scope", "read")
            .build();

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .noneMatch(authority -> authority.startsWith("ROLE_"));
    }
}
