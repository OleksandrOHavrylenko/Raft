package org.distributed.web.grpc;

import io.grpc.stub.StreamObserver;
import org.distributed.statemanager.StateManager;
import org.distributed.stubs.AppendEntriesServiceGrpc;
import org.distributed.stubs.RequestAppendEntriesRPC;
import org.distributed.stubs.ResponseAppendEntriesRPC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author Oleksandr Havrylenko
 **/
@Service
public class AppendService extends AppendEntriesServiceGrpc.AppendEntriesServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(AppendService.class);

    private final StateManager stateManager;

    public AppendService(StateManager stateManager) {
        this.stateManager = Objects.requireNonNull(stateManager);
    }

    @Override
    public void appendEntries(RequestAppendEntriesRPC request, StreamObserver<ResponseAppendEntriesRPC> responseObserver) {
        logger.info("appendEntries in gRPC server - request: {}", request);

        if (request.getEntriesList().isEmpty()) {
            stateManager.onHeartbeatFromLeader();
            ResponseAppendEntriesRPC response = ResponseAppendEntriesRPC.newBuilder()
                    .setTerm(10).setSuccess(true).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }


    }
}
