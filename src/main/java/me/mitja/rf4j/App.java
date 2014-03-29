package me.mitja.rf4j;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * Created by mitja on 3/29/14.
 */
public class App
{
    private static final Log log = LogFactory.getLog(App.class);

    public static void main( String[] args ) throws IOException, InterruptedException {
        RedisClientsManager manager = new RedisClientsManager();
        RedisClient client = manager.getClient();
        for (int i=0; i<100; ++i) {
            client.set("myKey", "myValue");
            client.expire("myKey", 5);
            log.info("Key set, sleeping");
            Thread.sleep(5000);
        }
        manager.release();
    }
}
