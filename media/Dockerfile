FROM eclipse-temurin:25-jre-alpine
COPY media/target/media*.jar app.jar
COPY sampledata/images /images
ENTRYPOINT ["java","-jar","/app.jar"]