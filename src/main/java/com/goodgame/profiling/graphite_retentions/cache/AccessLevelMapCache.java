package com.goodgame.profiling.graphite_retentions.cache;

import java.util.Optional;


import com.goodgame.profiling.commons.statistics.cache.CacheTracker;
import com.goodgame.profiling.graphite_retentions.MutableRetentionConfiguration;

public class AccessLevelMapCache extends ThreadLocalMapCache {
    public AccessLevelMapCache(CacheTracker tracker, String name) {
        super(tracker, name);
    }


    @Override
    protected String findLevelWithoutCache( String metricName, MutableRetentionConfiguration newConfig ){
        Optional<String> accessLevelName = newConfig.findAccessLevelForMetricWithoutCache(metricName); 
        if( accessLevelName.isPresent() ){
            addToCache( metricName, accessLevelName.get() );
            return accessLevelName.get();
        }
        return null;
    }

}
