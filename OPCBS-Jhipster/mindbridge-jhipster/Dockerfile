# ==============================================================================
# STAGE 1: Build Angular 19 SPA with Node.js 22 Alpine (LTS)
# ==============================================================================
FROM node:22-alpine AS builder

WORKDIR /app

# Cache package dependencies
COPY package.json package-lock.json ./
RUN npm ci --prefer-offline --no-audit || npm install --legacy-peer-deps

# Copy toàn bộ mã nguồn frontend
COPY . .

# Biên dịch production bundle
RUN npm run webapp:build:prod

# ==============================================================================
# STAGE 2: High-Performance Nginx Web Server for Angular SPA
# ==============================================================================
FROM nginx:1.27-alpine AS runtime

# Cài đặt cấu hình Nginx SPA (HTML5 PushState try_files)
RUN rm -rf /etc/nginx/conf.d/default.conf

COPY <<EOF /etc/nginx/conf.d/spa.conf
server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    # Gzip Compression
    gzip on;
    gzip_vary on;
    gzip_types text/plain text/css application/json application/javascript image/svg+xml;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    # Static Assets & Media Cache (1 năm)
    location ~* \.(?:ico|css|js|gif|jpe?g|png|woff2?|eot|ttf|svg|mp4|webm|ogg|m4v)$ {
        expires 1y;
        add_header Cache-Control "public, max-age=31536000, immutable";
    }
}
EOF

# Copy static assets từ Stage 1
COPY --from=builder /app/target/classes/static/ /usr/share/nginx/html/

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
