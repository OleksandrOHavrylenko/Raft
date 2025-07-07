package org.distributed.service.election;

import org.distributed.model.ElectionStatus;
import org.distributed.model.NodeInfo;
import org.distributed.model.cluster.ClusterInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author Oleksandr Havrylenko
 **/
@Service("electionService")
public class ElectionServiceImpl implements ElectionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElectionServiceImpl.class);

    private final NodeInfo nodeInfo;

    private final ClusterInfo clusterInfo;

    public ElectionServiceImpl(final NodeInfo nodeInfo, final ClusterInfo clusterInfo) {
        this.nodeInfo = Objects.requireNonNull(nodeInfo);
        this.clusterInfo = Objects.requireNonNull(clusterInfo);
    }

    @Override
    public ElectionStatus startLeaderElection() {
        LOGGER.info("Starting leader election");
        nodeInfo.incrementAndGet();

        return ElectionStatus.ELECTED;
    }
}
