package br.eti.logos.core.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyUtil {

    private MoneyUtil() {}

    public static Integer reaisParaCentavos(BigDecimal reais) {
        if (reais == null) return 0;
        return reais.multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue();
    }

    public static BigDecimal centavosParaReais(Integer centavos) {
        if (centavos == null || centavos == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(centavos).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal centavosParaReais(long centavos) {
        return BigDecimal.valueOf(centavos).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}