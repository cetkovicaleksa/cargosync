#!/usr/bin/env sh
# set -e
docker compose -f compose.yaml -f compose.test.yaml down
