FROM openjdk:17

ARG IMAGE_NAME

WORKDIR /app

ADD target/mlp-simple-action/lib    /app/lib
ADD target/mlp-simple-action        /app

ENV IMAGE_NAME="$IMAGE_NAME"

ENTRYPOINT ["java", "-cp", "*:lib/*", "simple_action.MainKt"]