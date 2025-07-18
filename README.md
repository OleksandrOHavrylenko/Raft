

#### Curl commands to append messages and read list of messages from Leader and Followers
```Shell
curl -H 'Content-Type: application/json' -X POST http://localhost:8081/append -d '{ "message":"msg0"}'

curl -X GET http://localhost:8081/list
curl -X GET http://localhost:8082/list
curl -X GET http://localhost:8083/list
```