FROM nginx:alpine

# Copy custom Nginx configuration with wasm mime-type & SPA fallback
COPY webApp/nginx.conf /etc/nginx/conf.d/default.conf

# Copy precompiled Wasm distribution files from the GitHub Actions builder
COPY webApp/build/dist/wasmJs/productionExecutable /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
