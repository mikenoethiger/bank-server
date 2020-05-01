# compile stage, layers from this stage won't appear in final image
FROM openjdk:15-slim AS compile

COPY src src
RUN javac -d out src/main/java/bank/Server.java src/main/java/bank/Client.java

# new stage for final image
FROM openjdk:15-slim

COPY --from=compile out .
EXPOSE 5001

CMD ["java", "bank.Server", "5001"]