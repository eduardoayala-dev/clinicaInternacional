FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

FROM tomcat:10.1-jdk17-temurin
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/target/clinicaInternacional.war /usr/local/tomcat/webapps/ROOT.war
COPY scripts/Iniciar-Tomcat.sh /usr/local/bin/iniciar-clinica
RUN sed -i 's/\r$//' /usr/local/bin/iniciar-clinica && chmod 755 /usr/local/bin/iniciar-clinica
EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/iniciar-clinica"]
CMD ["catalina.sh", "run"]
