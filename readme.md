

* Docker: [dockerhub.com/mikenoethiger/bank-server](https://hub.docker.com/repository/docker/mikenoethiger/bank-server)
* Client implementation [github.com/mikenoethiger/bank-client](https://github.com/mikenoethiger/bank-client)

# About

Implementation of a "hypothetical" bank server, providing basic banking operations, such as creating an account or transfer money (see chapter [Actions](#Actions) for full interface specification.) Communication with clients is established through sockets. A custom text protocol ensures consent (see chapter [Protocol](#Protocol).)

# Run with Java

Compile:

```
cd java
javac Server.java Client.java
```

Show usages:

```
java Server
java Client
```

Run Server on port `5001`:

```
java Server 5001
```

Send a request to the server via command line (CLI) client:

```
java Client <ip> <port> <action> [arguments]
```

E.g. create an account:

```
java Client 127.0.0.1 5001 3 mike
```

In order to speak with the server from java code, refer to the [bank-client](https://github.com/mikenoethiger/bank-client) project which provides a java client implementation.

A very simple alternative to the CLI client is using the [nc](https://linux.die.net/man/1/nc) program which ships out of the box in most linux distributions:

```
nc 127.0.0.1 5001
3
mike


```

(First enter the nc command, then the text you want to send to the server, make sure to terminate with two line breaks, otherwise the server waits for the end of the request.)

# Run with Docker

Alternatively use the docker image from [Dockerhub](https://hub.docker.com/repository/docker/mikenoethiger/bank-server) to run the server and/or client. You can find some examples in the following.

## Server Usage

Run server on (default) port `5001` in foreground (will be deleted upon `CTRL+C`):

```
docker run --rm -p 5001:5001 mikenoethiger/bank-server
```

Run server on custom port `1234` in background (`-d`) and name the container `bank-server`:

```
docker run -d -p 1234:1234 --name bank-server mikenoethiger/bank-server Server 1234
```

Show `stdout` of a named container:

```
docker logs bank-server
```

Stop and remove a named container:

```
docker rm -f bank-sever
```

## Client Usage

The same docker image also contains the compiled CLI client. If both, the client and the server run in a container they need to be connected to the same docker network, otherwise the client can't reach the server.

Create a docker network (excute for once only):

```
docker network create bank-server
```

Start a server that is attached to the network:

```
docker run --rm -d -p 5001:5001 --name bank-server --network bank-server mikenoethiger/bank-server
```

Send requests to `bank-server` container:

```
docker run --rm --network bank-server mikenoethiger/bank-server Client bank-server 5001 3 mike
```

# Protocol

Communication between server and client is established through sockets. A simple text protocol ensures consent between the server and client. It is subject of this chapter to reveal the mechanics of this protocol.

*Request* refers to data that is sent from client to server. *Response* refers to data that is sent from server to client.

## Request

Each request shall adhere to the following format:

```
action
[argument1]
[argument2]

```

* `action`: An integer value, stating the requested action to take place
* `argument_n`: Variadic argument list, that is zero or more arguments wich augment the action. Details on arguments can be found in the **Actions** chapter.
* The delimiter between described request parts is the new line character `\n`
* The request ends with two consecutive new line characters `\n\n`

Actual request encoding:

```
action\n[argument1]\n[argument2]\n\n
```

## Response

Each response shall adhere to the following format:

```
ok|nok
[error_number]
[error_text]
[response1]
[response2]

```

* `ok|nok`: Determines whether the the request could be processed with (`nok`) or without (`ok`) errors
* `error_number`: An integer that is present in case of `nok`. See chapter **Errors** for details on error codes.
* `error_text`: A string that is present in case of `nok`. Human readable description of the error.
* `response_n`: Variadic responses, that is zero or more responses that provide additional information about the requested action. Details on action responses can be found in the **Actions** chapter.
* The delimiter between described response parts is the new line character `\n`
* The response ends with two consecutive new line characters `\n\n`

Actual response encoding:

```
ok|nok\n[error_number]\n[error_text]\n[response1]\n[response2]\n\n
```

# Actions

This chapter is a summary of available actions. An action is something that a client can request the server to perform using the custom text protocol.

**Terminology**

* `account`: (string) Account number
* `owner`: (string) The name of an account owner
* `balance`: (float) An account balance
* `active`: (int) Denotes whether an account is active/inactive. `0` denotes an inactive account, every other integer denotes an active account

Sometimes, multiple entities of the same type appear in one request/response, in these cases above terms are suffixed with anything meaningful, but yet still adhere to above description, e.g. `account_from` and `account_to` are both of type `account`.

Variadic entities are suffixed with `_0 ` and `_n`, e.g.

```
account_0
account_n
```

Means zero or more accounts may follow.

For the sake of compactness, the terminating `\n\n` is omitted in all stated requests/responses.

## Get Account Numbers (1)

Request:

```
1
```

Success Response:

```
ok
account_1
account_n
```

## Get Account (2)

Request:

```
2
account
```

Success Response:

```
ok
account
owner
balance
active
```

Errors: 1 Account does not exist

## Create Account (3)

Request:

```
3 owner
```

Success Response:

```
ok
account
owner
balance
active
```

Errors: 2 Account could not be created

## Close Account (4)

Request:

```
4
account
```

Success Response:

```
ok
```

Errors: 3 Account could not be closed

## Transfer (5)

Request:

```
5
account_from
account_to
amount
```

Success Response:

```
ok
balance_from
balance_to
```

Errors: 4 Inactive account | 5 Account overdraw | 6 Illegal argument

## Deposit (6)

Request:

```
6
account
amount
```

Success Response:

```
ok
balance
```

Errors: 4 Inactive account | 6 Illegal argument

## Withdraw (7)

Request:

```
7
account
amount
```

Sucess Response:

```
ok
balance
```

Errors: 4 Inactive account | 5 Account overdraw | 6 Illegal argument

# Errors

| Error Code | Description                   |
| ---------- | ----------------------------- |
| 0          | Internal Error.               |
| 1          | Account does not exist.       |
| 2          | Account could not be created. |
| 3          | Account could not be closed.  |
| 4          | Inactive account.             |
| 5          | Account overdraw.             |
| 6          | Illegal argument.             |
| 7          | Bad request.                  |
