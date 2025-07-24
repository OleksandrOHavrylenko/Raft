package org.distributed.service.message;

import org.distributed.model.ClusterNode;
import org.distributed.model.appendentries.AppendEntriesRequest;
import org.distributed.model.appendentries.LogEntry;
import org.distributed.model.cluster.ClusterInfo;
import org.distributed.model.dto.LogItem;
import org.distributed.repository.LogRepository;
import org.distributed.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Oleksandr Havrylenko
 **/
@Service("messageService")
public class MessageServiceImpl implements MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MessageServiceImpl.class);

    private final LogRepository logRepository;
    private final ClusterInfo clusterInfo;
    private final ExecutorService executor;


    public MessageServiceImpl(final LogRepository logRepository,
                              final ClusterInfo clusterInfo) {
        this.logRepository = Objects.requireNonNull(logRepository);
        this.clusterInfo = Objects.requireNonNull(clusterInfo);
        this.executor = Executors.newFixedThreadPool(clusterInfo.getOtherNodeCount());
    }

    @Override
    public LogItem append(String message) {
        int id = IdGenerator.id();
        final LogItem logItem = new LogItem(id, message, clusterInfo.getCurrentNode().getTerm());
        logRepository.add(logItem);

        CountDownLatch writeConcernLatch = new CountDownLatch(clusterInfo.getMajoritySize());

        final AppendEntriesRequest appendEntriesRequest =
                new AppendEntriesRequest(
                        clusterInfo.getCurrentNode().getTerm(),
                        clusterInfo.getCurrentNode().getNodeId(),
                        clusterInfo.getCurrentNode().getPrevLogIndex(),
                        clusterInfo.getCurrentNode().getPrevLogTerm(),
                        List.of(new LogEntry(logItem.id(), logItem.term(), logItem.message())),
                        clusterInfo.getCurrentNode().getLeaderCommit());

        for (ClusterNode replica : clusterInfo.getOtherNodes()) {
            executor.submit(() -> replica.asyncSendMessage(appendEntriesRequest, writeConcernLatch, true));
        }
        try {
            writeConcernLatch.await();
        } catch (InterruptedException e) {
            logger.info("RuntimeException occurred while replication", e);
        }
        IdGenerator.setLeaderCommit(id);
        return logItem;
    }

    @Override
    public List<String> getMessages() {
        return logRepository.getAll(IdGenerator.getLast());
    }

    @Override
    public LogItem getLastMessage() {
        if (IdGenerator.getLast() == 0) {
            return null;
        }
        return logRepository.getLassMessage();
    }

    @Override
    public void saveMessages(LogItem logItem) {
        logRepository.add(logItem);
    }
}
