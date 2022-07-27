FROM openjdk:8-alpine

COPY target/uberjar/vending-machine.jar /vending-machine/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/vending-machine/app.jar"]
