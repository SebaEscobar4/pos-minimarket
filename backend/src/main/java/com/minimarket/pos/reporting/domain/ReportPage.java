package com.minimarket.pos.reporting.domain;

import java.util.List;

public record ReportPage<T>(List<T> items, int page, int size, long totalElements) {

    public long totalPages() {
        return totalElements == 0 ? 0 : (totalElements + size - 1) / size;
    }
}
