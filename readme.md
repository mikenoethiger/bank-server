[protocol](https://github.com/mikenoethiger/bank-server-socket#protocol) | [bank-client](https://github.com/mikenoethiger/bank-client) | [bank-server-socket](https://github.com/mikenoethiger/bank-server-socket) | [bank-server-graphql](https://github.com/mikenoethiger/bank-server-graphql) | [bank-server-rabbitmq](https://github.com/mikenoethiger/bank-server-rabbitmq)

# About

Java socket implementation for the server backend. The client counterpart can be found [here](https://github.com/mikenoethiger/bank-client/tree/master/src/main/java/bank/socket). 

A simple text protocol ensures consent between server/client (see chapter [Protocol](#Protocol).) 

Historically this was the first backend implementation, which is why most other implementations rely on the text protocol specified here.
That is because the protocol models the banking domain pretty well (i.e. banking operations, results, errors etc. see [Actions](#Actions), [Status Codes](#status-codes)) which makes it convenient to use on top of other technologies/protocols such as http, websocket, message queues etc. 


# Run Server

In your **IDE**, run `bank.Server` as Java Application.

Or execute the **script** `./run_server.sh` (make sure to grant execution rights `chmod u+x run_server.sh`)

Or with **gradle** `gradle run`

Or use the [docker image](https://hub.docker.com/r/mikenoethiger/bank-server-socket) `docker run --rm -p 5001:5001 mikenoethiger/bank-server-socket`


# Send Requests

Use any TCP/IP client to connect to the server and send a request according to the [Protocol](#protocol) (e.g. with [nc](https://linux.die.net/man/1/nc) `nc localhost 5001`)  

Or use the `Client` CLI program: `./run_client.sh` (make sure to grant execution rights `chmod u+x run_client.sh`)

E.g. to create an account:

```
$ ./run_client.sh localhost 5001 3 mike
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

## Actions

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

### Get Account Numbers (1)

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

### Get Account (2)

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

### Create Account (3)

Request:

```
3 
owner
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

### Close Account (4)

Request:

```
4
account
```

Success Response:

```
0
```

Errors: 1 Account does not exist | 3 Account could not be closed

### Transfer (5)

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

Errors: 1 Account does not exist | 4 Inactive account | 5 Account overdraw | 6 Illegal argument

### Deposit (6)

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

Errors: 1 Account does not exist | 4 Inactive account | 6 Illegal argument

### Withdraw (7)

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

Errors: 1 Account does not exist | 4 Inactive account | 5 Account overdraw | 6 Illegal argument

## Status Codes

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
