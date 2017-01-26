call gradlew build preparedocker
call docker-compose kill
call docker-compose rm -f
call docker-compose build
call docker-compose up -d
sleep 60
set SW360DEMO_REST_SERVER_URL=http://192.168.99.100:8091
set SW360DEMO_AUTH_SERVER_URL=http://192.168.99.100:8090
set SW360DEMO_THRIFT_SERVER_URL=http://192.168.99.100:8080
gradlew rundemo
