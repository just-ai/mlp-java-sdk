FROM openjdk:17

WORKDIR /app

ADD target/mpl-client-app/lib    /app/lib
ADD target/mpl-client-app        /app

ENTRYPOINT ["java", "-cp", "*:lib/*", "client.ClientSdkExampleKt"]