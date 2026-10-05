package com.minimarket.pos.sales.application;

import com.minimarket.pos.sales.domain.PaymentMethod;

public record CancelSaleCommand(PaymentMethod refundMethod, String reason) {}
