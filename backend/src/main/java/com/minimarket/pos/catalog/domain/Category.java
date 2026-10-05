package com.minimarket.pos.catalog.domain;

import java.time.Instant;
import java.util.UUID;

public record Category(
        UUID id, String name, CategoryStatus status, Instant createdAt, Instant updatedAt) {}
