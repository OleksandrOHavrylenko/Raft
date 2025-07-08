FROM openjdk:17
ARG JAR_FILE=raft-node/target/*.jar
COPY ${JAR_FILE} raft-node.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/raft-node.jar"]