package org.distributed.service.election;

import org.distributed.model.ElectionStatus;

/**
 * @author Oleksandr Havrylenko
 **/
public interface ElectionService {
    ElectionStatus startLeaderElection();
}
