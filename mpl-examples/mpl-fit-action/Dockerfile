FROM openjdk:17

WORKDIR /app

ADD target/mpl-dummy-action/lib    /app/lib
ADD target/mpl-dummy-action        /app

ENTRYPOINT ["java", "-cp", "*:lib/*", "fit_action.MainKt"]