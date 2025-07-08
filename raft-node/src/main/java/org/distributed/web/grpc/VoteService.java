package org.distributed.web.grpc;

import io.grpc.stub.StreamObserver;
import org.distributed.stubs.RequestVoteRPC;
import org.distributed.stubs.ResponseVoteRPC;
import org.distributed.stubs.VoteServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author Oleksandr Havrylenko
 **/
@Service
public class VoteService extends VoteServiceGrpc.VoteServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoteService.class);

    @Override
    public void requestVote(RequestVoteRPC request, StreamObserver<ResponseVoteRPC> responseObserver) {

        LOGGER.info("requestVote in gRPC server - request: {}", request);

//        TODO fixme
        ResponseVoteRPC response = ResponseVoteRPC.newBuilder()
                    .setTerm(0)
                    .setVoteGranted(true)
                .build();

        // Send the response to the client.
        responseObserver.onNext(response);

        // Notifies the customer that the call is completed.
        responseObserver.onCompleted();
    }
}
