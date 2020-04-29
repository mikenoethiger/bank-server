
[dockerhub](https://hub.docker.com/r/mikenoethiger/bank-server) | [client implementation](https://github.com/mikenoethiger/bank-client) 

# About

Minimal bank server backend, provides basic banking operations, such as creating an account or transfer money (see chapter [Actions](#Actions) for full API specification.) 

The code arose during a module in distributed systems at my university. The goal was to implement a banking backend using several technologies/approaches (sockets, http, rest, websockets, graphql, rabbitmq) to see different ways of implementing a distributed system. This is the plain socket implementation. 

A custom text protocol ensures consent (see chapter [Protocol](#Protocol).)

# Run Server

Compile and run on port `5001`:

```
$ cd java && ./compile.sh
$ java Server 5001
```

> Alternatively use the `mikenoethiger/bank-server` image from [dockerhub](https://hub.docker.com/r/mikenoethiger/bank-server) to run the server with docker.
> E.g. `docker run --rm -p 5001:5001 mikenoethiger/bank-server`

# Send Requests

Use any TCP/IP client to connect to the server on `host:port` and send a request according to the [Protocol](#protocol) (e.g. [nc](https://linux.die.net/man/1/nc).)  

Or use the `Client` program from this repo:

```
$ java Client <host> <port> <action> [arguments]
```

E.g. create an account:

```
$ java Client 127.0.0.1 5001 3 mike
```

> If you want to connect from java code, take a look [here](https://github.com/mikenoethiger/bank-client/tree/master/src/main/java/bank/socket).

# Protocol

Communication between server and client is established through sockets. A simple text protocol ensures consent between the parties. This chapter explains in detail how this protocol works. You might want to jump to the [Actions](#Actions) chapter if you're only interested in the usages / API specification.

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
[status_code]
[response_data1]
[response_data2]

```

* `status_code`: An integer representing the status of the processed request (0=ok, everything else is an error code.) See [Status Codes](#status-codes) for details on status codes.
* `response_data_n`: Zero or more lines containing response data for the requested action. If status code is an error, the first line of response data is the error description by convention. See [Actions](#actions) chapter for response data documentation.
* Response data is delimited with the new line character `\n`
* The whole response ends with two consecutive new line characters `\n\n`

Actual response encoding:

```
[status_code]\n[response_data1]\n[response_data2]\n\n
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
0
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
0
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
0
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
0
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
0
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
0
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
0
balance
```

Errors: 4 Inactive account | 5 Account overdraw | 6 Illegal argument

# Status Codes

| Status Code | Description                   |
| ----------- | ----------------------------- |
| 0           | OK                            |
| 1           | Account does not exist.       |
| 2           | Account could not be created. |
| 3           | Account could not be closed.  |
| 4           | Inactive account.             |
| 5           | Account overdraw.             |
| 6           | Illegal argument.             |
| 7           | Bad request.                  |
| 8           | Internal Error.               |

# License

Copyright 2020 Mike Nöthiger (noethiger.mike@gmail.com)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, version 3 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program (COPYING file.) If not, see <https://www.gnu.org/licenses/>.