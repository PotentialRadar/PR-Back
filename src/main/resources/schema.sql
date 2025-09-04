-- Ensure Postgres extensions exist before Hibernate DDL runs
-- This script should run before JPA schema generation.

CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

