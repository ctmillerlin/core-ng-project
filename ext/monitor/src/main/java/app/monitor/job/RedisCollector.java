package app.monitor.job;

import core.framework.internal.stat.Stats;
import core.framework.redis.Redis;

import java.text.NumberFormat;
import java.util.Map;

/**
 * @author neo
 */
public class RedisCollector implements Collector {
    private final Redis redis;
    private final double highMemUsageThreshold;

    public RedisCollector(Redis redis, double highMemUsageThreshold) {
        this.redis = redis;
        this.highMemUsageThreshold = highMemUsageThreshold;
    }

    @Override
    public Stats collect() {
        Map<String, String> info = redis.info();

        return collect(info);
    }

    Stats collect(Map<String, String> info) {
        Stats stats = new Stats();
        double maxMem = get(info, "maxmemory");
        if (maxMem == 0) maxMem = get(info, "total_system_memory");
        stats.put("redis_mem_max", maxMem);
        double usedMem = get(info, "used_memory");
        stats.put("redis_mem_used", usedMem);
        checkHighMemUsage(stats, usedMem, maxMem);

        String keySpace = info.get("db0");  // e.g. keys=5,expires=0,avg_ttl=0
        int index = keySpace.indexOf(',');
        stats.put("redis_keys", Integer.parseInt(keySpace.substring(5, index)));

        return stats;
    }

    private void checkHighMemUsage(Stats stats, double usedMem, double maxMem) {
        double usage = usedMem / maxMem;
        if (usage >= highMemUsageThreshold) {
            NumberFormat format = NumberFormat.getPercentInstance();
            stats.warn("HIGH_MEM_USAGE", "memory usage is too high, usage=" + format.format(usage));
        }
    }

    private double get(Map<String, String> info, String key) {
        String value = info.get(key);
        if (value == null) throw new Error("can not find key from info, key=" + key);
        return Double.parseDouble(value);
    }
}