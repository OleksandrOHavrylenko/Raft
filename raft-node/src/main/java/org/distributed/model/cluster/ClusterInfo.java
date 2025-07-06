package org.distributed.model.cluster;

import org.distributed.model.ClusterNode;
import org.distributed.model.NodeInfo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Oleksandr Havrylenko
 **/
@Component
public class ClusterInfo {
    private List<ClusterNode> otherNodes = new ArrayList<ClusterNode>();

    public int getClusterSize() {
        return otherNodes.size() + 1;
    }

    public int getMajoritySize() {
        return (getClusterSize() / 2) + 1;
    }

    public List<ClusterNode> clusterNodesNodes() {
        return new ArrayList<>(otherNodes);
    }


}
