package org.distributed.web.grpc;

import io.grpc.stub.StreamObserver;
import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;
import org.distributed.statemanager.StateManager;
import org.distributed.stubs.RequestVoteRPC;
import org.distributed.stubs.ResponseVoteRPC;
import org.distributed.stubs.VoteServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author Oleksandr Havrylenko
 **/
@Service
public class VoteService extends VoteServiceGrpc.VoteServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoteService.class);

    private final StateManager stateManager;

    public VoteService(final StateManager stateManager) {
        this.stateManager = Objects.requireNonNull(stateManager);
    }

    @Override
    public void requestVote(RequestVoteRPC request, StreamObserver<ResponseVoteRPC> responseObserver) {

        LOGGER.info("requestVote in gRPC server - request: {}", request);

        final VoteResponse voteResponse = stateManager.requestVote(
                new VoteRequest(
                        request.getTerm(), request.getCandidateId(),
                        request.getLastLogIndex(), request.getLastLogTerm()));

        ResponseVoteRPC response = ResponseVoteRPC.newBuilder()
                .setTerm(voteResponse.term())
                .setVoteGranted(voteResponse.voteGranted())
                .build();

        // Send the response to the client.
        responseObserver.onNext(response);

        // Notifies the customer that the call is completed.
        responseObserver.onCompleted();
    }
}
