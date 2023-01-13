FROM openjdk:17

WORKDIR /app

ADD target/mlp-client-app/lib    /app/lib
ADD target/mlp-client-app        /app

ENTRYPOINT ["java", "-cp", "*:lib/*", "client.ClientSdkExampleKt"]