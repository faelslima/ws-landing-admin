package br.eti.logos.core.config;

import br.eti.logos.service.pagbank.WebhookProcessorService;
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
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQConsumer {

    private static final int MAX_RETRIES = 5;

    private final WebhookProcessorService webhookProcessorService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.landing.exchange}")
    private String exchange;

    @Value("${rabbitmq.landing.subscription.webhook.routing.key}")
    private String webhookRoutingKey;

    @RabbitListener(queues = "${rabbitmq.landing.subscription.webhook.queue}", ackMode = "MANUAL")
    public void processarWebhookMessage(Message message, Channel channel) throws IOException {
        try {
            var payload = new String(message.getBody(), StandardCharsets.UTF_8);
            webhookProcessorService.processarEventoAsync(payload);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("Erro ao processar mensagem webhook: {}", e.getMessage());
            handleRetry(message, channel);
        }
    }

    private void handleRetry(Message message, Channel channel) throws IOException {
        var headers = message.getMessageProperties().getHeaders();
        var retries = headers.containsKey("x-retries")
                ? (int) headers.get("x-retries")
                : 0;

        if (retries < MAX_RETRIES) {
            message.getMessageProperties().setHeader("x-retries", retries + 1);
            rabbitTemplate.convertAndSend(exchange, webhookRoutingKey, message);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            log.warn("Retry {}/{} para mensagem", retries + 1, MAX_RETRIES);
        } else {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            log.error("Mensagem enviada para DLQ após {} tentativas", MAX_RETRIES);
        }
    }
}
