-- ======================================
-- SQL Init Script: Create "rider" Table
-- ======================================

CREATE TABLE IF NOT EXISTS rider (
                                     id SERIAL PRIMARY KEY,
                                     username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
    );