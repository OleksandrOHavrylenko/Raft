package org.distributed.service.heartbeat;

/**
 * @author Oleksandr Havrylenko
 **/
public interface HeartBeatService {
    public void startHeartBeatSchedule();
    void shutDownHeartBeats();
}
