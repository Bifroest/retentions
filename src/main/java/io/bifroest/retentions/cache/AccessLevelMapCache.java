package io.bifroest.retentions.cache;

import java.util.Optional;

import io.bifroest.retentions.MutableRetentionConfiguration;
import io.bifroest.commons.statistics.cache.CacheTracker;


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
