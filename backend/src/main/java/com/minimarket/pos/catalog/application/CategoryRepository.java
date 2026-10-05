package com.minimarket.pos.catalog.application;

import com.minimarket.pos.catalog.domain.Category;
import com.minimarket.pos.catalog.domain.CategoryStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {

    void create(Category category, UUID actorId);

    Optional<Category> findById(UUID id);

    List<Category> findAll(boolean includeInactive);

    boolean updateName(UUID id, String name, UUID actorId);

    boolean updateStatus(UUID id, CategoryStatus status, UUID actorId);
}
