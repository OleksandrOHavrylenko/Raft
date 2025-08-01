package org.distributed.statemanager;

import org.distributed.model.ClusterNode;
import org.distributed.model.appendentries.AppendEntriesRequest;
import org.distributed.model.appendentries.AppendEntriesResponse;
import org.distributed.model.cluster.ClusterInfo;
import org.distributed.model.dto.LogItem;
import org.distributed.model.vote.VoteRequest;
import org.distributed.model.vote.VoteResponse;
import org.distributed.service.heartbeat.HeartBeatService;
import org.distributed.service.message.MessageService;
import org.distributed.stubs.ResponseVoteRPC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * @author Oleksandr Havrylenko
 **/
public class LeaderState extends BaseState{
    private static final Logger logger = LoggerFactory.getLogger(LeaderState.class);
    private final State currentState = State.LEADER;
    private final HeartBeatService heartBeatService;
    private final MessageService messageService;

    public LeaderState(final StateManager stateManager, final HeartBeatService heartBeatService,
                       final MessageService messageService, final ClusterInfo clusterInfo) {
        super(stateManager, messageService, clusterInfo);
        this.heartBeatService = Objects.requireNonNull(heartBeatService);
        this.messageService = Objects.requireNonNull(messageService);
    }

    @Override
    public void onStart() {
        logger.info("Starting Leader at NodeId: {}! with Term = {}", clusterInfo.getCurrentNode().getNodeId(), clusterInfo.getCurrentNode().getTerm());
        clusterInfo.setNextIndexToFollowers(clusterInfo.getCurrentNode().getLastLogIndex() + 1);
        heartBeatService.startHeartBeatSchedule();
    }

    @Override
    public void onAppendEntriesResponse(AppendEntriesResponse appendEntriesResponse, ClusterNode clusterNode) {
        if (appendEntriesResponse.term() > clusterInfo.getCurrentNode().getTerm()) {
            this.heartBeatService.shutDownHeartBeats();
            clusterInfo.getCurrentNode().setTerm(appendEntriesResponse.term());
            this.nextState(State.FOLLOWER);
        }
        if(appendEntriesResponse.success()) {
            logger.info("Here because success response from {}", clusterNode);
            if (clusterNode.getNextIndex() < clusterInfo.getCurrentNode().getNextLogIndex()) {
                clusterNode.getAndIncrementNextIndex();
            }
        }else {
            clusterNode.decrementAndGetNextIndex();
            logger.info("Here because fail response from {}, new nextIndex = {}", clusterNode, clusterNode.getNextIndex());
        }
        logger.info("New nextIndex={} for node Leader = {}", clusterNode.getNextIndex() , clusterInfo.getCurrentNode().getNextLogIndex());
        logger.info("New nextIndex={} for node = {}", clusterNode.getNextIndex() , clusterNode);
    }

    @Override
    public VoteResponse onRequestVote(final VoteRequest voteRequest) {

        if (clusterInfo.getCurrentNode().getTerm() > voteRequest.term() ||
                (clusterInfo.getCurrentNode().getTerm() == voteRequest.term() &&
                        clusterInfo.getCurrentNode().getLastLogIndex() > voteRequest.lastLogIndex())) {
            return new VoteResponse(clusterInfo.getCurrentNode().getTerm(), false);
        } else if (voteRequest.term() > clusterInfo.getCurrentNode().getTerm()) {
            this.clusterInfo.getCurrentNode().setTerm(voteRequest.term());
            this.heartBeatService.shutDownHeartBeats();
            this.nextState(State.FOLLOWER);
            return new VoteResponse(clusterInfo.getCurrentNode().getTerm(), false);
        }

        return new VoteResponse(clusterInfo.getCurrentNode().getTerm(), false);
    }

    @Override
    public void onResponseVote(final ResponseVoteRPC responseVoteRPC, final ClusterNode clusterNode) {
        logger.debug("Nothing to do onResponseVote in LeaderState");
    }

    @Override
    public AppendEntriesResponse onReplicateRequest(final AppendEntriesRequest request) {
        logger.info("Leader --> received ReplicateRequest from Leader, nothing to do");
        if (clusterInfo.getCurrentNode().getTerm() > request.term()) {
            return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), false);
        }
        if (request.term() > clusterInfo.getCurrentNode().getTerm()) {
            this.heartBeatService.shutDownHeartBeats();
            clusterInfo.getCurrentNode().setTerm(request.term());
            this.nextState(State.FOLLOWER);
            return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), false);
        }
        return new AppendEntriesResponse(clusterInfo.getCurrentNode().getTerm(), false);
    }

    @Override
    public LogItem append(String message) {
        return messageService.append(message);
    }

    @Override
    public List<LogItem> getMessages() {
        return messageService.getMessages();
    }

    @Override
    public void nextState(State nextState) {
        logger.info("Leader goes to nextState = {}", nextState);
        this.stateManager.setState(nextState);
    }

    @Override
    public State getCurrentState() {
        return this.currentState;
    }

    @Override
    public void onStop() {
        this.heartBeatService.shutDownHeartBeats();
    }
}
