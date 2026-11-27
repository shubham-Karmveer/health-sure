package com.healthdekho.repository;

import java.math.BigDecimal;
import java.util.UUID;

public interface PolicyPaymentSummaryProjection {

    UUID getPolicyId();

    BigDecimal getPremiumPaid();

    Long getIncompleteInstallments();

    Long getOverdueInstallments();

    Long getInstallmentsWithPayment();
}
