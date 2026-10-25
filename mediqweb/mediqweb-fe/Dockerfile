FROM node:24-alpine
WORKDIR /app
COPY package.json ./
RUN npm install --no-fund --no-audit
COPY . .
# Substitute Render-provided API URLs into environment.prod.ts before building
RUN if [ -n "$API_BASE_URL" ]; then sed -i "s#https://api\.mediq\.app#$API_BASE_URL#g" src/environments/environment.prod.ts; fi
RUN if [ -n "$BFF_BASE_URL" ]; then sed -i "s#https://bff\.mediq\.app#$BFF_BASE_URL#g" src/environments/environment.prod.ts; fi
RUN npx ng build
ENV API_BASE_URL=http://localhost:8099
ENV BFF_BASE_URL=http://localhost:3000
ENV NODE_ENV=production
EXPOSE 80
CMD ["npx", "http-server", "dist/mediqapp-fe", "-p", "80"]
