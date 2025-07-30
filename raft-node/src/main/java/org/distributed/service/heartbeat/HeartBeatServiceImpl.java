package org.distributed.service.heartbeat;

import org.distributed.model.ClusterNode;
import org.distributed.model.NodeInfo;
import org.distributed.model.appendentries.AppendEntriesRequest;
import org.distributed.model.cluster.ClusterInfo;
import org.distributed.model.dto.LogItem;
import org.distributed.service.message.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
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
    private List<? extends ScheduledFuture<?>> scheduledHeartBeatHandler;
    private MessageService messageService;

    public HeartBeatServiceImpl(final ClusterInfo clusterInfo, final MessageService messageService) {
        this.clusterInfo = Objects.requireNonNull(clusterInfo);
        this.scheduledExecutor = Executors.newScheduledThreadPool(clusterInfo.getOtherNodeCount());
        this.messageService = Objects.requireNonNull(messageService);
    }

    @Override
    public void startHeartBeatSchedule() {
        logger.debug("Starting HeartBeat scheduling");


        scheduledHeartBeatHandler = clusterInfo.getOtherNodes().stream()
                .map(otherNode -> onSchedule(otherNode))
                .toList();
    }

    private ScheduledFuture<?> onSchedule(ClusterNode clusterNode) {

        return scheduledExecutor.scheduleAtFixedRate(
                () -> clusterNode.getGrpcClient().asyncHeartBeat(createRequest(clusterNode)), 0, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
    }

    private AppendEntriesRequest createRequest(ClusterNode clusterNode) {

        int nextIndex = clusterInfo.getCurrentNode().getNextLogIndex();
        int prevLogIndex = nextIndex - 1;
        long prevLogTerm = messageService.getTermByIndex(prevLogIndex);
        List<LogItem> entries = List.of();
        int leaderCommit = clusterInfo.getCurrentNode().getLeaderCommit();

        if (clusterNode.getNextIndex() != clusterInfo.getCurrentNode().getNextLogIndex()) {
            nextIndex = clusterNode.getNextIndex();
            prevLogIndex = nextIndex - 1;
            prevLogTerm = messageService.getTermByIndex(prevLogIndex);
            LogItem logItem = messageService.getByIndex(prevLogIndex);
            entries = logItem == null ? List.of() : List.of(logItem);
            leaderCommit = Math.max(0, prevLogIndex);
        }

        final AppendEntriesRequest request =
                new AppendEntriesRequest(
                        clusterInfo.getCurrentNode().getTerm(),
                        clusterInfo.getCurrentNode().getNodeId(),
                        prevLogIndex,
                        prevLogTerm,
                        entries,
                        leaderCommit,
                        true);
        return request;
    }

    @Override
    public void shutDownHeartBeats() {
        logger.info("Shutting down <-- HeartBeat executor.");
        scheduledHeartBeatHandler.stream()
                .filter(Objects::nonNull)
                .filter(future -> !future.isDone())
                .forEach(future -> future.cancel(true));
    }
}
