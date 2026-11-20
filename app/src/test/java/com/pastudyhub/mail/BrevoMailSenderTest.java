package com.pastudyhub.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** The Brevo client against a fake Brevo, which checks what the client sends. */
@DisplayName("Brevo mail sender")
class BrevoMailSenderTest {

    private static final String BASE_URL = "https://api.brevo.test/v3";

    private MockRestServiceServer server;
    private BrevoMailSender sender;

    @BeforeEach
    void setUp() {
        MailProperties properties = new MailProperties();
        properties.setBrevoApiKey("test-key");
        properties.setFromAddress("sender@example.com");
        properties.setFromName("PA Study Hub");

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        sender = new BrevoMailSender(properties, builder.build());
    }

    @Test
    @DisplayName("Sends the email in Brevo's format with the key in the api-key header")
    void sendsBrevoRequest() {
        server.expect(requestTo(BASE_URL + "/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "test-key"))
                .andExpect(jsonPath("$.sender.name").value("PA Study Hub"))
                .andExpect(jsonPath("$.sender.email").value("sender@example.com"))
                .andExpect(jsonPath("$.to[0].email").value("student@example.com"))
                .andExpect(jsonPath("$.subject").value("Reset your password"))
                .andExpect(jsonPath("$.textContent").value("Plain text"))
                .andExpect(jsonPath("$.htmlContent").value("<p>Plain text</p>"))
                .andRespond(withSuccess("{\"messageId\":\"<1@brevo>\"}", MediaType.APPLICATION_JSON));

        sender.send("student@example.com", "Reset your password", "Plain text", "<p>Plain text</p>");
        server.verify();
    }

    @Test
    @DisplayName("A refusal from Brevo (for example an IP not on the allow-list) becomes MailNotSentException")
    void refusal_throws() {
        server.expect(requestTo(BASE_URL + "/smtp/email"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"unauthorized\",\"message\":\"unrecognised IP address\"}"));

        assertThatThrownBy(() -> sender.send("student@example.com", "Subject", "Text", "<p>Text</p>"))
                .isInstanceOf(MailNotSentException.class)
                .hasMessageContaining("Brevo did not accept the email");
    }
}
