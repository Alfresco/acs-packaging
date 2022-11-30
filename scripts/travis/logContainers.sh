#!/usr/bin/env bash

set -e

while true; do
    sleep 10
    docker ps -a
    docker stats -a --no-stream
done
