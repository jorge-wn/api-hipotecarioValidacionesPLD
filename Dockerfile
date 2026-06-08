FROM maven:3.9.6-amazoncorretto-21 AS build
WORKDIR /app

# Copiar pom y descargar dependencias (Caché)
COPY pom.xml .
#RUN mvn dependency:go-offline
RUN mvn dependency:go-offline "-Dos.detected.name=linux" "-Dos.detected.arch=x86_64" "-Dos.detected.classifier=linux-x86_64"

# Construir el WAR
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa de ejecución
FROM amazoncorretto:21-al2023-headless
WORKDIR /app

# CAMBIO AQUÍ: Copiamos el .war en lugar del .jar
COPY --from=build /app/target/*.war app.war

EXPOSE 8082


# Ejecutamos el WAR como si fuera un JAR ejecutable
#ENTRYPOINT ["java", "-jar", "app.war"]
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.war"]