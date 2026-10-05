CREATE TABLE catalog_product (
    id UUID PRIMARY KEY,
    code VARCHAR(64),
    name VARCHAR(150) NOT NULL,
    search_name VARCHAR(150) NOT NULL,
    category_id UUID NOT NULL REFERENCES catalog_category (id),
    purchase_price NUMERIC(12, 2) NOT NULL,
    sale_price NUMERIC(12, 2) NOT NULL,
    minimum_stock INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES app_user (id),
    updated_by UUID NOT NULL REFERENCES app_user (id),
    CONSTRAINT catalog_product_code_format_check CHECK (
        code IS NULL OR code ~ '^[-A-Za-z0-9._/]{1,64}$'
    ),
    CONSTRAINT catalog_product_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT catalog_product_search_name_not_blank CHECK (btrim(search_name) <> ''),
    CONSTRAINT catalog_product_purchase_price_check CHECK (
        purchase_price >= 0 AND purchase_price <= 9999999999.99
    ),
    CONSTRAINT catalog_product_sale_price_check CHECK (
        sale_price >= 0 AND sale_price <= 9999999999.99
    ),
    CONSTRAINT catalog_product_minimum_stock_check CHECK (
        minimum_stock >= 0 AND minimum_stock <= 1000000
    ),
    CONSTRAINT catalog_product_status_check CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX catalog_product_code_unique_idx
    ON catalog_product (code)
    WHERE code IS NOT NULL;
CREATE INDEX catalog_product_search_name_idx ON catalog_product (search_name, id);
CREATE INDEX catalog_product_category_idx ON catalog_product (category_id, id);
CREATE INDEX catalog_product_status_search_idx ON catalog_product (status, search_name, id);

CREATE TABLE inventory_balance (
    product_id UUID PRIMARY KEY REFERENCES catalog_product (id),
    quantity INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT inventory_balance_quantity_check CHECK (quantity >= 0),
    CONSTRAINT inventory_balance_version_check CHECK (version >= 0)
);
