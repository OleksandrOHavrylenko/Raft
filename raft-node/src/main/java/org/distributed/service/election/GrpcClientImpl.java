package org.distributed.service.election;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;
import org.distributed.stubs.RequestVoteRPC;
import org.distributed.stubs.ResponseVoteRPC;
import org.distributed.stubs.VoteServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author Oleksandr Havrylenko
 **/
public class GrpcClientImpl implements GrpcClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcClientImpl.class);
    private String host;
    private final ManagedChannel channel;
    private VoteServiceGrpc.VoteServiceBlockingStub voteBlockingStub;

    public GrpcClientImpl(final String host, final int port) {
        this.host = host;
        this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        this.voteBlockingStub = VoteServiceGrpc.newBlockingStub(this.channel);
    }

    @Override
    public VoteResponse requestVote(final VoteRequest voteRequest, final long timeoutMillis) {
        final RequestVoteRPC request = RequestVoteRPC.newBuilder()
                .setTerm(voteRequest.term())
                .setCandidateId(voteRequest.candidateId())
                .setLastLogIndex(voteRequest.lastLogIndex())
                .setLastLogTerm(voteRequest.lastLogTerm())
                .build();

        ResponseVoteRPC responseVoteRPC;
        try {
            responseVoteRPC = voteBlockingStub.withDeadlineAfter(timeoutMillis, TimeUnit.MICROSECONDS)
                    .requestVote(request);
        } catch (StatusRuntimeException e) {
            LOGGER.warn("RPC requestVote to host: {} - failed: {}", this.host, e.getStatus());
            return new VoteResponse(0, false);
        }

        return new VoteResponse(responseVoteRPC.getTerm(), responseVoteRPC.getVoteGranted());
    }
}
