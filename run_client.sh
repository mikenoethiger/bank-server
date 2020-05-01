#!/bin/bash

# exit if any command fails
set -e

javac -d out src/main/java/bank/Client.java
cd out
java bank/Client "$@"