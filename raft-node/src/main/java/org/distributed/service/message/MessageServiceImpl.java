package org.distributed.service.message;

import org.distributed.model.cluster.ClusterInfo;
import org.distributed.model.dto.LogItem;
import org.distributed.repository.LogRepository;
import org.distributed.util.IdGenerator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * @author Oleksandr Havrylenko
 **/
@Service("messageService")
public class MessageServiceImpl implements MessageService {

    private final LogRepository logRepository;
    private final ClusterInfo clusterInfo;

    public MessageServiceImpl(final LogRepository logRepository, final ClusterInfo clusterInfo) {
        this.logRepository = Objects.requireNonNull(logRepository);
        this.clusterInfo = Objects.requireNonNull(clusterInfo);
    }

    @Override
    public LogItem append(String message) {
        final LogItem logItem = new LogItem(IdGenerator.id(), message, clusterInfo.getCurrentNode().getTerm());
        logRepository.add(logItem);
        return logItem;
    }

    @Override
    public List<String> getMessages() {
        return logRepository.getAll(IdGenerator.getLast());
    }
}
