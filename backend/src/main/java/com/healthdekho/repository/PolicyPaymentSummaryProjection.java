package com.healthdekho.repository;

import java.math.BigDecimal;

public interface PolicyPaymentSummaryProjection {

    Long getPolicyId();

    BigDecimal getPremiumPaid();

    Long getIncompleteInstallments();

    Long getOverdueInstallments();

    Long getInstallmentsWithPayment();
}
