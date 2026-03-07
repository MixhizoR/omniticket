# 1. Aşama: Derleme (Build)
FROM maven:3.9.12-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Bağımlılıkları cache'lemek için sadece pom.xml
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Kodları kopyala ve paketle
COPY src ./src
RUN mvn package -DskipTests

# Ekstra Adım: JAR dosyasını katmanlarına (layers) ayırıyoruz 🛠️
RUN java -Djarmode=layertools -jar target/*.jar extract

# 2. Aşama: Çalıştırma (Run)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Güvenlik: Uygulama için düşük yetkili bir kullanıcı oluşturuyoruz 🛡️
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring

# Katmanları sırasıyla kopyalıyoruz (Değişme ihtimali en düşükten en yükseğe)
# Bu sayede sadece kodun değişirse sadece son katman güncellenir
COPY --from=build /app/dependencies/ ./
COPY --from=build /app/spring-boot-loader/ ./
COPY --from=build /app/snapshot-dependencies/ ./
COPY --from=build /app/application/ ./

EXPOSE 8080

# JVM'in konteyner limitlerine uymasını sağlayan parametreler
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]