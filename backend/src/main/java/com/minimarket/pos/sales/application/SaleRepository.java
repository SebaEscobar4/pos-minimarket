package com.minimarket.pos.sales.application;

import com.minimarket.pos.sales.domain.Sale;
import com.minimarket.pos.sales.domain.SaleCancellation;
import java.util.Optional;
import java.util.UUID;

public interface SaleRepository {

    void lockIdempotencyKey(UUID idempotencyKey);

    Optional<Sale> findByIdempotencyKey(UUID idempotencyKey);

    Optional<Sale> findById(UUID id);

    Optional<Sale> findByIdForUpdate(UUID id);

    long nextFolio();

    void save(Sale sale);

    void saveCancellation(SaleCancellation cancellation);

    boolean markVoided(UUID saleId);

    SalePage<Sale> findRecent(int page, int size);
}
