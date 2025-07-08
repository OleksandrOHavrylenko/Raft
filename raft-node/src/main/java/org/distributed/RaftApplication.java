package org.distributed;

import org.distributed.grpc.GrpcServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class RaftApplication {

    public static void main(String[] args) {
        ApplicationContext applicationContext = SpringApplication.run(RaftApplication.class, args);

        GrpcServer grpcServer = applicationContext.getBean("grpcServer", GrpcServer.class);
        grpcServer.start();
    }

}
