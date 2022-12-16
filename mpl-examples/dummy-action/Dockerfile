FROM openjdk:17

WORKDIR /app

ADD target/gate-dummy-action/lib    /app/lib
ADD target/gate-dummy-action        /app

ENTRYPOINT ["java", "-cp", "*:lib/*", "dummy_action.MainKt"]