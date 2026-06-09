package br.eti.logos.core.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper;
    private final br.eti.logos.service.i18n.MessageTranslationService translationService;

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurity(SecurityException e) {
        log.warn("Acesso negado: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "status", 401,
                "error", "Unauthorized",
                "message", e.getMessage() != null ? e.getMessage() : "Acesso não autorizado",
                "timestamp", OffsetDateTime.now().toString()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", e.getMessage(),
                "timestamp", OffsetDateTime.now().toString()
        ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "status", 409,
                "error", "Conflict",
                "message", e.getMessage(),
                "timestamp", OffsetDateTime.now().toString()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        var errors = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", 400,
                "error", "Validation Error",
                "message", errors,
                "timestamp", OffsetDateTime.now().toString()
        ));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, Object>> handleFeignException(FeignException e) {
        log.error("Erro ao comunicar com PagBank: status={}", e.status());

        String userMessage = "Erro ao comunicar com PagBank";
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        try {
            // Tentar extrair mensagem amigável do response do PagBank
            String body = e.contentUTF8();
            if (body != null && !body.isEmpty()) {
                JsonNode json = objectMapper.readTree(body);

                // Formato: {"error_messages":[{"error":"...","description":"..."}]}
                if (json.has("error_messages") && json.get("error_messages").isArray()) {
                    var errorMessages = json.get("error_messages");
                    if (errorMessages.size() > 0) {
                        var firstError = errorMessages.get(0);
                        var description = firstError.path("description").asText();
                        var error = firstError.path("error").asText();

                        if (!description.isEmpty()) {
                            // Traduz descrição para português
                            userMessage = translationService.translatePagBankMessage(description);
                        } else if (!error.isEmpty()) {
                            // Traduz código de erro
                            userMessage = translationService.translateErrorCode(error);
                        }
                    }
                }
            }

            // Mapear status HTTP
            if (e.status() == 400) {
                status = HttpStatus.BAD_REQUEST;
            } else if (e.status() == 401) {
                status = HttpStatus.UNAUTHORIZED;
                userMessage = "Token PagBank inválido ou expirado";
            } else if (e.status() == 404) {
                status = HttpStatus.NOT_FOUND;
                userMessage = "Recurso não encontrado no PagBank";
            } else if (e.status() == 409) {
                status = HttpStatus.CONFLICT;
                // Mantém a mensagem extraída do body (ex: customer já existe)
            } else if (e.status() >= 500) {
                status = HttpStatus.BAD_GATEWAY;
                userMessage = "PagBank temporariamente indisponível. Tente novamente em alguns minutos.";
            }

        } catch (Exception ex) {
            log.warn("Erro ao parsear response do PagBank: {}", ex.getMessage());
        }

        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", userMessage,
                "timestamp", OffsetDateTime.now().toString()
        ));
    }

    private String formatErrorCode(String errorCode) {
        return switch (errorCode) {
            case "invalid_parameter" -> "Parâmetro inválido enviado ao PagBank";
            case "duplicate_entry" -> "Registro duplicado no PagBank";
            case "insufficient_funds" -> "Saldo insuficiente";
            case "payment_declined" -> "Pagamento recusado";
            case "invalid_card" -> "Cartão inválido";
            default -> "Erro: " + errorCode.replace("_", " ");
        };
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception e) {
        log.error("Erro não tratado: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", 500,
                "error", "Internal Server Error",
                "message", "Erro inesperado. Tente novamente.",
                "timestamp", OffsetDateTime.now().toString()
        ));
    }
}
