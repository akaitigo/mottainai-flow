-- supplies table: registered surplus food from providers
CREATE TABLE IF NOT EXISTS supplies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id VARCHAR(100) NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    category SMALLINT NOT NULL DEFAULT 0,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit VARCHAR(20) NOT NULL,
    expiry_date TIMESTAMPTZ NOT NULL,
    pickup_window_start TIMESTAMPTZ NOT NULL,
    pickup_window_end TIMESTAMPTZ NOT NULL,
    postal_code VARCHAR(10),
    prefecture VARCHAR(10),
    city VARCHAR(50),
    street VARCHAR(200),
    location GEOGRAPHY(POINT, 4326),
    latitude DOUBLE PRECISION DEFAULT 0.0,
    longitude DOUBLE PRECISION DEFAULT 0.0,
    status SMALLINT NOT NULL DEFAULT 1,
    description TEXT DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (pickup_window_start < pickup_window_end)
);

CREATE INDEX IF NOT EXISTS idx_supplies_location ON supplies USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_supplies_status_created ON supplies (status, created_at DESC, id);
CREATE INDEX IF NOT EXISTS idx_supplies_provider ON supplies (provider_id);

-- demands table: registered food needs from recipients
CREATE TABLE IF NOT EXISTS demands (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id VARCHAR(100) NOT NULL,
    category SMALLINT NOT NULL DEFAULT 0,
    desired_quantity INT NOT NULL CHECK (desired_quantity > 0),
    unit VARCHAR(20) NOT NULL,
    delivery_window_start TIMESTAMPTZ NOT NULL,
    delivery_window_end TIMESTAMPTZ NOT NULL,
    postal_code VARCHAR(10),
    prefecture VARCHAR(10),
    city VARCHAR(50),
    street VARCHAR(200),
    location GEOGRAPHY(POINT, 4326),
    latitude DOUBLE PRECISION DEFAULT 0.0,
    longitude DOUBLE PRECISION DEFAULT 0.0,
    status SMALLINT NOT NULL DEFAULT 1,
    description TEXT DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (delivery_window_start < delivery_window_end)
);

CREATE INDEX IF NOT EXISTS idx_demands_location ON demands USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_demands_status_created ON demands (status, created_at DESC, id);
CREATE INDEX IF NOT EXISTS idx_demands_recipient ON demands (recipient_id);

-- deliveries table: delivery records for traceability
CREATE TABLE IF NOT EXISTS deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id VARCHAR(100) NOT NULL,
    supply_id UUID NOT NULL REFERENCES supplies(id),
    demand_id UUID NOT NULL REFERENCES demands(id),
    driver_id VARCHAR(100) DEFAULT '',
    status SMALLINT NOT NULL DEFAULT 1,
    pickup_photo_url TEXT DEFAULT '',
    delivery_photo_url TEXT DEFAULT '',
    pickup_quantity INT DEFAULT 0,
    delivery_quantity INT DEFAULT 0,
    pickup_condition TEXT DEFAULT '',
    delivery_condition TEXT DEFAULT '',
    pickup_at TIMESTAMPTZ,
    delivery_at TIMESTAMPTZ,
    notes TEXT DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_deliveries_match ON deliveries (match_id);
CREATE INDEX IF NOT EXISTS idx_deliveries_status ON deliveries (status);
CREATE INDEX IF NOT EXISTS idx_deliveries_driver ON deliveries (driver_id);

-- match_results table: stores matching execution results
CREATE TABLE IF NOT EXISTS match_results (
    id VARCHAR(100) PRIMARY KEY,
    status SMALLINT NOT NULL DEFAULT 0,
    result_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
