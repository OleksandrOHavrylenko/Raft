package org.distributed.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.distributed.model.appendentries.AppendEntriesRequest;
import org.distributed.model.appendentries.AppendEntriesResponse;
import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;
import org.distributed.statemanager.StateManager;
import org.distributed.stubs.*;
import org.distributed.util.SpringContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.distributed.statemanager.BaseState.HEARTBEAT_INTERVAL;

/**
 * @author Oleksandr Havrylenko
 **/
public class GrpcClientImpl implements GrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(GrpcClientImpl.class);
    private String host;
    private final ManagedChannel channel;
    private VoteServiceGrpc.VoteServiceBlockingStub voteBlockingStub;
    private AppendEntriesServiceGrpc.AppendEntriesServiceStub asyncAppendEntriesStub;

    public GrpcClientImpl(final String host, final int port) {
        this.host = host;
        this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        this.voteBlockingStub = VoteServiceGrpc.newBlockingStub(this.channel);
        this.asyncAppendEntriesStub = AppendEntriesServiceGrpc.newStub(this.channel);
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
            responseVoteRPC = voteBlockingStub
                    .withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS)
                    .requestVote(request);
        } catch (StatusRuntimeException e) {
            logger.warn("RPC requestVote to host: {} - failed: {}", this.host, e.getStatus());
            return new VoteResponse(0, false);
        }

        return new VoteResponse(responseVoteRPC.getTerm(), responseVoteRPC.getVoteGranted());
    }

    @Override
    public void asyncHeartBeat(final AppendEntriesRequest appendEntriesRequest) {
        logger.info("HeartBeat --> sent to host: {} , with Term = {}", this.host, appendEntriesRequest.term());

        RequestAppendEntriesRPC heartBeatRequest = RequestAppendEntriesRPC.newBuilder()
                .setTerm(appendEntriesRequest.term())
                .build();

        StreamObserver<ResponseAppendEntriesRPC> responseObserver = new StreamObserver<ResponseAppendEntriesRPC>() {

            @Override
            public void onNext(ResponseAppendEntriesRPC value) {
                logger.info("Response for HeatBeat: {} in onNext", value);
                AppendEntriesResponse response = new AppendEntriesResponse(value.getTerm(), value.getSuccess());

                StateManager stateManager = Objects.requireNonNull(SpringContext.getBean(StateManager.class));
                stateManager.onHeartBeatResponse(response);

            }

            @Override
            public void onError(Throwable t) {
                logger.warn("HeartBeat to host: {} - failed", host, t);
            }

            @Override
            public void onCompleted() {
                logger.info("HeartBeat to host: {} successful", host);
            }
        };
        asyncAppendEntriesStub.withDeadlineAfter(HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS).appendEntries(heartBeatRequest, responseObserver);
    }
}
