ALTER TABLE inventory_balance
    ADD CONSTRAINT inventory_balance_maximum_quantity_check CHECK (quantity <= 100000000);

CREATE TABLE inventory_movement (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES catalog_product (id),
    movement_type VARCHAR(30) NOT NULL,
    quantity INTEGER NOT NULL,
    delta INTEGER NOT NULL,
    previous_balance INTEGER NOT NULL,
    resulting_balance INTEGER NOT NULL,
    reason VARCHAR(500),
    reference VARCHAR(100),
    actor_id UUID NOT NULL REFERENCES app_user (id),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT inventory_movement_type_check CHECK (
        movement_type IN (
            'ENTRY',
            'SALE_OUT',
            'ADJUSTMENT_IN',
            'ADJUSTMENT_OUT',
            'DAMAGED',
            'EXPIRED',
            'SALE_REVERSAL'
        )
    ),
    CONSTRAINT inventory_movement_quantity_check CHECK (
        quantity > 0 AND quantity <= 1000000
    ),
    CONSTRAINT inventory_movement_delta_check CHECK (quantity = abs(delta)),
    CONSTRAINT inventory_movement_balance_check CHECK (
        previous_balance >= 0
        AND resulting_balance >= 0
        AND resulting_balance <= 100000000
        AND resulting_balance = previous_balance + delta
    ),
    CONSTRAINT inventory_movement_evidence_check CHECK (
        reason IS NOT NULL OR reference IS NOT NULL
    ),
    CONSTRAINT inventory_movement_reason_not_blank CHECK (
        reason IS NULL OR btrim(reason) <> ''
    ),
    CONSTRAINT inventory_movement_reference_not_blank CHECK (
        reference IS NULL OR btrim(reference) <> ''
    ),
    CONSTRAINT inventory_movement_direction_check CHECK (
        (movement_type IN ('ENTRY', 'ADJUSTMENT_IN', 'SALE_REVERSAL') AND delta > 0)
        OR (movement_type IN ('SALE_OUT', 'ADJUSTMENT_OUT', 'DAMAGED', 'EXPIRED') AND delta < 0)
    )
);

CREATE INDEX inventory_movement_product_time_idx
    ON inventory_movement (product_id, occurred_at DESC, id DESC);
CREATE INDEX inventory_movement_actor_time_idx
    ON inventory_movement (actor_id, occurred_at DESC);

CREATE FUNCTION prevent_inventory_movement_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'inventory movements are immutable';
END;
$$;

CREATE TRIGGER inventory_movement_immutable_trigger
BEFORE UPDATE OR DELETE ON inventory_movement
FOR EACH ROW
EXECUTE FUNCTION prevent_inventory_movement_mutation();
