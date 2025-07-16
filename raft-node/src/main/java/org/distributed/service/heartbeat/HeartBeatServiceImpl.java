package org.distributed.service.heartbeat;

import org.distributed.model.appendentries.AppendEntriesRequest;
import org.distributed.model.cluster.ClusterInfo;
import org.distributed.statemanager.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.distributed.statemanager.BaseState.HEARTBEAT_INTERVAL;

/**
 * @author Oleksandr Havrylenko
 **/
@Service("heartBeatService")
public class HeartBeatServiceImpl implements HeartBeatService {
    private static final Logger logger = LoggerFactory.getLogger(HeartBeatServiceImpl.class);

    private final ClusterInfo clusterInfo;
    private final ScheduledExecutorService scheduledExecutor;
    private Timer heartBeatTimer;
    private List<? extends ScheduledFuture<?>> scheduledHeartBeatHandler;

    public HeartBeatServiceImpl(final ClusterInfo clusterInfo) {
        this.clusterInfo = Objects.requireNonNull(clusterInfo);
        this.scheduledExecutor = Executors.newScheduledThreadPool(clusterInfo.getOtherNodeCount());

    }

    @Override
    public void startHeartBeatSchedule() {
        logger.info("Starting HeartBeat scheduling");

        final AppendEntriesRequest request =
                new AppendEntriesRequest(
                        clusterInfo.getCurrentNode().getTerm(), clusterInfo.getCurrentNode().getNodeId(),
                        clusterInfo.getCurrentNode().getLastLogIndex(), clusterInfo.getCurrentNode().getLastLogTerm(), List.of(), 0);
        scheduledHeartBeatHandler = clusterInfo.getOtherNodes().stream()
                .map(otherNode -> scheduledExecutor.scheduleAtFixedRate(
                        () -> otherNode.getGrpcClient().asyncHeartBeat(request), 0,  HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS)).toList();
    }

    @Override
    public void shutDownHeartBeats() {
        logger.info("Shutting down <-- HeartBeat executor.");
//        scheduledExecutor.shutdownNow();
        scheduledHeartBeatHandler.stream()
                .filter(Objects::nonNull)
                .filter(task -> !task.isDone())
                .forEach(task -> task.cancel(true));

    }

//    public void startHeartBeatTimer() {
//        stopHeartBeatTimer();
//        logger.info("Starting election timer in FollowerState");
//        final TimerTask heartBeatTask = new TimerTask() {
//            @Override
//            public void run() {
//                doHeartBeat();
//            }
//        };
//        this.heartBeatTimer = new Timer();
//        this.heartBeatTimer.scheduleAtFixedRate(heartBeatTask, 0,  HEARTBEAT_INTERVAL);
//    }
//
//    public void stopHeartBeatTimer() {
//        try {
//            logger.error("Stopping <-- HeartBeat timer on node = {}", clusterInfo.getCurrentNode().getNodeId());
//            if (this.heartBeatTimer != null) {
//                this.heartBeatTimer.cancel();
//                this.heartBeatTimer.purge();
//            }
//        } catch (Exception e) {
//            logger.error("Error stopping HeartBeat timer on node = {}", clusterInfo.getCurrentNode().getNodeId(), e);
//        }
//    }

    private void doHeartBeat() {
        if (this.clusterInfo.getNodeState() == State.LEADER) {
            final AppendEntriesRequest request =
                    new AppendEntriesRequest(
                            clusterInfo.getCurrentNode().getTerm(), clusterInfo.getCurrentNode().getNodeId(),
                            clusterInfo.getCurrentNode().getLastLogIndex(), clusterInfo.getCurrentNode().getLastLogTerm(), List.of(), 0);

            clusterInfo.getOtherNodes().stream()
                    .forEach(otherNode -> otherNode.getGrpcClient().asyncHeartBeat(request));
        }
    }
}
