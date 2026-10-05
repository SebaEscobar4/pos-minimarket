package com.minimarket.pos.catalog.application;

import com.minimarket.pos.catalog.domain.Category;
import com.minimarket.pos.catalog.domain.CategoryInputRules;
import com.minimarket.pos.catalog.domain.CategoryStatus;
import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository repository;
    private final Clock clock;

    @Autowired
    public CategoryService(CategoryRepository repository) {
        this(repository, Clock.systemUTC());
    }

    CategoryService(CategoryRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Category create(String name, UUID actorId) {
        String normalizedName = CategoryInputRules.normalizeName(name);
        Instant now = clock.instant();
        Category category =
                new Category(UUID.randomUUID(), normalizedName, CategoryStatus.ACTIVE, now, now);
        repository.create(category, actorId);
        return category;
    }

    @Transactional(readOnly = true)
    public List<Category> list(boolean includeInactive) {
        return repository.findAll(includeInactive);
    }

    @Transactional
    public Category rename(UUID id, String name, UUID actorId) {
        String normalizedName = CategoryInputRules.normalizeName(name);
        if (!repository.updateName(id, normalizedName, actorId)) {
            throw categoryNotFound();
        }
        return get(id);
    }

    @Transactional
    public Category changeStatus(UUID id, CategoryStatus status, UUID actorId) {
        if (status == null) {
            throw new ApplicationException(ProblemType.VALIDATION, "El estado de la categoría es obligatorio.");
        }
        if (!repository.updateStatus(id, status, actorId)) {
            throw categoryNotFound();
        }
        return get(id);
    }

    private Category get(UUID id) {
        return repository.findById(id).orElseThrow(CategoryService::categoryNotFound);
    }

    private static ApplicationException categoryNotFound() {
        return new ApplicationException(ProblemType.NOT_FOUND, "La categoría solicitada no existe.");
    }
}
