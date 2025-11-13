# =========================================
# Stage 1: Initialize PostgreSQL image
# =========================================
FROM postgres:16-alpine AS base

# Set environment variables for PostgreSQL
ENV POSTGRES_DB=metrocarpool \
    POSTGRES_USER=postgres \
    POSTGRES_PASSWORD=postgres

# Copy initialization SQL scripts if any exist
# (Optional — for schema or seed data)
# Place your SQL scripts in a folder called `initdb/` next to this Dockerfile
COPY initdb /docker-entrypoint-initdb.d/

# =========================================
# Stage 2: (Optional) Slim final image
# =========================================
# Since PostgreSQL images are already optimized, we can reuse the base stage
# directly — no need for a separate runtime stage.

# Expose PostgreSQL port
EXPOSE 5432

# Volume mount point for database persistence
# Docker will store your DB data here
VOLUME ["/var/lib/postgresql/data"]

# Default entrypoint and command
ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["postgres"]