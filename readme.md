

# About

Implementation of a "hypothetical" bank server, providing basic banking operations, such as creating an account or transfer money. Communication with clients is established through sockets. A custom text protocol ensures consent.

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

Errors: 1

## Create Account (3)

Request:

```
3 owner
```

Success Response:

```
ok
account
```

Errors: 2

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

Errors: 3

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
```

Errors: 4|5|6

## Deposit (6)

Request:

```
6
account
amount
```

Response:

```
ok
```

Errors: 4|6

## Withdraw (7)

Request:

```
7
account
amount
```

Response:

```
ok
```

Errors: 4|5|6

# Errors

| Error Code | Description                   |
| ---------- | ----------------------------- |
| 0          | Internal Error.               |
| 1          | Account does not exist.       |
| 2          | Account could not be created. |
| 3          | Account could not be closed.  |
| 4          | Inactive Account.             |
| 5          | Account Overdraw.             |
| 6          | Illegal argument.             |
| 7          | Bad request.                  |
