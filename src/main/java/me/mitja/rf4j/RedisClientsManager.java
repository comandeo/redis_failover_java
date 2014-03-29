package me.mitja.rf4j;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by mitja on 3/29/14.
 */
public class RedisClientsManager {

    private static final Log log = LogFactory.getLog(RedisClientsManager.class);

    private static RedisFailoverMonitor monitor;

    private static final Set<RedisClientsManager> managers = new HashSet<>();

    private static Thread monitorThread;

    private static final Object lock = new Object();

    public RedisClientsManager() throws IOException {
        log.info("Creating client manager");
        if (monitor == null) {
            log.info("No monitor, starting one");
            monitor = new RedisFailoverMonitor();
            monitor.init();
            monitorThread = new Thread(monitor);
            monitorThread.start();
            synchronized (lock) {
                managers.add(this);
            }
        }
    }

    public RedisClient getClient() {
        RedisClientProxy redisClientProxy = new RedisClientProxy(monitor);
        monitor.attachClientProxy(redisClientProxy);
        return (RedisClient) Proxy.newProxyInstance(RedisClient.class.getClassLoader(), new Class[]{RedisClient.class}, redisClientProxy);
    }

    public void release() {
        log.info("Client manager released");
        synchronized (lock) {
            managers.remove(this);
            if (managers.isEmpty()) {
                log.info("All managers released, stopping monitor");
                monitor.shutdown();
            }
        }
    }

}
