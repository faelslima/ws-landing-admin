package br.eti.logos.service.email.impl;

import br.eti.logos.service.email.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${email.from}")
    private String fromEmail;

    @Value("${email.from.name}")
    private String fromName;

    private static final Map<String, String> SUBJECT_MAP = Map.of(
            "pt", "Bem-vindo ao i12! Sua igreja está pronta",
            "en", "Welcome to i12! Your church is ready",
            "es", "Bienvenido a i12! Tu iglesia está lista"
    );

    private static final Map<String, String> SUBJECT_FALHA_MAP = Map.of(
            "pt", "i12 — Problema com o pagamento. Tente novamente",
            "en", "i12 — Payment issue. Please try again",
            "es", "i12 — Problema con el pago. Inténtalo de nuevo"
    );

    private static final Map<String, String> SUBJECT_ESTORNO_MAP = Map.of(
            "pt", "i12 — Sua assinatura foi cancelada por estorno",
            "en", "i12 — Your subscription was canceled due to chargeback",
            "es", "i12 — Tu suscripción fue cancelada por contracargo"
    );

    private static final Map<String, String> SUBJECT_REATIVACAO_MAP = Map.of(
            "pt", "i12 — Sua assinatura foi reativada com sucesso!",
            "en", "i12 — Your subscription has been reactivated!",
            "es", "i12 — ¡Tu suscripción ha sido reactivada!"
    );

    @Override
    public void send(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email enviado com sucesso para: {}", maskEmail(to));
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Erro ao enviar email para {}: {}", maskEmail(to), e.getMessage());
        }
    }

    @Override
    public void enviarBoasVindas(String to, String nomeIgreja, String nomeResponsavel, String lang) {
        if (lang == null || lang.isBlank()) lang = "pt";

        var templatePath = resolveTemplatePath("welcome-church", lang);
        try {
            var resource = new ClassPathResource(templatePath);
            var html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            html = html.replace(":nomeIgreja", nomeIgreja)
                    .replace(":nomeResponsavel", nomeResponsavel);

            var subject = SUBJECT_MAP.getOrDefault(lang, SUBJECT_MAP.get("pt"));
            send(to, subject, html);
        } catch (IOException e) {
            log.error("Erro ao carregar template de boas-vindas ({}): {}", lang, e.getMessage());
        }
    }

    @Override
    public void enviarFalhaPagamento(String to, String nomeResponsavel, String planoNome, String retryUrl, String lang) {
        if (lang == null || lang.isBlank()) lang = "pt";

        var templatePath = resolveTemplatePath("payment-failed", lang);
        try {
            var resource = new ClassPathResource(templatePath);
            var html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            html = html.replace(":nomeResponsavel", nomeResponsavel)
                    .replace(":planoNome", planoNome)
                    .replace(":retryUrl", retryUrl);

            var subject = SUBJECT_FALHA_MAP.getOrDefault(lang, SUBJECT_FALHA_MAP.get("pt"));
            send(to, subject, html);
        } catch (IOException e) {
            log.error("Erro ao carregar template de falha de pagamento ({}): {}", lang, e.getMessage());
        }
    }

    @Override
    public void enviarCancelamentoEstorno(String to, String nomeResponsavel, String planoNome, String retryUrl, String lang) {
        if (lang == null || lang.isBlank()) lang = "pt";

        var templatePath = resolveTemplatePath("subscription-canceled-chargeback", lang);
        try {
            var resource = new ClassPathResource(templatePath);
            var html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            html = html.replace(":nomeResponsavel", nomeResponsavel)
                    .replace(":planoNome", planoNome)
                    .replace(":retryUrl", retryUrl);

            var subject = SUBJECT_ESTORNO_MAP.getOrDefault(lang, SUBJECT_ESTORNO_MAP.get("pt"));
            send(to, subject, html);
        } catch (IOException e) {
            log.error("Erro ao carregar template de cancelamento por estorno ({}): {}", lang, e.getMessage());
        }
    }

    @Override
    public void enviarReativacao(String to, String nomeResponsavel, String planoNome, String lang) {
        if (lang == null || lang.isBlank()) lang = "pt";

        var templatePath = resolveTemplatePath("subscription-reactivated", lang);
        try {
            var resource = new ClassPathResource(templatePath);
            var html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            html = html.replace(":nomeResponsavel", nomeResponsavel)
                    .replace(":planoNome", planoNome);

            var subject = SUBJECT_REATIVACAO_MAP.getOrDefault(lang, SUBJECT_REATIVACAO_MAP.get("pt"));
            send(to, subject, html);
        } catch (IOException e) {
            log.error("Erro ao carregar template de reativação ({}): {}", lang, e.getMessage());
        }
    }

    private String resolveTemplatePath(String baseName, String lang) {
        var localizedPath = "templates/" + baseName + "_" + lang + ".html";
        if (new ClassPathResource(localizedPath).exists()) {
            return localizedPath;
        }
        return "templates/" + baseName + ".html";
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        var parts = email.split("@");
        var local = parts[0];
        var masked = local.length() > 2
                ? local.substring(0, 2) + "***"
                : "***";
        return masked + "@" + parts[1];
    }
}
