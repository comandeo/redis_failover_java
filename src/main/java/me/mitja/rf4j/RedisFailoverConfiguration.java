package me.mitja.rf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mitja on 3/8/14.
 */
public class RedisFailoverConfiguration {

    private String master;

    private List<String> slaves = new ArrayList<>();

    private List<String> unavailable = new ArrayList<>();

    public String getMaster() {
        return master;
    }

    public void setMaster(String master) {
        this.master = master;
    }

    public List<String> getSlaves() {
        return slaves;
    }

    public void setSlaves(List<String> slaves) {
        this.slaves = slaves;
    }

    public List<String> getUnavailable() {
        return unavailable;
    }

    public void setUnavailable(List<String> unavailable) {
        this.unavailable = unavailable;
    }

    @Override
    public String toString() {
        StringBuilder slavesBuf = new StringBuilder();
        for (String slave : slaves) {
            slavesBuf.append(slave).append(" ");
        }
        StringBuilder unavailableBuf = new StringBuilder();
        for (String unav : unavailable) {
            slavesBuf.append(unav).append(" ");
        }
        return "RedisFailoverConfiguration{" +
                "master='" + master + '\'' +
                ", slaves=" + slavesBuf.toString() +
                ", unavailable=" + unavailable.toString() +
                '}';
    }
}
