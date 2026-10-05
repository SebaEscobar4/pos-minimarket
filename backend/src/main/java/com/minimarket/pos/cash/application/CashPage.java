package com.minimarket.pos.cash.application;

import java.util.List;

public record CashPage<T>(List<T> items, int page, int size, long totalElements) {

    public CashPage {
        items = List.copyOf(items);
    }

    public long totalPages() {
        return totalElements == 0 ? 0 : (totalElements + size - 1) / size;
    }
}
