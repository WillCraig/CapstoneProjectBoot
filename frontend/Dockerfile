# Runtime-only image: CI supplies the verified Angular production bundle in dist/.
# The bundle is packaged once here; it is not rebuilt during deployment.
FROM nginxinc/nginx-unprivileged:1.27-alpine

COPY dist/lab50-crm-ui/browser/ /usr/share/nginx/html/

# Angular client-side routes must fall back to index.html instead of returning 404.
RUN printf '%s\n' \
      'server {' \
      '  listen 8080;' \
      '  server_name _;' \
      '  root /usr/share/nginx/html;' \
      '  index index.html;' \
      '  location / {' \
      '    try_files $uri $uri/ /index.html;' \
      '  }' \
      '}' \
      > /etc/nginx/conf.d/default.conf

EXPOSE 8080
