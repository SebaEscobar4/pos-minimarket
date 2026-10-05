CREATE SEQUENCE sale_folio_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE sale (
    id UUID PRIMARY KEY,
    folio BIGINT NOT NULL DEFAULT nextval('sale_folio_sequence'),
    cash_session_id UUID NOT NULL REFERENCES cash_session (id),
    actor_id UUID NOT NULL REFERENCES app_user (id),
    confirmed_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    total NUMERIC(19, 0) NOT NULL,
    idempotency_key UUID NOT NULL,
    request_hash CHAR(64) NOT NULL,
    CONSTRAINT sale_folio_unique UNIQUE (folio),
    CONSTRAINT sale_idempotency_key_unique UNIQUE (idempotency_key),
    CONSTRAINT sale_status_check CHECK (status IN ('CONFIRMED', 'VOIDED')),
    CONSTRAINT sale_total_check CHECK (
        total >= 0 AND total <= 9007199254740991
    ),
    CONSTRAINT sale_request_hash_check CHECK (request_hash ~ '^[0-9a-f]{64}$')
);

CREATE TABLE sale_line (
    id UUID PRIMARY KEY,
    sale_id UUID NOT NULL REFERENCES sale (id),
    line_number INTEGER NOT NULL,
    product_id UUID NOT NULL REFERENCES catalog_product (id),
    product_name VARCHAR(150) NOT NULL,
    product_code VARCHAR(64),
    quantity INTEGER NOT NULL,
    unit_sale_price NUMERIC(19, 0) NOT NULL,
    unit_estimated_cost NUMERIC(12, 2) NOT NULL,
    subtotal NUMERIC(19, 0) NOT NULL,
    CONSTRAINT sale_line_number_unique UNIQUE (sale_id, line_number),
    CONSTRAINT sale_line_product_unique UNIQUE (sale_id, product_id),
    CONSTRAINT sale_line_name_not_blank CHECK (btrim(product_name) <> ''),
    CONSTRAINT sale_line_quantity_check CHECK (
        quantity > 0 AND quantity <= 1000000
    ),
    CONSTRAINT sale_line_unit_price_check CHECK (
        unit_sale_price >= 0 AND unit_sale_price <= 9007199254740991
    ),
    CONSTRAINT sale_line_cost_check CHECK (
        unit_estimated_cost >= 0 AND unit_estimated_cost <= 9999999999.99
    ),
    CONSTRAINT sale_line_subtotal_check CHECK (
        subtotal = unit_sale_price * quantity
        AND subtotal <= 9007199254740991
    )
);

CREATE TABLE payment (
    id UUID PRIMARY KEY,
    sale_id UUID NOT NULL REFERENCES sale (id),
    method VARCHAR(20) NOT NULL,
    amount NUMERIC(19, 0) NOT NULL,
    cash_received NUMERIC(19, 0),
    change_amount NUMERIC(19, 0),
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT payment_sale_unique UNIQUE (sale_id),
    CONSTRAINT payment_method_check CHECK (method IN ('CASH', 'CARD', 'TRANSFER')),
    CONSTRAINT payment_amount_check CHECK (
        amount >= 0 AND amount <= 9007199254740991
    ),
    CONSTRAINT payment_cash_fields_check CHECK (
        (
            method = 'CASH'
            AND cash_received IS NOT NULL
            AND change_amount IS NOT NULL
            AND cash_received >= amount
            AND change_amount = cash_received - amount
        )
        OR (
            method IN ('CARD', 'TRANSFER')
            AND cash_received IS NULL
            AND change_amount IS NULL
        )
    )
);

CREATE INDEX sale_confirmed_at_idx ON sale (confirmed_at DESC, id DESC);
CREATE INDEX sale_cash_session_idx ON sale (cash_session_id, confirmed_at DESC);
CREATE INDEX sale_actor_idx ON sale (actor_id, confirmed_at DESC);
CREATE INDEX sale_line_product_idx ON sale_line (product_id, sale_id);

CREATE FUNCTION prevent_sale_history_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'confirmed sale history is immutable';
END;
$$;

CREATE TRIGGER sale_history_immutable_trigger
BEFORE UPDATE OR DELETE ON sale
FOR EACH ROW
EXECUTE FUNCTION prevent_sale_history_mutation();

CREATE TRIGGER sale_line_history_immutable_trigger
BEFORE UPDATE OR DELETE ON sale_line
FOR EACH ROW
EXECUTE FUNCTION prevent_sale_history_mutation();

CREATE TRIGGER payment_history_immutable_trigger
BEFORE UPDATE OR DELETE ON payment
FOR EACH ROW
EXECUTE FUNCTION prevent_sale_history_mutation();
