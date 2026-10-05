package com.minimarket.pos.reporting.application;

import java.util.List;

public record AuditEventPage<T>(List<T> items, int page, int size, long totalElements) {

    public AuditEventPage {
        items = List.copyOf(items);
    }

    public long totalPages() {
        return totalElements == 0 ? 0 : (totalElements + size - 1) / size;
    }
}
