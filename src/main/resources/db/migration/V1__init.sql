CREATE TABLE sector (
    sector_code VARCHAR(10) NOT NULL PRIMARY KEY,
    base_price DECIMAL(10,2) NOT NULL,
    max_capacity INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE parking_spot (
    id BIGINT NOT NULL PRIMARY KEY,
    sector_code VARCHAR(10) NOT NULL,
    lat DECIMAL(10,6) NOT NULL,
    lng DECIMAL(10,6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_parking_spot_sector FOREIGN KEY (sector_code) REFERENCES sector(sector_code),
    CONSTRAINT uk_spot_coordinates UNIQUE (lat, lng)
);

CREATE TABLE parking_session (
    id CHAR(36) NOT NULL PRIMARY KEY,
    license_plate VARCHAR(20) NOT NULL,
    sector_code VARCHAR(10) NULL,
    spot_id BIGINT NULL,
    status VARCHAR(20) NOT NULL,
    entry_time TIMESTAMP NOT NULL,
    parked_at TIMESTAMP NULL,
    exit_time TIMESTAMP NULL,
    price_multiplier DECIMAL(5,2) NULL,
    hourly_rate DECIMAL(10,2) NULL,
    charged_hours INT NULL,
    amount DECIMAL(10,2) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_session_sector FOREIGN KEY (sector_code) REFERENCES sector(sector_code),
    CONSTRAINT fk_session_spot FOREIGN KEY (spot_id) REFERENCES parking_spot(id)
);

CREATE INDEX idx_session_plate_status ON parking_session (license_plate, status);
CREATE INDEX idx_session_exit_sector ON parking_session (exit_time, sector_code);
CREATE INDEX idx_spot_sector_status ON parking_spot (sector_code, status);
