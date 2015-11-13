package com.goodgame.profiling.graphite_retentions.cache;

import java.lang.ref.WeakReference;
import java.util.Objects;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.goodgame.profiling.commons.statistics.cache.CacheTracker;
import com.goodgame.profiling.graphite_retentions.MutableRetentionConfiguration;
import com.goodgame.profiling.graphite_retentions.RetentionConfiguration;

public abstract class ThreadLocalMapCache {
    
    private static final Logger log = LogManager.getLogger();
    
    private final String name;
    private final CacheTracker tracker;
    
    private WeakReference<MutableRetentionConfiguration> createdFromWeak;
    private LRUMap<String, String> cache;
    
    public ThreadLocalMapCache( CacheTracker tracker, String name ){
        this.name = name;
        this.tracker = Objects.requireNonNull( tracker );
        this.createdFromWeak = new WeakReference<>( null );
        cache = new LRUMap<String, String>( 50 );
    }
    
    public String name(){
        return this.name;
    }
    
    public String get( String  metricName, MutableRetentionConfiguration currentRetentionConfiguration ){
        RetentionConfiguration createdFrom = createdFromWeak.get();
        if( createdFrom == currentRetentionConfiguration ){
            String cachedName = cache.get(metricName);
            if( cachedName == null ){
                return findLevelWithoutCache( metricName, currentRetentionConfiguration );
            }
            else {
                tracker.cacheHit(cache.size(), cache.maxSize());
                return log.exit( cachedName );
            }
        }
        else {
            //invalidate
            cache.clear();
            createdFromWeak = new WeakReference<> ( currentRetentionConfiguration );
            return findLevelWithoutCache( metricName, currentRetentionConfiguration );
        }
    }
    
    protected void addToCache( String metric, String value ){
        cache.put(metric, value);
        tracker.cacheMiss(cache.size(), cache.maxSize());
  
    }
    
    protected abstract String findLevelWithoutCache( String metricName, MutableRetentionConfiguration newConfig );

}
