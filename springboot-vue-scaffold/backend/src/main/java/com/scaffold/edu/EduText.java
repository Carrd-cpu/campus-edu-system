package com.scaffold.edu;

import cn.hutool.core.util.StrUtil;
import com.scaffold.common.BusinessException;

import java.math.BigDecimal;

/** 教务模块校验小工具 */
public final class EduText {
    private EduText() {
    }

    public static void requireText(String value, String message) {
        if (StrUtil.isBlank(value)) {
            throw new BusinessException(message);
        }
    }

    public static void checkScoreRange(BigDecimal value, String label) {
        if (value == null) {
            throw new BusinessException(label + "不能为空");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException(label + "须在 0 到 100 之间");
        }
    }
}