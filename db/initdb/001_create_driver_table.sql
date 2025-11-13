-- ======================================
-- SQL Init Script: Create "driver" Table
-- ======================================

CREATE TABLE IF NOT EXISTS driver (
                                      id SERIAL PRIMARY KEY,
                                      username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    license_id VARCHAR(100) NOT NULL UNIQUE
    );
