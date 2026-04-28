FROM node:22-alpine AS builder
WORKDIR /app
COPY admin-web/package*.json ./
RUN npm ci
COPY admin-web ./
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.27-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
