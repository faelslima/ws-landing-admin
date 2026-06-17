package br.eti.logos.core.config;

import br.eti.logos.dto.saga.OnboardingCompletedEvent;
import br.eti.logos.service.email.EmailService;
import br.eti.logos.service.pagbank.WebhookProcessorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQConsumer {

    private static final int MAX_RETRIES = 5;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WebhookProcessorService webhookProcessorService;
    private final EmailService emailService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.landing.exchange}")
    private String exchange;

    @Value("${rabbitmq.landing.subscription.webhook.routing.key}")
    private String webhookRoutingKey;

    @Value("${rabbitmq.landing.saga.completed.routing.key}")
    private String sagaCompletedRoutingKey;

    @RabbitListener(queues = "${rabbitmq.landing.subscription.webhook.queue}", ackMode = "MANUAL")
    public void processarWebhookMessage(Message message, Channel channel) throws IOException {
        try {
            var payload = new String(message.getBody(), StandardCharsets.UTF_8);
            webhookProcessorService.processarEventoAsync(payload);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("Erro ao processar mensagem webhook: {}", e.getMessage());
            handleRetry(message, channel, webhookRoutingKey);
        }
    }

    @RabbitListener(queues = "${rabbitmq.landing.saga.completed.queue}", ackMode = "MANUAL")
    public void processarOnboardingCompleted(Message message, Channel channel) throws IOException {
        try {
            var payload = new String(message.getBody(), StandardCharsets.UTF_8);
            var event = OBJECT_MAPPER.readValue(payload, OnboardingCompletedEvent.class);
            log.info("Onboarding concluído recebido da saga para igreja: {} ({})", event.getNomeIgreja(), event.getIgrejaId());
            emailService.enviarBoasVindas(
                    event.getEmail(),
                    event.getNomeIgreja(),
                    event.getNomeResponsavel(),
                    event.getLang() != null ? event.getLang() : "pt"
            );
            log.info("E-mail de boas-vindas enviado para: {}", event.getEmail());
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("Erro ao processar onboarding.completed: {}", e.getMessage());
            handleRetry(message, channel, sagaCompletedRoutingKey);
        }
    }

    private void handleRetry(Message message, Channel channel, String routingKey) throws IOException {
        var headers = message.getMessageProperties().getHeaders();
        var retries = headers.containsKey("x-retries")
                ? (int) headers.get("x-retries")
                : 0;

        if (retries < MAX_RETRIES) {
            message.getMessageProperties().setHeader("x-retries", retries + 1);
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            log.warn("Retry {}/{} para mensagem (routing key: {})", retries + 1, MAX_RETRIES, routingKey);
        } else {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            log.error("Mensagem enviada para DLQ após {} tentativas (routing key: {})", MAX_RETRIES, routingKey);
        }
    }
}
