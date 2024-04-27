FROM openjdk:17

WORKDIR /app

ADD target/mlp-fit-action/lib    /app/lib
ADD target/mlp-fit-action        /app

ENTRYPOINT ["java", "-cp", "*:lib/*", "MainKt"]