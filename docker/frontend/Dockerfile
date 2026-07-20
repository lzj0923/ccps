# 阶段 1: 构建 Vue 前端
FROM node:20-alpine AS builder
WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ .
# 生产环境通过 nginx 代理 /api，使用相对路径
ENV VITE_API_BASE_URL=/api
RUN npm run build

# 阶段 2: Nginx 服务静态文件
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY docker/frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
