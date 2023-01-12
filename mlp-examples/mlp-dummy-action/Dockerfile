FROM openjdk:17

WORKDIR /app

ADD target/mlp-dummy-action/lib    /app/lib
ADD target/mlp-dummy-action        /app

ENTRYPOINT ["java", "-cp", "*:lib/*", "dummy_action.MainKt"]