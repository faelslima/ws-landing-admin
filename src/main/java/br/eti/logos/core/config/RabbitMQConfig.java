package br.eti.logos.core.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.landing.exchange}")
    private String exchange;

    @Value("${rabbitmq.landing.subscription.webhook.queue}")
    private String webhookQueue;

    @Value("${rabbitmq.landing.subscription.webhook.routing.key}")
    private String webhookRoutingKey;

    @Value("${rabbitmq.landing.onboarding.queue}")
    private String onboardingQueue;

    @Value("${rabbitmq.landing.onboarding.routing.key}")
    private String onboardingRoutingKey;

    @Value("${rabbitmq.landing.alert.queue}")
    private String alertQueue;

    @Value("${rabbitmq.landing.alert.routing.key}")
    private String alertRoutingKey;

    @Bean
    public DirectExchange landingExchange() {
        return new DirectExchange(exchange);
    }

    @Bean
    public Queue webhookQueue() {
        return QueueBuilder.durable(webhookQueue)
                .withArgument("x-dead-letter-exchange", exchange + ".dlq")
                .withArgument("x-dead-letter-routing-key", webhookRoutingKey + ".dlq")
                .build();
    }

    @Bean
    public Queue onboardingQueue() {
        return QueueBuilder.durable(onboardingQueue).build();
    }

    @Bean
    public Queue alertQueue() {
        return QueueBuilder.durable(alertQueue).build();
    }

    @Bean
    public Binding webhookBinding() {
        return BindingBuilder.bind(webhookQueue()).to(landingExchange()).with(webhookRoutingKey);
    }

    @Bean
    public Binding onboardingBinding() {
        return BindingBuilder.bind(onboardingQueue()).to(landingExchange()).with(onboardingRoutingKey);
    }

    @Bean
    public Binding alertBinding() {
        return BindingBuilder.bind(alertQueue()).to(landingExchange()).with(alertRoutingKey);
    }

    // DLQ
    @Bean
    public DirectExchange dlqExchange() {
        return new DirectExchange(exchange + ".dlq");
    }

    @Bean
    public Queue dlqWebhookQueue() {
        return QueueBuilder.durable(webhookQueue + ".dlq").build();
    }

    @Bean
    public Binding dlqWebhookBinding() {
        return BindingBuilder.bind(dlqWebhookQueue()).to(dlqExchange()).with(webhookRoutingKey + ".dlq");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        var template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
