package com.liujianan.agentdemo.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void register_shouldPersistUserAndReturnJwt() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"enterprise_user\",\"password\":\"Test123456\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.username").value("enterprise_user"));
    }

    @Test
    void protectedApi_withoutToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/documents/page"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void protectedApi_withToken_shouldAllowAccess() throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"enterprise_user2\",\"password\":\"Test123456\"}"))
                .andReturn().getResponse().getContentAsString();
        String token = response.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/documents/page")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void refreshAndLogout_shouldRotateRefreshTokenAndRevokeAccessToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"enterprise_user3\",\"password\":\"Test123456\"}"))
                .andReturn().getResponse().getContentAsString();
        String token = response.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        String refreshToken = response.replaceAll(".*\"refreshToken\":\"([^\"]+)\".*", "$1");

        String refreshResponse = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn().getResponse().getContentAsString();
        String rotatedRefreshToken = refreshResponse.replaceAll(".*\"refreshToken\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + rotatedRefreshToken + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/documents/page")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
