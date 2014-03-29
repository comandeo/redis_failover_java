redis_failover_java
===================

Java client for a redis cluster managed by [redis_failover](https://github.com/ryanlecompte/redis_failover) node manager.
It has [jedis](https://github.com/xetorthio/jedis) under the hood.

According to redis_failover architecture, redis_failover_java discovers cluster state from Zookeeper, get the current master,
and send all commands to the master. If the master is down for a while, it will keep trying until new master is set.

A short example is provided below:

```java
RedisClientsManager manager = new RedisClientsManager("some.zookeeper.com:2181");
RedisClient client = manager.getClient();
client.set("myKey", "myValue");
client.expire("myKey", 5);
manager.release();
```


### TODO

- discover slaves
- send read commands to slaves while write commands to the master

