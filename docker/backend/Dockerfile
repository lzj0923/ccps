FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 限制 JVM 内存（服务器仅 1.8G 总内存）
ENV JAVA_OPTS="-Xms256m -Xmx512m"

COPY backend/target/ccps-backend-0.0.1-SNAPSHOT.jar app.jar

# 创建上传目录
RUN mkdir -p uploads/payment-proofs uploads/maintenance-attachments uploads/reserve-topups

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
