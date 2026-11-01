package com.healthbox.hms_backend.modules.notifications;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "sms.provider-enabled", havingValue = "true")
public class ConfiguredSmsProvider implements SmsProvider {
    private final RestClient client;
    private final String apiKey;
    private final String senderId;
    private final String templateId;

    public ConfiguredSmsProvider(@Value("${sms.provider-url}") String providerUrl,
                                 @Value("${sms.api-key:}") String apiKey,
                                 @Value("${sms.sender-id:}") String senderId,
                                 @Value("${sms.template-id:}") String templateId) {
        this.client = RestClient.builder().baseUrl(providerUrl).build();
        this.apiKey = apiKey;
        this.senderId = senderId;
        this.templateId = templateId;
    }

    @Override
    public SmsResult send(String phoneNumber, String message) {
        try {
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("numbers", phoneNumber);
            body.put("message", message);
            if (!senderId.isBlank()) {
                body.put("sender_id", senderId);
            }
            if (!templateId.isBlank()) {
                body.put("template_id", Integer.valueOf(templateId));
            }

            var response = client.post().contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .toEntity(Map.class);
            Object reference = response.getBody() == null ? null : response.getBody().get("id");
            return new SmsResult(true, reference == null ? null : reference.toString(), null);
        } catch (RestClientResponseException exception) {
            return new SmsResult(false, null, "SMS provider rejected the request: " + exception.getStatusCode());
        } catch (NumberFormatException exception) {
            return new SmsResult(false, null, "SMS template id must be numeric");
        } catch (org.springframework.web.client.RestClientException exception) {
            return new SmsResult(false, null, "SMS provider request failed");
        }
    }
}
