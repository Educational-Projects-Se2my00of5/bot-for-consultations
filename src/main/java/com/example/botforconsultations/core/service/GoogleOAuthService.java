package com.example.botforconsultations.core.service;

import com.example.botforconsultations.config.GoogleCalendarConfig;
import com.example.botforconsultations.core.model.GoogleCalendarToken;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.repository.GoogleCalendarTokenRepository;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

/**
 * Сервис для OAuth авторизации с Google Calendar
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar";

    private final GoogleCalendarConfig config;
    private final GoogleCalendarTokenRepository tokenRepository;

    /**
     * Генерация URL для OAuth авторизации
     */
    public String getAuthorizationUrl(Long userId) {
        try {
            GoogleAuthorizationCodeFlow flow = createFlow();

            return flow.newAuthorizationUrl()
                    .setRedirectUri(config.getRedirectUri())
                    .setState(String.valueOf(userId)) // Передаем userId через state
                    .build();
        } catch (Exception e) {
            log.error("Error generating authorization URL: {}", e.getMessage());
            throw new RuntimeException("Failed to generate authorization URL", e);
        }
    }

    /**
     * Обработка callback от Google и сохранение токенов
     */
    @Transactional
    public void handleCallback(String code, TelegramUser user) {
        try {
            log.info("Handling OAuth callback for user #{}", user.getId());

            GoogleAuthorizationCodeFlow flow = createFlow();
            log.debug("Created GoogleAuthorizationCodeFlow");

            GoogleTokenResponse tokenResponse = flow.newTokenRequest(code)
                    .setRedirectUri(config.getRedirectUri())
                    .execute();
            log.debug("Received token response from Google");

            // Вычисляем время истечения токена
            LocalDateTime expiresAt = LocalDateTime.now()
                    .plusSeconds(tokenResponse.getExpiresInSeconds());
            log.debug("Token expires at: {}", expiresAt);

            // Проверяем, есть ли уже токен для этого пользователя
            Optional<GoogleCalendarToken> existingToken = tokenRepository.findByUser(user);

            if (existingToken.isPresent()) {
                // Обновляем существующий токен
                GoogleCalendarToken token = existingToken.get();
                token.setAccessToken(tokenResponse.getAccessToken());
                token.setRefreshToken(tokenResponse.getRefreshToken());
                token.setExpiresAt(expiresAt);
                tokenRepository.save(token);
                log.info("Updated Google Calendar token for user #{}", user.getId());
            } else {
                // Создаем новый токен
                GoogleCalendarToken token = GoogleCalendarToken.builder()
                        .user(user)
                        .accessToken(tokenResponse.getAccessToken())
                        .refreshToken(tokenResponse.getRefreshToken())
                        .expiresAt(expiresAt)
                        .build();
                tokenRepository.save(token);
                log.info("Saved new Google Calendar token for user #{}", user.getId());
            }

        } catch (Exception e) {
            log.error("Error handling OAuth callback for user #{}: {}", user.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to handle OAuth callback: " + e.getMessage(), e);
        }
    }

    /**
     * Получение валидного Credential для пользователя
     * (автоматически обновляет токен если истёк)
     */
    @Transactional
    public Optional<Credential> getCredential(TelegramUser user) {
        Optional<GoogleCalendarToken> tokenOpt = tokenRepository.findByUser(user);

        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }

        GoogleCalendarToken token = tokenOpt.get();

        // Если токен истёк, обновляем его
        if (token.isExpired()) {
            try {
                refreshToken(token);
            } catch (Exception e) {
                log.error("Failed to refresh token for user #{}: {}", user.getId(), e.getMessage());
                return Optional.empty();
            }
        }

        // Создаем Credential
        Credential credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                .setTransport(new NetHttpTransport())
                .setJsonFactory(JSON_FACTORY)
                .setTokenServerUrl(new GenericUrl("https://oauth2.googleapis.com/token"))
                .setClientAuthentication(new ClientParametersAuthentication(
                        config.getClientId(), config.getClientSecret()))
                .build();

        credential.setAccessToken(token.getAccessToken());
        credential.setRefreshToken(token.getRefreshToken());
        credential.setExpiresInSeconds(3600L); // Примерное значение

        return Optional.of(credential);
    }

    /**
     * Обновление access token через refresh token
     */
    @Transactional
    public void refreshToken(GoogleCalendarToken token) {
        try {
            GoogleAuthorizationCodeFlow flow = createFlow();

            Credential credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                    .setTransport(new NetHttpTransport())
                    .setJsonFactory(JSON_FACTORY)
                    .setTokenServerUrl(new GenericUrl("https://oauth2.googleapis.com/token"))
                    .setClientAuthentication(new ClientParametersAuthentication(
                            config.getClientId(), config.getClientSecret()))
                    .build();

            credential.setRefreshToken(token.getRefreshToken());
            credential.refreshToken();

            // Обновляем токен в базе
            token.setAccessToken(credential.getAccessToken());
            token.setExpiresAt(LocalDateTime.now().plusSeconds(3600)); // Google обычно дает 1 час
            tokenRepository.save(token);

            log.info("Refreshed access token for user #{}", token.getUser().getId());

        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to refresh token", e);
        }
    }

    /**
     * Отключение Google Calendar (удаление токенов)
     */
    @Transactional
    public void disconnect(TelegramUser user) {
        tokenRepository.deleteByUser(user);
        log.info("Disconnected Google Calendar for user #{}", user.getId());
    }

    /**
     * Проверка, подключен ли Google Calendar у пользователя
     */
    public boolean isConnected(TelegramUser user) {
        return tokenRepository.existsByUser(user);
    }

    /**
     * Создание GoogleAuthorizationCodeFlow
     */
    private GoogleAuthorizationCodeFlow createFlow() {
        try {
            return new GoogleAuthorizationCodeFlow.Builder(
                    new NetHttpTransport(),
                    JSON_FACTORY,
                    config.getClientId(),
                    config.getClientSecret(),
                    Collections.singleton(CALENDAR_SCOPE))
                    .setAccessType("offline")
                    .setApprovalPrompt("force")
                    .build();
        } catch (Exception e) {
            log.error("Error creating authorization flow: {}", e.getMessage());
            throw new RuntimeException("Failed to create authorization flow", e);
        }
    }
}
