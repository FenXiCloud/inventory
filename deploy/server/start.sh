#!/bin/bash
set -e
cd "$(dirname "$0")/.."
docker compose -f docker-compose.yml up -d
docker ps --filter name=jxc_
