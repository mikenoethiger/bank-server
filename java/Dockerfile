FROM openjdk:15-slim

COPY Server.java .
COPY Client.java .
RUN javac Server.java Client.java

ENTRYPOINT ["java"]
CMD ["Server", "50001"]
