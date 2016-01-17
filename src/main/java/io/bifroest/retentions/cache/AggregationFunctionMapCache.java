package io.bifroest.retentions.cache;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.bifroest.retentions.MutableRetentionConfiguration;
import io.bifroest.commons.statistics.cache.CacheTracker;

public class AggregationFunctionMapCache extends ThreadLocalMapCache {
    public AggregationFunctionMapCache(CacheTracker tracker, String name) {
        super(tracker, name);
    }

    private static final Logger log = LogManager.getLogger();

    @Override
    protected String findLevelWithoutCache( String metricName, MutableRetentionConfiguration newConfig ){
        String functionName = newConfig.findAggregationNameThroughATonOfRegexes( metricName );
        addToCache( metricName, functionName );
        return log.exit( functionName );
    }
}
