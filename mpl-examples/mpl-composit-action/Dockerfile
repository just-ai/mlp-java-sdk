FROM openjdk:17

WORKDIR /app

ADD target/mpl-composit-action/lib    /app/lib
ADD target/mpl-composit-action        /app

ENTRYPOINT ["java", "-cp", "*:lib/*", "composit_action.MainKt"]