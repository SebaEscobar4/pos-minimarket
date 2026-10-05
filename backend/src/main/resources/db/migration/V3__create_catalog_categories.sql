CREATE TABLE catalog_category (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES app_user (id),
    updated_by UUID NOT NULL REFERENCES app_user (id),
    CONSTRAINT catalog_category_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT catalog_category_status_check CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX catalog_category_status_name_idx ON catalog_category (status, name, id);
