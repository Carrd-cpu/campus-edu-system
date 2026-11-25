package com.scaffold.edu;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 总评 = 平时 30% + 期末 70%。绩点按常见四分制分段。
 */
public final class GradeRules {

    private GradeRules() {
    }

    public static BigDecimal total(BigDecimal usual, BigDecimal exam) {
        BigDecimal u = usual == null ? BigDecimal.ZERO : usual;
        BigDecimal e = exam == null ? BigDecimal.ZERO : exam;
        return u.multiply(new BigDecimal("0.3"))
                .add(e.multiply(new BigDecimal("0.7")))
                .setScale(1, RoundingMode.HALF_UP);
    }

    public static BigDecimal point(BigDecimal total) {
        double score = total == null ? 0 : total.doubleValue();
        double point;
        if (score >= 90) {
            point = 4.0;
        } else if (score >= 85) {
            point = 3.7;
        } else if (score >= 82) {
            point = 3.3;
        } else if (score >= 78) {
            point = 3.0;
        } else if (score >= 75) {
            point = 2.7;
        } else if (score >= 72) {
            point = 2.3;
        } else if (score >= 68) {
            point = 2.0;
        } else if (score >= 64) {
            point = 1.5;
        } else if (score >= 60) {
            point = 1.0;
        } else {
            point = 0;
        }
        return BigDecimal.valueOf(point).setScale(1, RoundingMode.HALF_UP);
    }

    public static String band(BigDecimal total) {
        double score = total == null ? 0 : total.doubleValue();
        if (score >= 90) {
            return "优秀";
        }
        if (score >= 80) {
            return "良好";
        }
        if (score >= 70) {
            return "中等";
        }
        if (score >= 60) {
            return "及格";
        }
        return "不及格";
    }
}