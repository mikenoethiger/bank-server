#!/bin/bash

# exit if any command fails
set -e

javac -d out src/main/java/bank/Server.java
cd out
java bank/Server "$@"