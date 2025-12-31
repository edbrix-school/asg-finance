package com.asg.finance.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface CurrencyRateProjection {
    String getCurrencyCode();
    LocalDate getRateDate();
    BigDecimal getRate();
}
