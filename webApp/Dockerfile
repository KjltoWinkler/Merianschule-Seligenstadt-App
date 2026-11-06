FROM nginx:alpine

# Ensure application/wasm mime type exists
RUN grep -q "application/wasm" /etc/nginx/mime.types || sed -i '/types {/a \    application/wasm wasm;' /etc/nginx/mime.types

# Copy custom Nginx configuration with SPA fallback
COPY webApp/nginx.conf /etc/nginx/conf.d/default.conf

# Copy precompiled Wasm distribution files from the GitHub Actions builder
COPY webApp/build/dist/wasmJs/productionExecutable /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
