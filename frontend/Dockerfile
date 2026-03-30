FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npx tsc

FROM node:22-alpine
WORKDIR /app
COPY --from=build /app/dist /app/dist
COPY --from=build /app/package.json /app/
EXPOSE 3000
CMD ["node", "dist/index.js"]
