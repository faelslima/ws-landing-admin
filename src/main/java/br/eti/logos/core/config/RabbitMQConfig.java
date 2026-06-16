package br.eti.logos.core.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableRabbit
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

    @Value("${rabbitmq.landing.saga.provisioning.queue}")
    private String sagaProvisioningQueue;

    @Value("${rabbitmq.landing.saga.provisioning.routing.key}")
    private String sagaProvisioningRoutingKey;

    @Value("${rabbitmq.landing.saga.completed.queue}")
    private String sagaCompletedQueue;

    @Value("${rabbitmq.landing.saga.completed.routing.key}")
    private String sagaCompletedRoutingKey;

    @Value("${rabbitmq.landing.saga.license.suspension.queue}")
    private String sagaLicenseSuspensionQueue;

    @Value("${rabbitmq.landing.saga.license.suspension.routing.key}")
    private String sagaLicenseSuspensionRoutingKey;

    @Value("${rabbitmq.landing.saga.license.reactivation.queue}")
    private String sagaLicenseReactivationQueue;

    @Value("${rabbitmq.landing.saga.license.reactivation.routing.key}")
    private String sagaLicenseReactivationRoutingKey;

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

    // Saga: onboarding.provisioning (landing → security)
    @Bean
    public Queue sagaProvisioningQueue() {
        return QueueBuilder.durable(sagaProvisioningQueue)
                .withArgument("x-dead-letter-exchange", exchange + ".dlq")
                .withArgument("x-dead-letter-routing-key", sagaProvisioningRoutingKey + ".dlq")
                .build();
    }

    @Bean
    public Binding sagaProvisioningBinding() {
        return BindingBuilder.bind(sagaProvisioningQueue()).to(landingExchange()).with(sagaProvisioningRoutingKey);
    }

    // Saga: onboarding.completed (security → landing)
    @Bean
    public Queue sagaCompletedQueue() {
        return QueueBuilder.durable(sagaCompletedQueue)
                .withArgument("x-dead-letter-exchange", exchange + ".dlq")
                .withArgument("x-dead-letter-routing-key", sagaCompletedRoutingKey + ".dlq")
                .build();
    }

    @Bean
    public Binding sagaCompletedBinding() {
        return BindingBuilder.bind(sagaCompletedQueue()).to(landingExchange()).with(sagaCompletedRoutingKey);
    }

    // Saga: license.suspension (landing → security)
    @Bean
    public Queue sagaLicenseSuspensionQueue() {
        return QueueBuilder.durable(sagaLicenseSuspensionQueue)
                .withArgument("x-dead-letter-exchange", exchange + ".dlq")
                .withArgument("x-dead-letter-routing-key", sagaLicenseSuspensionRoutingKey + ".dlq")
                .build();
    }

    @Bean
    public Binding sagaLicenseSuspensionBinding() {
        return BindingBuilder.bind(sagaLicenseSuspensionQueue()).to(landingExchange()).with(sagaLicenseSuspensionRoutingKey);
    }

    // Saga: license.reactivation (landing → security)
    @Bean
    public Queue sagaLicenseReactivationQueue() {
        return QueueBuilder.durable(sagaLicenseReactivationQueue)
                .withArgument("x-dead-letter-exchange", exchange + ".dlq")
                .withArgument("x-dead-letter-routing-key", sagaLicenseReactivationRoutingKey + ".dlq")
                .build();
    }

    @Bean
    public Binding sagaLicenseReactivationBinding() {
        return BindingBuilder.bind(sagaLicenseReactivationQueue()).to(landingExchange()).with(sagaLicenseReactivationRoutingKey);
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
    public Queue dlqSagaProvisioningQueue() {
        return QueueBuilder.durable(sagaProvisioningQueue + ".dlq").build();
    }

    @Bean
    public Binding dlqSagaProvisioningBinding() {
        return BindingBuilder.bind(dlqSagaProvisioningQueue()).to(dlqExchange()).with(sagaProvisioningRoutingKey + ".dlq");
    }

    @Bean
    public Queue dlqSagaCompletedQueue() {
        return QueueBuilder.durable(sagaCompletedQueue + ".dlq").build();
    }

    @Bean
    public Binding dlqSagaCompletedBinding() {
        return BindingBuilder.bind(dlqSagaCompletedQueue()).to(dlqExchange()).with(sagaCompletedRoutingKey + ".dlq");
    }

    @Bean
    public Queue dlqSagaLicenseSuspensionQueue() {
        return QueueBuilder.durable(sagaLicenseSuspensionQueue + ".dlq").build();
    }

    @Bean
    public Binding dlqSagaLicenseSuspensionBinding() {
        return BindingBuilder.bind(dlqSagaLicenseSuspensionQueue()).to(dlqExchange()).with(sagaLicenseSuspensionRoutingKey + ".dlq");
    }

    @Bean
    public Queue dlqSagaLicenseReactivationQueue() {
        return QueueBuilder.durable(sagaLicenseReactivationQueue + ".dlq").build();
    }

    @Bean
    public Binding dlqSagaLicenseReactivationBinding() {
        return BindingBuilder.bind(dlqSagaLicenseReactivationQueue()).to(dlqExchange()).with(sagaLicenseReactivationRoutingKey + ".dlq");
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
