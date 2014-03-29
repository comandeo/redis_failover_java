package me.mitja.rf4j;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import redis.clients.jedis.Jedis;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by mitja on 3/29/14.
 */
public class RedisClientProxy implements InvocationHandler {

    private static final Log log = LogFactory.getLog(RedisClientProxy.class);

    private final RedisFailoverMonitor monitor;

    public static final int MAX_TRIES = 5;

    private RedisFailoverConfiguration configuration;

    private final Object lock = new Object();

    private Jedis master;

    public RedisClientProxy(RedisFailoverMonitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object res = null;
        int currentTry = 1;
        while (currentTry <= MAX_TRIES) {
            try {
                String methodName = method.getName();
                Class[] argsClasses = new Class[args.length];
                for (int i = 0; i < argsClasses.length; ++i) {
                    switch ((args[i].getClass().getName())) {
                        case "java.lang.Integer":
                            argsClasses[i] = int.class;
                            break;
                        case "java.lang.Long":
                            argsClasses[i] = long.class;
                            break;
                        case "java.lang.Double":
                            argsClasses[i] = double.class;
                            break;
                        default:
                            argsClasses[i] = args[i].getClass();
                    }
                }
                Method jedisMethod = Class.forName("redis.clients.jedis.Jedis").getMethod(methodName, argsClasses);
                res = jedisMethod.invoke(getMaster(), args);
                break;
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof NoSuchMethodError) {
                    throw cause;
                } else {
                    log.warn("Cannot reach redis " + currentTry + " time: " + cause.getMessage());
                    synchronized (this) {
                        wait(3000);
                    }
                    currentTry++;
                }
            }
            if (currentTry > MAX_TRIES) {
                throw new RedisNotAvailable();
            }
        }
        return res;
    }

    public void configurationChanged(RedisFailoverConfiguration newConfiguration) {
        synchronized (lock) {
            try {
                if (configuration == null) {
                    configuration = newConfiguration;
                    URI masterUri = new URI("tcp://" + configuration.getMaster());
                    master = new Jedis(masterUri.getHost(), masterUri.getPort());
                } else {
                    if (!configuration.getMaster().equals(newConfiguration.getMaster())) {
                        URI masterUri = new URI("tcp://" + newConfiguration.getMaster());
                        master = new Jedis(masterUri.getHost(), masterUri.getPort());
                        configuration.setMaster(newConfiguration.getMaster());
                    }
                }
            } catch (URISyntaxException e) {
                log.error("Incorrect configuration: " + e);
            }
            log.info("Configuration set: " + configuration);
        }
    }

    public void setConfiguration(RedisFailoverConfiguration configuration) {
        configurationChanged(configuration);
    }

    private Jedis getMaster() {
        synchronized (lock) {
            return master;
        }
    }

}
