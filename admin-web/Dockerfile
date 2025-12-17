FROM node:21-slim

WORKDIR /app

COPY package*.json ./

RUN npm install --omit=dev && npm cache clean --force

COPY . .

CMD ["npm", "start"]

EXPOSE 3000     