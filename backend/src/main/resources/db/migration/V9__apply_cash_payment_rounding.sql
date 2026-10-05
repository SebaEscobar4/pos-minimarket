ALTER TABLE payment
    ADD COLUMN cash_payable NUMERIC(19, 0),
    ADD COLUMN rounding_adjustment NUMERIC(19, 0),
    ADD COLUMN cash_rounding_version SMALLINT NOT NULL DEFAULT 0;

DROP TRIGGER payment_history_immutable_trigger ON payment;

UPDATE payment
SET cash_payable = amount,
    rounding_adjustment = 0
WHERE method = 'CASH';

ALTER TABLE payment
    ALTER COLUMN cash_rounding_version SET DEFAULT 1;

ALTER TABLE payment
    DROP CONSTRAINT payment_cash_fields_check,
    ADD CONSTRAINT payment_cash_rounding_version_check CHECK (
        cash_rounding_version IN (0, 1)
    ),
    ADD CONSTRAINT payment_cash_fields_check CHECK (
        (
            method = 'CASH'
            AND cash_payable IS NOT NULL
            AND rounding_adjustment IS NOT NULL
            AND cash_received IS NOT NULL
            AND change_amount IS NOT NULL
            AND cash_payable = amount + rounding_adjustment
            AND cash_payable >= 0
            AND cash_payable <= 9007199254740991
            AND cash_received >= cash_payable
            AND change_amount = cash_received - cash_payable
            AND (
                (
                    cash_rounding_version = 0
                    AND cash_payable = amount
                    AND rounding_adjustment = 0
                )
                OR (
                    cash_rounding_version = 1
                    AND rounding_adjustment = CASE
                        WHEN MOD(amount, 10) BETWEEN 1 AND 5 THEN -MOD(amount, 10)
                        WHEN MOD(amount, 10) BETWEEN 6 AND 9 THEN 10 - MOD(amount, 10)
                        ELSE 0
                    END
                )
            )
        )
        OR (
            method IN ('CARD', 'TRANSFER')
            AND cash_payable IS NULL
            AND rounding_adjustment IS NULL
            AND cash_received IS NULL
            AND change_amount IS NULL
        )
    );

CREATE FUNCTION enforce_current_cash_rounding_policy()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.cash_rounding_version <> 1 THEN
        RAISE EXCEPTION 'new payments must use the current cash rounding policy';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER payment_current_cash_rounding_policy_trigger
BEFORE INSERT ON payment
FOR EACH ROW
EXECUTE FUNCTION enforce_current_cash_rounding_policy();

CREATE TRIGGER payment_history_immutable_trigger
BEFORE UPDATE OR DELETE ON payment
FOR EACH ROW
EXECUTE FUNCTION prevent_sale_history_mutation();
