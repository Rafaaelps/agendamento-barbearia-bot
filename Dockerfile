# 1. Traz o Maven para baixar as dependências e empacotar o código
FROM maven:3.9.6-eclipse-temurin-17 AS build
COPY . /app
WORKDIR /app
RUN mvn clean package -DskipTests

# 2. Pega só o pacote pronto e coloca num servidor Java limpo e leve
FROM eclipse-temurin:17-jre
COPY --from=build /app/target/*.jar /app/app.jar

# 3. Libera a porta 8080 e aperta o Play!
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]