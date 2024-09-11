FROM openjdk:17

ARG IMAGE_NAME

WORKDIR /app

ADD target/mlp-client-app/lib    /app/lib
ADD target/mlp-client-app        /app

ENV IMAGE_NAME="$IMAGE_NAME"

ENTRYPOINT ["java", "-cp", "*:lib/*", "client.ClientSdkExampleKt"]