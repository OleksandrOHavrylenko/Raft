package org.distributed.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.distributed.model.ClusterNode;
import org.distributed.model.appendentries.AppendEntriesRequest;
import org.distributed.model.appendentries.AppendEntriesResponse;
import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;
import org.distributed.statemanager.StateManager;
import org.distributed.stubs.*;
import org.distributed.util.SpringContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
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
    private ClusterNode clusterNode;

    public GrpcClientImpl(final String host, final int port, final ClusterNode clusterNode) {
        this.host = host;
        this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        this.voteBlockingStub = VoteServiceGrpc.newBlockingStub(this.channel);
        this.asyncAppendEntriesStub = AppendEntriesServiceGrpc.newStub(this.channel);
        this.clusterNode = Objects.requireNonNull(clusterNode);
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
    public void asyncHeartBeat(final AppendEntriesRequest request) {
        logger.debug("HeartBeat --> sent to host: {} , with Term = {}", this.host, request.term());

        List<RequestAppendEntriesRPC.LogEntry> logEntries = request.entries().stream()
                .map(item -> RequestAppendEntriesRPC.LogEntry.newBuilder()
                        .setIndex(item.id())
                        .setTerm(item.term())
                        .setCommand(item.message())
                        .build())
                .toList();

        RequestAppendEntriesRPC.Builder builder = RequestAppendEntriesRPC.newBuilder();
        logEntries.forEach(builder::addEntries);
        builder
                .setTerm(request.term())
                .setLeaderId(request.leaderId())
                .setPrevLogIndex(request.prevLogIndex())
                .setPrevLogTerm(request.prevLogTerm())
                .setLeaderCommit(request.leaderCommit())
                .setIsHb(request.isHb());
        RequestAppendEntriesRPC heartBeatRequest = builder.build();

        StreamObserver<ResponseAppendEntriesRPC> responseObserver = new StreamObserver<ResponseAppendEntriesRPC>() {

            @Override
            public void onNext(ResponseAppendEntriesRPC value) {
                logger.debug("Response for HeatBeat: {} in onNext", value);
                AppendEntriesResponse response = new AppendEntriesResponse(value.getTerm(), value.getSuccess());

                StateManager stateManager = Objects.requireNonNull(SpringContext.getBean(StateManager.class));
                stateManager.onHeartBeatResponse(response, clusterNode);

            }

            @Override
            public void onError(Throwable t) {
                logger.error("HeartBeat to host: {} - failed", host, t);
            }

            @Override
            public void onCompleted() {
                logger.debug("HeartBeat to host: {} successful", host);
            }
        };
        asyncAppendEntriesStub.withDeadlineAfter(HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS).appendEntries(heartBeatRequest, responseObserver);
    }

    @Override
    public void asyncReplicateLog(AppendEntriesRequest request, CountDownLatch writeConcernLatch, boolean waitForReady) {
        List<RequestAppendEntriesRPC.LogEntry> logEntries = request.entries().stream()
                .map(item -> RequestAppendEntriesRPC.LogEntry.newBuilder()
                        .setIndex(item.id())
                        .setTerm(item.term())
                        .setCommand(item.message())
                        .build())
                .toList();

        RequestAppendEntriesRPC.Builder builder = RequestAppendEntriesRPC.newBuilder();
        logEntries.forEach(builder::addEntries);
        builder
            .setTerm(request.term())
            .setLeaderId(request.leaderId())
            .setPrevLogIndex(request.prevLogIndex())
            .setPrevLogTerm(request.prevLogTerm())
            .setLeaderCommit(request.leaderCommit())
            .setIsHb(request.isHb());
        RequestAppendEntriesRPC requestRpc = builder.build();

        StreamObserver<ResponseAppendEntriesRPC> responseObserver = new StreamObserver<ResponseAppendEntriesRPC>() {

            @Override
            public void onNext(ResponseAppendEntriesRPC value) {
                logger.info("Response from : {} node: {}", host, value);
                AppendEntriesResponse response = new AppendEntriesResponse(value.getTerm(), value.getSuccess());

                StateManager stateManager = Objects.requireNonNull(SpringContext.getBean(StateManager.class));
                stateManager.onHeartBeatResponse(response, clusterNode);
            }

            @Override
            public void onError(Throwable t) {
                logger.error("Replication of LogEntry to {}: Failed: {}", host, Status.fromThrowable(t));
                if(writeConcernLatch != null) {
                    writeConcernLatch.countDown();
                }
            }

            @Override
            public void onCompleted() {
                logger.info("Replication of items to {}: Completed", host);
                if(writeConcernLatch != null) {
                    writeConcernLatch.countDown();
                }
            }
        };

        if (waitForReady) {
            asyncAppendEntriesStub.withWaitForReady().appendEntries(requestRpc, responseObserver);
        } else {
            asyncAppendEntriesStub.appendEntries(requestRpc, responseObserver);
        }
    }
}
