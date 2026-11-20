package dev.harsh.ticketing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTests {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");
    private static final String VALID_PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerLoginAndFetchCurrentUser() throws Exception {
        String email = uniqueEmail();
        register(email, VALID_PASSWORD, "ATTENDEE");

        String token = login(email, VALID_PASSWORD);

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.role").value("ATTENDEE"));
    }

    @Test
    void meWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        String email = uniqueEmail();
        register(email, VALID_PASSWORD, "ATTENDEE");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(email, "wrong-password")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        String email = uniqueEmail();
        register(email, VALID_PASSWORD, "ATTENDEE");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(email, VALID_PASSWORD, "ATTENDEE")))
            .andExpect(status().isConflict());
    }

    @Test
    void registeringAdminReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(uniqueEmail(), VALID_PASSWORD, "ADMIN")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shortPasswordReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(uniqueEmail(), "short", "ATTENDEE")))
            .andExpect(status().isBadRequest());
    }

    private void register(String email, String password, String role) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(email, password, role)))
            .andExpect(status().is2xxSuccessful());
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(email, password)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Matcher matcher = TOKEN_PATTERN.matcher(body);
        assertThat(matcher.find())
            .as("login response should contain a token, but was: %s", body)
            .isTrue();
        return matcher.group(1);
    }

    private static String registerBody(String email, String password, String role) {
        return """
            {"email":"%s","password":"%s","role":"%s"}
            """.formatted(email, password, role);
    }

    private static String loginBody(String email, String password) {
        return """
            {"email":"%s","password":"%s"}
            """.formatted(email, password);
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
