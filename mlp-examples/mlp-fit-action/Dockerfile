FROM openjdk:17

ARG IMAGE_NAME

WORKDIR /app

ADD target/mlp-fit-action/lib    /app/lib
ADD target/mlp-fit-action        /app

ENV IMAGE_NAME="$IMAGE_NAME"

ENTRYPOINT ["java", "-cp", "*:lib/*", "MainKt"]