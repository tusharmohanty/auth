package com.portfolio.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.auth.config.KiteConfig;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class KiteAuthService {

    private final KiteConfig config;
    private final TokenService tokenService;

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public String loginUrl() {
        return "https://kite.trade/connect/login?api_key=" + config.getApiKey() + "&v=3";
    }

    public void exchangeAndStore(String requestToken) throws IOException {
        RequestBody body = new FormBody.Builder()
                .add("api_key", config.getApiKey())
                .add("request_token", requestToken)
                .add("checksum", checksum(requestToken))
                .build();

        Request request = new Request.Builder()
                .url("https://api.kite.trade/session/token")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = (response.body() != null) ? response.body().string() : "";

            if (!response.isSuccessful()) {
                throw new IOException("Kite token API failed. HTTP " + response.code() + ": " + responseBody);
            }

            JsonNode root = mapper.readTree(responseBody);
            String accessToken = root.path("data").path("access_token").asText(null);
            if (accessToken == null || accessToken.isBlank()) {
                throw new IOException("access_token not found in: " + responseBody);
            }

            tokenService.save(accessToken);
        }
    }

    public boolean isTokenValid(String accessToken) {
        Request request = new Request.Builder()
                .url("https://api.kite.trade/user/profile")
                .get()
                .addHeader("X-Kite-Version", "3")
                .addHeader("Authorization", "token " + config.getApiKey() + ":" + accessToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            int code = response.code();
            if (code == 200) return true;
            if (code == 401 || code == 403) return false;

            // If Kite returns something else (5xx, etc), don’t force relogin
            return true;
        } catch (Exception e) {
            // Network issues: don't break system with endless login loops
            return true;
        }
    }

    private String checksum(String requestToken) {
        String data = config.getApiKey() + requestToken + config.getApiSecret();
        return org.apache.commons.codec.digest.DigestUtils.sha256Hex(data);
    }
}