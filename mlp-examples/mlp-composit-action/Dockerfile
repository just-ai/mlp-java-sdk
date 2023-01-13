FROM openjdk:17

WORKDIR /app

ADD target/mlp-composit-action/lib    /app/lib
ADD target/mlp-composit-action        /app

ENTRYPOINT ["java", "-cp", "*:lib/*", "composit_action.MainKt"]