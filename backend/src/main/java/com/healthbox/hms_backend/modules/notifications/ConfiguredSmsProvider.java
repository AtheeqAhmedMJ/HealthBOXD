package com.healthbox.hms_backend.modules.notifications;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "sms.provider-url")
public class ConfiguredSmsProvider implements SmsProvider {
    private final RestClient client;
    private final String apiKey;
    private final String senderId;

    public ConfiguredSmsProvider(@Value("${sms.provider-url}") String providerUrl,
                                 @Value("${sms.api-key:}") String apiKey,
                                 @Value("${sms.sender-id:HealthBoxD}") String senderId) {
        this.client = RestClient.builder().baseUrl(providerUrl).build();
        this.apiKey = apiKey;
        this.senderId = senderId;
    }

    @Override
    public SmsResult send(String phoneNumber, String message) {
        try {
            client.post().contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(Map.of("to", phoneNumber, "from", senderId, "message", message))
                    .retrieve().toBodilessEntity();
            return new SmsResult(true, null, null);
        } catch (Exception exception) {
            return new SmsResult(false, null, "SMS provider request failed");
        }
    }
}
