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
    private NodeInfo currentNode = new NodeInfo("node1", "localhost", 9092);
    private List<ClusterNode> otherNodes = new ArrayList<ClusterNode>();

    public NodeInfo getCurrentNode() {
        return currentNode;
    }

    public int getClusterSize() {
        return otherNodes.size() + 1;
    }

    public int getMajoritySize() {
        return (getClusterSize() / 2) + 1;
    }

//   TODO fixme
    public int getOtherNodeCount() {
//        return otherNodes.size();
        return 2;
    }

    public List<ClusterNode> getOtherNodes() {
        return new ArrayList<>(otherNodes);
    }


}
