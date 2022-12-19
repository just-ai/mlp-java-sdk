FROM openjdk:17

WORKDIR /app

ADD target/mpl-simple-action/lib    /app/lib
ADD target/mpl-simple-action        /app

ENTRYPOINT ["java", "-cp", "*:lib/*", "simple_action.MainKt"]