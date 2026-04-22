package com.charlie2code.bravotechnicalassessment.infrastructure.webhook;

import com.charlie2code.bravotechnicalassessment.domain.entity.CreditApplication;
import com.charlie2code.bravotechnicalassessment.domain.port.WebhookNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpWebhookNotifier implements WebhookNotifier {

    private static final Logger log = LoggerFactory.getLogger(HttpWebhookNotifier.class);

    private final boolean enabled;
    private final RestClient restClient;

    public HttpWebhookNotifier(
            RestClient.Builder builder,
            @Value("${webhook.url:}") String webhookUrl) {
        this.enabled = !webhookUrl.isBlank();
        this.restClient = enabled ? builder.baseUrl(webhookUrl).build() : builder.build();
    }

    @Override
    public void notify(CreditApplication application) {
        if (!enabled) return;

        try {
            restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(WebhookPayload.from(application))
                    .retrieve()
                    .toBodilessEntity();

            log.info("Webhook sent for application {} — status: {}", application.getId(), application.getStatus());
        } catch (Exception e) {
            log.warn("Webhook notification failed for application {}: {}", application.getId(), e.getMessage());
        }
    }
}
