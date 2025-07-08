package org.distributed.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import jakarta.annotation.PreDestroy;
import org.distributed.web.grpc.VoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author Oleksandr Havrylenko
 **/
@Component
public class GrpcServer {
    private static final Logger log = LoggerFactory.getLogger(GrpcServer.class);

    private int port;
    private Server grpcServer;

    public GrpcServer(final VoteService voteService, @Value("${grpc.server.port}") final int port) {
        this.port = port;
        grpcServer = ServerBuilder.
                forPort(port).
                addService(voteService).
                build();
    }

    public void start() {
        try {
            grpcServer.start();
            log.info("gRPC server started at port: {}", port);
            // Server is kept alive for the client to communicate.
            grpcServer.awaitTermination();
        } catch (IOException e) {
            log.error("gRPC server NOT started at port: {}", port);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            log.error("gRPC server NOT started awaitTermination at port: {}", port);
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void destroy() {
        grpcServer.shutdown();
        log.info("gRPC server stopped at port: {}", port);
    }
}
