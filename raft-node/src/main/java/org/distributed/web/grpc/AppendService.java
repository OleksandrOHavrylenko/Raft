package org.distributed.web.grpc;

import io.grpc.stub.StreamObserver;
import org.distributed.model.appendentries.AppendEntriesRequest;
import org.distributed.model.appendentries.AppendEntriesResponse;
import org.distributed.model.dto.LogItem;
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
        logger.debug("appendEntries in gRPC server - request: {}", request);

        AppendEntriesResponse appendEntriesResponse = stateManager.onReplicateRequest(convertTo(request));
        ResponseAppendEntriesRPC response = ResponseAppendEntriesRPC.newBuilder()
                .setTerm(appendEntriesResponse.term())
                .setSuccess(appendEntriesResponse.success())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private AppendEntriesRequest convertTo(RequestAppendEntriesRPC request) {
        return new AppendEntriesRequest(
                request.getTerm(),
                request.getLeaderId(),
                request.getPrevLogIndex(),
                request.getPrevLogTerm(),
                request.getEntriesList().stream()
                        .map(v -> new LogItem(v.getIndex(), v.getCommand(), v.getTerm()))
                        .toList(),
                request.getLeaderCommit());
    }
}
