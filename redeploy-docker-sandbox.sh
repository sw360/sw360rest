#!/bin/bash
./gradlew build preparedocker
docker-compose kill
docker-compose rm -f
docker-compose build
docker-compose up -d
sleep 1m
./gradlew rundemo
