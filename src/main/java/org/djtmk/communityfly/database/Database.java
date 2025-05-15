package org.djtmk.communityfly.database;

import java.util.UUID;

public interface Database {
    void initialize();
    double getFlightTime(UUID uuid);
    void setFlightTime(UUID uuid, double time);
    void close();
}