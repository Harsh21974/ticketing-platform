package dev.harsh.ticketing;

import dev.harsh.ticketing.auth.JwtService;
import dev.harsh.ticketing.user.Role;
import dev.harsh.ticketing.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SecurityFilterChainTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());
    }

    @Test
    void authEndpointsArePublic() throws Exception {
        mockMvc.perform(get("/api/auth/login"))
            .andExpect(status().isNotFound());
    }

    @Test
    void otherEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/events"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenIsAccepted() throws Exception {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn("attendee@example.com");
        when(user.getRole()).thenReturn(Role.ATTENDEE);

        String token = jwtService.createToken(user);

        mockMvc.perform(get("/api/events").header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }
}
