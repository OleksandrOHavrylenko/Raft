package org.distributed.service.message;

import org.distributed.exceptions.NoMajorityException;
import org.distributed.model.ClusterNode;
import org.distributed.model.appendentries.AppendEntriesRequest;
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
import java.util.concurrent.TimeUnit;

import static org.distributed.statemanager.BaseState.REPLICATE_TIMEOUT;

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

        CountDownLatch writeConcernLatch = new CountDownLatch(clusterInfo.getMajoritySize() - 1);

        final AppendEntriesRequest appendEntriesRequest =
                new AppendEntriesRequest(
                        clusterInfo.getCurrentNode().getTerm(),
                        clusterInfo.getCurrentNode().getNodeId(),
                        clusterInfo.getCurrentNode().getPrevLogIndex(),
                        clusterInfo.getCurrentNode().getPrevLogTerm(),
                        List.of(logItem),
                        clusterInfo.getCurrentNode().getLeaderCommit());

        for (ClusterNode replica : clusterInfo.getOtherNodes()) {
            executor.submit(() -> replica.asyncSendMessage(appendEntriesRequest, writeConcernLatch, false));
        }
        boolean countDownIsZero = false;
        try {
            countDownIsZero = writeConcernLatch.await(REPLICATE_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.info("TimeOut occurred while replication", e);
        }
        if (!countDownIsZero) {
            IdGenerator.setId(IdGenerator.getPreviousIndex());
            throw new NoMajorityException(clusterInfo.getNodeState());
        } else {
            IdGenerator.setLeaderCommit(id);
            return logItem;
        }
    }

    @Override
    public List<LogItem> getMessages() {
        return logRepository.getAll(IdGenerator.getLeaderCommit() + 1);
    }

    @Override
    public LogItem getLastMessage() {
        if (IdGenerator.getNextIndex() == 0) {
            return null;
        }
        return logRepository.getMessageByIndex(IdGenerator.getLeaderCommit());
    }

    @Override
    public void saveMessages(LogItem logItem) {
        logRepository.add(logItem);
    }

    @Override
    public long getTermByIndex(int index) {
        LogItem logItem = logRepository.getMessageByIndex(index);
        return logItem == null ? 0 : logItem.term();
    }

    @Override
    public LogItem getByIndex(int index) {
        return logRepository.getMessageByIndex(index);
    }
}
