-- H2-compatible schema for testing (mirrors db/init/002_schema.sql)
-- UUID defaults are managed by application code, not DB defaults

CREATE TABLE IF NOT EXISTS supplies (
    id UUID PRIMARY KEY,
    provider_id VARCHAR(100) NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    category SMALLINT NOT NULL DEFAULT 0,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit VARCHAR(20) NOT NULL,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    pickup_window_start TIMESTAMP WITH TIME ZONE NOT NULL,
    pickup_window_end TIMESTAMP WITH TIME ZONE NOT NULL,
    postal_code VARCHAR(10),
    prefecture VARCHAR(10),
    city VARCHAR(50),
    street VARCHAR(200),
    latitude DOUBLE PRECISION DEFAULT 0.0,
    longitude DOUBLE PRECISION DEFAULT 0.0,
    status SMALLINT NOT NULL DEFAULT 1,
    description TEXT DEFAULT '',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (pickup_window_start < pickup_window_end)
);

CREATE TABLE IF NOT EXISTS demands (
    id UUID PRIMARY KEY,
    recipient_id VARCHAR(100) NOT NULL,
    category SMALLINT NOT NULL DEFAULT 0,
    desired_quantity INT NOT NULL CHECK (desired_quantity > 0),
    unit VARCHAR(20) NOT NULL,
    delivery_window_start TIMESTAMP WITH TIME ZONE NOT NULL,
    delivery_window_end TIMESTAMP WITH TIME ZONE NOT NULL,
    postal_code VARCHAR(10),
    prefecture VARCHAR(10),
    city VARCHAR(50),
    street VARCHAR(200),
    latitude DOUBLE PRECISION DEFAULT 0.0,
    longitude DOUBLE PRECISION DEFAULT 0.0,
    status SMALLINT NOT NULL DEFAULT 1,
    description TEXT DEFAULT '',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (delivery_window_start < delivery_window_end)
);

CREATE TABLE IF NOT EXISTS deliveries (
    id UUID PRIMARY KEY,
    match_id VARCHAR(100) NOT NULL,
    supply_id UUID NOT NULL,
    demand_id UUID NOT NULL,
    driver_id VARCHAR(100) DEFAULT '',
    status SMALLINT NOT NULL DEFAULT 1,
    pickup_photo_url TEXT DEFAULT '',
    delivery_photo_url TEXT DEFAULT '',
    pickup_quantity INT DEFAULT 0,
    delivery_quantity INT DEFAULT 0,
    pickup_condition TEXT DEFAULT '',
    delivery_condition TEXT DEFAULT '',
    pickup_at TIMESTAMP WITH TIME ZONE,
    delivery_at TIMESTAMP WITH TIME ZONE,
    notes TEXT DEFAULT '',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS match_results (
    id VARCHAR(100) PRIMARY KEY,
    status SMALLINT NOT NULL DEFAULT 0,
    result_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
