package com.minimarket.pos.sales.application;

import com.minimarket.pos.sales.domain.Sale;

public record SaleConfirmation(Sale sale, boolean created) {}
