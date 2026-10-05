CREATE TABLE cash_session (
    id UUID PRIMARY KEY,
    opened_by UUID NOT NULL REFERENCES app_user (id),
    opened_at TIMESTAMPTZ NOT NULL,
    opening_amount NUMERIC(19, 0) NOT NULL,
    status VARCHAR(20) NOT NULL,
    closed_by UUID REFERENCES app_user (id),
    closed_at TIMESTAMPTZ,
    counted_cash NUMERIC(19, 0),
    expected_cash NUMERIC(19, 0),
    cash_difference NUMERIC(19, 0),
    CONSTRAINT cash_session_status_check CHECK (status IN ('OPEN', 'CLOSED')),
    CONSTRAINT cash_session_opening_amount_check CHECK (
        opening_amount >= 0 AND opening_amount <= 9007199254740991
    ),
    CONSTRAINT cash_session_closure_check CHECK (
        (
            status = 'OPEN'
            AND closed_by IS NULL
            AND closed_at IS NULL
            AND counted_cash IS NULL
            AND expected_cash IS NULL
            AND cash_difference IS NULL
        )
        OR (
            status = 'CLOSED'
            AND closed_by IS NOT NULL
            AND closed_at IS NOT NULL
            AND counted_cash IS NOT NULL
            AND expected_cash IS NOT NULL
            AND cash_difference IS NOT NULL
        )
    ),
    CONSTRAINT cash_session_counted_cash_check CHECK (
        counted_cash IS NULL OR (counted_cash >= 0 AND counted_cash <= 9007199254740991)
    ),
    CONSTRAINT cash_session_expected_cash_check CHECK (
        expected_cash IS NULL OR (expected_cash >= 0 AND expected_cash <= 9007199254740991)
    ),
    CONSTRAINT cash_session_difference_check CHECK (
        cash_difference IS NULL OR cash_difference = counted_cash - expected_cash
    )
);

CREATE UNIQUE INDEX cash_session_single_open_idx
    ON cash_session (status)
    WHERE status = 'OPEN';

CREATE INDEX cash_session_opened_at_idx ON cash_session (opened_at DESC, id DESC);
