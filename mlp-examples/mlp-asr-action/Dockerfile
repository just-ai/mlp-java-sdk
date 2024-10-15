FROM openjdk:17

ARG IMAGE_NAME

WORKDIR /app

ADD target/mlp-asr-action/lib    /app/lib
ADD target/mlp-asr-action        /app

ENV IMAGE_NAME="$IMAGE_NAME"

ENTRYPOINT ["java", "-cp", "*:lib/*", "asr_action.MainKt"]
