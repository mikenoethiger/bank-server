FROM openjdk:15-slim

COPY java/Server.java .
COPY java/Client.java .
RUN javac Server.java Client.java

EXPOSE 5001

ENTRYPOINT ["java"]
CMD ["Server", "5001"]
