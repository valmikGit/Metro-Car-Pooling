# Base
FROM node:20-bookworm-slim AS base
WORKDIR /app

# Dependencies
FROM base AS deps
# Copy from frontend/ since build context is project root
COPY package.json package-lock.json ./
RUN npm install

# Builder
FROM base AS builder
ENV NEXT_TELEMETRY_DISABLED=1
ENV npm_config_user_agent=npm
ARG NEXT_PUBLIC_API_URL
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL

COPY --from=deps /app/node_modules ./node_modules
# Copy frontend folder contents
COPY . .
RUN npm run build

# Runtime
FROM node:20-bookworm-slim AS runner
WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

RUN addgroup --system nextjs && adduser --system --ingroup nextjs nextjs

COPY --from=builder /app/public ./public
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static

USER nextjs
EXPOSE 3000
CMD ["node", "server.js"]
