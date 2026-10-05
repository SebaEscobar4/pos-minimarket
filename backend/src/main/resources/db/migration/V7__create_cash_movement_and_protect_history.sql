CREATE TABLE cash_movement (
    id UUID PRIMARY KEY,
    cash_session_id UUID NOT NULL REFERENCES cash_session (id),
    movement_type VARCHAR(30) NOT NULL,
    category VARCHAR(40),
    amount NUMERIC(19, 0) NOT NULL,
    reason VARCHAR(500),
    reference VARCHAR(100),
    actor_id UUID NOT NULL REFERENCES app_user (id),
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT cash_movement_type_check CHECK (
        movement_type IN ('CASH_SALE', 'MANUAL_INCOME', 'MANUAL_WITHDRAWAL', 'CASH_REFUND')
    ),
    CONSTRAINT cash_movement_amount_check CHECK (
        amount > 0 AND amount <= 9007199254740991
    ),
    CONSTRAINT cash_movement_reason_not_blank CHECK (
        reason IS NULL OR btrim(reason) <> ''
    ),
    CONSTRAINT cash_movement_reference_not_blank CHECK (
        reference IS NULL OR btrim(reference) <> ''
    ),
    CONSTRAINT cash_movement_evidence_check CHECK (
        (
            movement_type = 'MANUAL_INCOME'
            AND category IN ('CASH_REPLENISHMENT', 'OTHER_INCOME')
            AND reason IS NOT NULL
            AND reference IS NULL
        )
        OR (
            movement_type = 'MANUAL_WITHDRAWAL'
            AND category IN (
                'SUPPLIER_PAYMENT',
                'OPERATING_EXPENSE',
                'SAFE_DROP',
                'OTHER_WITHDRAWAL'
            )
            AND reason IS NOT NULL
            AND reference IS NULL
        )
        OR (
            movement_type IN ('CASH_SALE', 'CASH_REFUND')
            AND category IS NULL
            AND reason IS NULL
            AND reference IS NOT NULL
        )
    )
);

CREATE INDEX cash_movement_session_time_idx
    ON cash_movement (cash_session_id, occurred_at DESC, id DESC);
CREATE INDEX cash_movement_actor_time_idx
    ON cash_movement (actor_id, occurred_at DESC);

CREATE FUNCTION prevent_cash_movement_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'cash movements are immutable';
END;
$$;

CREATE TRIGGER cash_movement_immutable_trigger
BEFORE UPDATE OR DELETE ON cash_movement
FOR EACH ROW
EXECUTE FUNCTION prevent_cash_movement_mutation();

CREATE FUNCTION protect_cash_session_history()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'cash sessions cannot be deleted';
    END IF;
    IF OLD.status = 'CLOSED' THEN
        RAISE EXCEPTION 'closed cash sessions are immutable';
    END IF;
    IF NEW.id IS DISTINCT FROM OLD.id
       OR NEW.opened_by IS DISTINCT FROM OLD.opened_by
       OR NEW.opened_at IS DISTINCT FROM OLD.opened_at
       OR NEW.opening_amount IS DISTINCT FROM OLD.opening_amount
       OR NEW.status <> 'CLOSED' THEN
        RAISE EXCEPTION 'only cash session closure is allowed';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER cash_session_history_trigger
BEFORE UPDATE OR DELETE ON cash_session
FOR EACH ROW
EXECUTE FUNCTION protect_cash_session_history();
