package br.eti.logos.service.i18n;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class MessageTranslationService {

    private static final Map<String, String> PAGBANK_MESSAGES_PT = new HashMap<>();

    static {
        // Customer errors
        PAGBANK_MESSAGES_PT.put(
            "The customer cannot be created, as there is already a customer registered with the informed tax_ID. Check that the data is correct and try again.",
            "Já existe um cadastro com este CPF/CNPJ. Verifique se os dados estão corretos e tente novamente."
        );

        PAGBANK_MESSAGES_PT.put(
            "Invalid tax_id",
            "CPF/CNPJ inválido"
        );

        // Card errors
        PAGBANK_MESSAGES_PT.put(
            "Invalid card number",
            "Número do cartão inválido"
        );

        PAGBANK_MESSAGES_PT.put(
            "Card expired",
            "Cartão vencido"
        );

        PAGBANK_MESSAGES_PT.put(
            "Invalid security code",
            "Código de segurança (CVV) inválido"
        );

        PAGBANK_MESSAGES_PT.put(
            "Card declined",
            "Cartão recusado pela operadora"
        );

        PAGBANK_MESSAGES_PT.put(
            "Insufficient funds",
            "Saldo insuficiente"
        );

        // Generic errors
        PAGBANK_MESSAGES_PT.put(
            "Invalid parameter",
            "Parâmetro inválido"
        );

        PAGBANK_MESSAGES_PT.put(
            "Resource not found",
            "Recurso não encontrado"
        );

        PAGBANK_MESSAGES_PT.put(
            "Unauthorized",
            "Não autorizado"
        );
    }

    /**
     * Traduz mensagem do PagBank para português
     */
    public String translatePagBankMessage(String originalMessage) {
        if (originalMessage == null || originalMessage.isEmpty()) {
            return "Erro ao processar solicitação";
        }

        // Busca tradução exata
        String translated = PAGBANK_MESSAGES_PT.get(originalMessage);
        if (translated != null) {
            return translated;
        }

        // Busca por correspondência parcial (case-insensitive)
        for (Map.Entry<String, String> entry : PAGBANK_MESSAGES_PT.entrySet()) {
            if (originalMessage.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }

        // Se contém keywords conhecidas, tenta tradução contextual
        String lower = originalMessage.toLowerCase();

        if (lower.contains("customer") && lower.contains("already") && lower.contains("registered")) {
            return "Cliente já cadastrado com estes dados";
        }

        if (lower.contains("tax_id") || lower.contains("cpf") || lower.contains("cnpj")) {
            return "Erro no CPF/CNPJ informado";
        }

        if (lower.contains("card") && lower.contains("declined")) {
            return "Cartão recusado. Verifique os dados ou tente outro cartão";
        }

        if (lower.contains("insufficient") && lower.contains("funds")) {
            return "Saldo insuficiente para completar a transação";
        }

        if (lower.contains("invalid") && lower.contains("card")) {
            return "Dados do cartão inválidos";
        }

        // Retorna mensagem original se não encontrar tradução
        return originalMessage;
    }

    /**
     * Traduz código de erro do PagBank
     */
    public String translateErrorCode(String errorCode) {
        if (errorCode == null || errorCode.isEmpty()) {
            return "Erro desconhecido";
        }

        return switch (errorCode.toLowerCase()) {
            case "invalid_parameter" -> "Parâmetro inválido";
            case "duplicate_entry" -> "Registro duplicado";
            case "insufficient_funds" -> "Saldo insuficiente";
            case "payment_declined" -> "Pagamento recusado";
            case "invalid_card" -> "Cartão inválido";
            case "card_expired" -> "Cartão vencido";
            case "invalid_security_code" -> "CVV inválido";
            case "unauthorized" -> "Não autorizado";
            case "not_found" -> "Não encontrado";
            case "conflict" -> "Conflito nos dados";
            default -> formatErrorCode(errorCode);
        };
    }

    private String formatErrorCode(String errorCode) {
        // Converte "invalid_parameter" -> "Invalid Parameter"
        String[] parts = errorCode.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (!formatted.isEmpty()) {
                formatted.append(" ");
            }
            formatted.append(part.substring(0, 1).toUpperCase())
                     .append(part.substring(1).toLowerCase());
        }
        return formatted.toString();
    }
}
