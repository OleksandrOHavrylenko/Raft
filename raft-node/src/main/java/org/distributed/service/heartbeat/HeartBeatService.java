package org.distributed.service.heartbeat;

/**
 * @author Oleksandr Havrylenko
 **/
public interface HeartBeatService {
    void startHeartBeatSchedule();
    void shutDownHeartBeats();
//    void startHeartBeatTimer();
//    void stopHeartBeatTimer();
}
