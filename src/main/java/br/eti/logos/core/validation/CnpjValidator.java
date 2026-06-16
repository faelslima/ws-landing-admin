package br.eti.logos.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Aceita CNPJ numérico (14 dígitos) ou alfanumérico (formato XX.XXX.XXX/XXXX-XX).
 * Valida dígitos verificadores apenas quando todos os 12 primeiros chars forem dígitos.
 */
public class CnpjValidator implements ConstraintValidator<ValidCnpj, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true; // @NotBlank cuida disso

        var stripped = strip(value);

        if (stripped.length() != 14) return false;
        if (isAllSameChar(stripped)) return false;

        // Alfanumérico: só verifica tamanho e charset (letras maiúsculas + dígitos)
        if (!stripped.matches("\\d{14}")) {
            return stripped.matches("[A-Z0-9]{14}");
        }

        // Numérico: valida dígitos verificadores
        return validarDigitosVerificadores(stripped);
    }

    /** Remove pontos, barra e hífen; normaliza para maiúsculo. */
    public static String strip(String cnpj) {
        if (cnpj == null) return null;
        return cnpj.toUpperCase().replaceAll("[.\\-/]", "");
    }

    private boolean isAllSameChar(String s) {
        char first = s.charAt(0);
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) != first) return false;
        }
        return true;
    }

    private boolean validarDigitosVerificadores(String cnpj) {
        int[] weights1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] weights2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

        int d1 = calcDigit(cnpj, weights1);
        int d2 = calcDigit(cnpj, weights2);

        return cnpj.charAt(12) == ('0' + d1) && cnpj.charAt(13) == ('0' + d2);
    }

    private int calcDigit(String cnpj, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += Character.getNumericValue(cnpj.charAt(i)) * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
