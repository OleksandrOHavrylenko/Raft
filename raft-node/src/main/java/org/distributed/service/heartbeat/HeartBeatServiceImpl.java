package org.distributed.service.heartbeat;

import org.distributed.model.appendentries.AppendEntriesRequest;
import org.distributed.model.cluster.ClusterInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Oleksandr Havrylenko
 **/
@Service("heartBeatService")
public class HeartBeatServiceImpl implements HeartBeatService {
    private static final Logger logger = LoggerFactory.getLogger(HeartBeatServiceImpl.class);
    public static final int HEARTBEAT_INTERVAL = 500;

    private final ClusterInfo clusterInfo;
    private final ScheduledExecutorService scheduledExecutor;

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
        clusterInfo.getOtherNodes().stream()
                .forEach(otherNode -> scheduledExecutor.scheduleWithFixedDelay(
                        () -> otherNode.getGrpcClient().asyncHeartBeat(request),0,  HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS));
    }

    public void shutDownHeartBeats() {
        logger.info("Shutting down HeartBeat executor.");
        scheduledExecutor.shutdownNow();
    }
}
