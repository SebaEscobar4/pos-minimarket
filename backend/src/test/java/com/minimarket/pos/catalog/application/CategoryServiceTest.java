package com.minimarket.pos.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.minimarket.pos.catalog.domain.Category;
import com.minimarket.pos.catalog.domain.CategoryStatus;
import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-04T04:00:00Z");

    @Mock
    private CategoryRepository repository;

    private CategoryService service;

    @BeforeEach
    void setUp() {
        service = new CategoryService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsAnActiveCategoryAndRecordsTheActor() {
        UUID actorId = UUID.randomUUID();

        Category created = service.create("  Abarrotes  ", actorId);

        assertThat(created.name()).isEqualTo("Abarrotes");
        assertThat(created.status()).isEqualTo(CategoryStatus.ACTIVE);
        assertThat(created.createdAt()).isEqualTo(NOW);
        ArgumentCaptor<Category> category = ArgumentCaptor.forClass(Category.class);
        verify(repository).create(category.capture(), org.mockito.ArgumentMatchers.eq(actorId));
        assertThat(category.getValue()).isEqualTo(created);
    }

    @Test
    void listsCategoriesUsingTheRequestedInactiveFilter() {
        when(repository.findAll(true)).thenReturn(List.of());

        assertThat(service.list(true)).isEmpty();

        verify(repository).findAll(true);
    }

    @Test
    void renamesAnExistingCategory() {
        UUID id = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Category updated = category(id, "Lácteos", CategoryStatus.ACTIVE);
        when(repository.updateName(id, "Lácteos", actorId)).thenReturn(true);
        when(repository.findById(id)).thenReturn(Optional.of(updated));

        assertThat(service.rename(id, " Lácteos ", actorId)).isEqualTo(updated);
    }

    @Test
    void changesTheStatusOfAnExistingCategory() {
        UUID id = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Category updated = category(id, "Panadería", CategoryStatus.INACTIVE);
        when(repository.updateStatus(id, CategoryStatus.INACTIVE, actorId)).thenReturn(true);
        when(repository.findById(id)).thenReturn(Optional.of(updated));

        assertThat(service.changeStatus(id, CategoryStatus.INACTIVE, actorId)).isEqualTo(updated);
    }

    @Test
    void rejectsMissingStatusAndUnknownCategories() {
        UUID id = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(repository.updateName(id, "Ausente", actorId)).thenReturn(false);

        assertProblem(() -> service.changeStatus(id, null, actorId), ProblemType.VALIDATION);
        assertProblem(() -> service.rename(id, "Ausente", actorId), ProblemType.NOT_FOUND);
    }

    private Category category(UUID id, String name, CategoryStatus status) {
        return new Category(id, name, status, NOW, NOW);
    }

    private void assertProblem(Runnable action, ProblemType type) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).problemType())
                .isEqualTo(type);
    }
}
