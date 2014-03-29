package me.mitja.rf4j;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by mitja on 3/9/14.
 */
public class RedisFailoverMonitor implements Watcher, Runnable {

    private static final Log log = LogFactory.getLog(RedisFailoverMonitor.class);

    public static final String DEFAULT_ZK_CONN_STRING = "localhost:2181";

    public static final int DEFAULT_ZK_TIMEOUT = 300;

    private final String RF_NODE = "/redis_failover/nodes";

    private final ZooKeeper zk;

    private String lastState;

    private RedisFailoverConfiguration lastConfiguration;

    private List<RedisClientProxy> redises = new CopyOnWriteArrayList<>();

    private boolean isRunning = false;

    public RedisFailoverMonitor() throws IOException {
        zk = new ZooKeeper(DEFAULT_ZK_CONN_STRING, DEFAULT_ZK_TIMEOUT, this);
    }

    public RedisFailoverMonitor(String zkConnectionString) throws IOException {
        zk = new ZooKeeper(zkConnectionString, DEFAULT_ZK_TIMEOUT, this);
    }

    public void attachClientProxy(RedisClientProxy redisClient) {
        redisClient.setConfiguration(getLastConfiguration());
        redises.add(redisClient);
    }

    public void detachClientProxy(RedisClientProxy redisClient) {
        redises.remove(redisClient);
    }

    @Override
    public void process(WatchedEvent we) {
        try {
            switch (we.getType()) {
                case NodeDataChanged:
                    getClusterState();
                    break;
                default:
                    log.info(we.toString());
                    zk.exists(RF_NODE, true);
            }
        } catch (KeeperException e) {
            log.error(e);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public synchronized void getClusterState() throws KeeperException, InterruptedException {
        byte[] data = zk.getData(RF_NODE, true, null);
        String newState = new String(data);
        if (lastState == null || !lastState.equals(newState)) {
            log.info("Cluster state changed: " + newState);
            lastState = newState;
            stateChanged();
        }
    }

    @Override
    public void run() {
        isRunning = true;
        while (isRunning) {
            synchronized (this) {
                try {
                    wait(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void init() {
        Stat stat = null;
        while (stat == null) {
            try {
                stat = zk.exists(RF_NODE, true);
            } catch (KeeperException e) {
                Thread.currentThread().interrupt();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (stat == null) {
                log.info("Redis failover nod not found, waiting...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        try {
            getClusterState();
        } catch (KeeperException e) {
            log.error(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() {
        isRunning = false;
    }

    public RedisFailoverConfiguration getLastConfiguration() {
        return lastConfiguration;
    }

    private void stateChanged() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            lastConfiguration = mapper.readValue(lastState, RedisFailoverConfiguration.class);
            for (RedisClientProxy redisClient : redises) {
                redisClient.configurationChanged(getLastConfiguration());
            }
        } catch (IOException e) {
            log.error("Error while parsing configuration: " + e);
        }
    }
}
