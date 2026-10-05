package com.minimarket.pos.sales.application;

import java.util.List;

public record SalePage<T>(List<T> items, int page, int size, long totalElements) {

    public long totalPages() {
        return totalElements == 0 ? 0 : (totalElements + size - 1) / size;
    }
}
