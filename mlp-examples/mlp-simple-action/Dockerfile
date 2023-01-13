FROM openjdk:17

WORKDIR /app

ADD target/mlp-simple-action/lib    /app/lib
ADD target/mlp-simple-action        /app

ENTRYPOINT ["java", "-cp", "*:lib/*", "simple_action.MainKt"]