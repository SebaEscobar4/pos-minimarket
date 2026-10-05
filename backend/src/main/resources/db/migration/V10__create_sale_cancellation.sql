CREATE TABLE sale_cancellation (
    id UUID PRIMARY KEY,
    sale_id UUID NOT NULL REFERENCES sale (id),
    refund_method VARCHAR(20) NOT NULL,
    amount NUMERIC(19, 0) NOT NULL,
    cash_payable NUMERIC(19, 0),
    rounding_adjustment NUMERIC(19, 0),
    reason VARCHAR(500) NOT NULL,
    actor_id UUID NOT NULL REFERENCES app_user (id),
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT sale_cancellation_sale_unique UNIQUE (sale_id),
    CONSTRAINT sale_cancellation_method_check CHECK (
        refund_method IN ('CASH', 'CARD', 'TRANSFER')
    ),
    CONSTRAINT sale_cancellation_amount_check CHECK (
        amount >= 0 AND amount <= 9007199254740991
    ),
    CONSTRAINT sale_cancellation_reason_check CHECK (
        btrim(reason) <> '' AND char_length(reason) <= 500
    ),
    CONSTRAINT sale_cancellation_cash_fields_check CHECK (
        (
            refund_method = 'CASH'
            AND cash_payable IS NOT NULL
            AND rounding_adjustment IS NOT NULL
            AND cash_payable = amount + rounding_adjustment
            AND cash_payable >= 0
            AND cash_payable <= 9007199254740991
            AND rounding_adjustment = CASE
                WHEN MOD(amount, 10) BETWEEN 1 AND 5 THEN -MOD(amount, 10)
                WHEN MOD(amount, 10) BETWEEN 6 AND 9 THEN 10 - MOD(amount, 10)
                ELSE 0
            END
        )
        OR (
            refund_method IN ('CARD', 'TRANSFER')
            AND cash_payable IS NULL
            AND rounding_adjustment IS NULL
        )
    )
);

CREATE INDEX sale_cancellation_actor_time_idx
    ON sale_cancellation (actor_id, occurred_at DESC);

CREATE FUNCTION validate_sale_cancellation_insert()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    current_status VARCHAR(20);
    current_total NUMERIC(19, 0);
BEGIN
    SELECT status, total INTO current_status, current_total
    FROM sale
    WHERE id = NEW.sale_id;

    IF current_status <> 'CONFIRMED' THEN
        RAISE EXCEPTION 'only a confirmed sale can be cancelled';
    END IF;
    IF NEW.amount <> current_total THEN
        RAISE EXCEPTION 'cancellation amount must equal the exact sale total';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER sale_cancellation_validate_insert_trigger
BEFORE INSERT ON sale_cancellation
FOR EACH ROW
EXECUTE FUNCTION validate_sale_cancellation_insert();

CREATE TRIGGER sale_cancellation_immutable_trigger
BEFORE UPDATE OR DELETE ON sale_cancellation
FOR EACH ROW
EXECUTE FUNCTION prevent_sale_history_mutation();

DROP TRIGGER sale_history_immutable_trigger ON sale;

CREATE FUNCTION protect_sale_history()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'confirmed sale history is immutable';
    END IF;
    IF OLD.status = 'CONFIRMED'
       AND NEW.status = 'VOIDED'
       AND NEW.id IS NOT DISTINCT FROM OLD.id
       AND NEW.folio IS NOT DISTINCT FROM OLD.folio
       AND NEW.cash_session_id IS NOT DISTINCT FROM OLD.cash_session_id
       AND NEW.actor_id IS NOT DISTINCT FROM OLD.actor_id
       AND NEW.confirmed_at IS NOT DISTINCT FROM OLD.confirmed_at
       AND NEW.total IS NOT DISTINCT FROM OLD.total
       AND NEW.idempotency_key IS NOT DISTINCT FROM OLD.idempotency_key
       AND NEW.request_hash IS NOT DISTINCT FROM OLD.request_hash
       AND EXISTS (SELECT 1 FROM sale_cancellation WHERE sale_id = OLD.id) THEN
        RETURN NEW;
    END IF;
    RAISE EXCEPTION 'confirmed sale history is immutable';
END;
$$;

CREATE TRIGGER sale_history_immutable_trigger
BEFORE UPDATE OR DELETE ON sale
FOR EACH ROW
EXECUTE FUNCTION protect_sale_history();
