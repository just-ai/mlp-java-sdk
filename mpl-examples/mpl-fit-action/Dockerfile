FROM openjdk:17

WORKDIR /app

ADD target/mpl-fit-action/lib    /app/lib
ADD target/mpl-fit-action        /app

ENTRYPOINT ["java", "-cp", "*:lib/*", "fit_action.MainKt"]