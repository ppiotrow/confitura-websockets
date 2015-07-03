
--Run application
sbt run -Dakka.remote.netty.tcp.port=2550 -Dhttp.port=9000

--Create user:
curl -X POST http://localhost:9000/user/przemek
curl -X POST http://localhost:9000/user/clientX

--Create auction
curl -H "Content-Type: application/json" -X POST -d '{"title":"confitura.pl ticket","img":"confitura.png"}' http://localhost:9000/item/


--Cluster
sbt run -Dakka.remote.netty.tcp.port=2551 -Dhttp.port=9001
sbt run -Dakka.remote.netty.tcp.port=2552 -Dhttp.port=9002

curl -X POST http://localhost:9000/user/user1
curl -X POST http://localhost:9000/user/hack3r

curl -H "Content-Type: application/json" -X POST -d '{"title":"Tesla, model S","img":"tesla.png"}' http://localhost:9001/item/
