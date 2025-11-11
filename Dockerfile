# ================================
# 🏗️ Stage 1 — Build (Maven + JDK 21)
# ================================
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copy Maven config and preload dependencies
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

# Copy source code and build
COPY src ./src
RUN mvn -B -DskipTests clean package

# ================================
# 🚀 Stage 2 — Runtime (JRE 21 only)
# ================================
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /workspace/target/*.jar app.jar

# Expose app port
EXPOSE 8080

# Optional memory tuning
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# ✅ Entry point — automatically reads Render's $PORT env
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
