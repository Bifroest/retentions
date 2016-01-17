package io.bifroest.retentions;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

import org.apache.commons.collections4.map.LinkedMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import io.bifroest.retentions.cache.AccessLevelMapCache;
import io.bifroest.retentions.cache.AggregationFunctionMapCache;
import io.bifroest.retentions.cache.ThreadLocalMapCache;
import io.bifroest.commons.statistics.aggregation.ValueAggregation;
import io.bifroest.commons.statistics.aggregation.ValueAggregationFactory;
import io.bifroest.commons.statistics.cache.CacheTracker;

public class MutableRetentionConfiguration implements RetentionConfiguration {
    private static final Logger log = LogManager.getLogger();
    private static final Marker AGGREGATION_CONFIG_MARKER = MarkerManager.getMarker( "AGGREGATION_CONFIG_MARKER" );

    private final Map<Pattern, String> functionMap;
    private final Map<Pattern, String> accessLevelMap;

    private final Map<String, RetentionLevel> levels;

    private LinkedList<RetentionLevel> topologicalSort;
    
    private static volatile CacheTracker functionCacheTracker;
    private static volatile CacheTracker accessLevelCacheTracker;


    private static ThreadLocal<ThreadLocalMapCache> functionMapCache = new ThreadLocal<ThreadLocalMapCache>() {
        protected ThreadLocalMapCache initialValue() {
            log.trace( "Creating new FunctionMapCache" );
            if ( functionCacheTracker == null ) {
                synchronized ( this ) {
                    if ( functionCacheTracker == null ) {
                        functionCacheTracker = CacheTracker.storingIn( "ThreadLocalRetentionFunctionMapCache" );
                    }
                }
            }
            return new AggregationFunctionMapCache( functionCacheTracker, "RetentionFunctionMapCache" );
        }
    };
    
    private static ThreadLocal<ThreadLocalMapCache> accessLevelCache = new ThreadLocal<ThreadLocalMapCache>() {
        protected ThreadLocalMapCache initialValue() {
            log.trace( "Creating new FunctionMapCache" );
            if ( accessLevelCacheTracker == null ) {
                synchronized ( this ) {
                    if ( accessLevelCacheTracker == null ) {
                        accessLevelCacheTracker = CacheTracker.storingIn( "ThreadLocalAccessLevelCache" );
                    }
                }
            }
            return new AccessLevelMapCache(accessLevelCacheTracker, "AccessLevelMapCache" );
        }
    };
    
    

    private static final Map<String, ValueAggregationFactory> aggregationFunctionFactories;

    static {
        // counted valueaggregations by hand and added some
        aggregationFunctionFactories = new HashMap<String, ValueAggregationFactory>( 10 );
        for ( ValueAggregationFactory factory : ServiceLoader.load( ValueAggregationFactory.class ) ) {
            aggregationFunctionFactories.put( factory.getFunctionName().toLowerCase(), factory );
        }
    }

    public MutableRetentionConfiguration( ) {
        // function map and strategy map need to preserve order.
        this.functionMap = new LinkedMap<>();
        this.accessLevelMap = new LinkedMap<>();
        this.levels = new HashMap<>();
    }

    public void addFunctionEntry( String regex, String function ) {
        log.trace( "Adding new functionEntry {} {}", regex, function );
        functionMap.put( Pattern.compile( regex ), function.toLowerCase() );
    }

    public void addAccessLevelEntry( String regex, String name ) {
        log.trace( "Adding new accessLevelEntry {} {}", regex, name );
        accessLevelMap.put( Pattern.compile( regex ), name );

    }

    public void addLevel( RetentionLevel level ) {
        log.trace( "Adding new Level {}", level.name() );
        levels.put( level.name(), level );
    }

    @Override
    public Optional<RetentionLevel> getNextLevel( RetentionLevel level ) {
        log.entry( level );
        RetentionLevel nextLevel = levels.get( level.next() );
        return log.exit( Optional.ofNullable( nextLevel ) );
    }

    public Optional<RetentionLevel> getLevelForName( String name ) {
        log.entry( name );
        RetentionLevel level = levels.get( name );
        return log.exit( Optional.ofNullable( level ) );
    }

    @Override
    public ValueAggregation findFunctionForMetric( String name ) {
        return log.exit( aggregationFunctionFactories.get( functionMapCache.get().get( name, this ) ).createAggregation() );
    }

    public String findAggregationNameThroughATonOfRegexes( String name ) {
        log.entry( name );
        for ( Entry<Pattern, String> entry : functionMap.entrySet() ) {
            if ( entry.getKey().matcher( name ).find() ) {
                if ( aggregationFunctionFactories.containsKey( entry.getValue() ) ) {
                    return log.exit( entry.getValue() );
                } else {
                    log.warn( AGGREGATION_CONFIG_MARKER, entry.getValue() + " is not a supported aggregation method - using average" );
                    return log.exit( "average" );
                }
            }
        }
        log.debug( AGGREGATION_CONFIG_MARKER, "No aggregation function defined for " + name + " - using average" );
        return log.exit( "average" );
    }

    @Override
    public Optional<RetentionTable> findAccessTableForMetric( String name, long timestamp ) {
        log.entry( name, timestamp );
        Optional<RetentionLevel> level = findAccessLevelForMetric( name );
        if ( level.isPresent() ) {
            return Optional.of( new RetentionTable( level.get(), level.get().indexOf( timestamp ) ) );
        }
        log.warn( "No AccessTable found for {} {}", name, timestamp );
        return log.exit( Optional.empty() );
    }

	@Override
	public Optional<RetentionLevel> findAccessLevelForMetric(String name) {
	   String accessLevelName = accessLevelCache.get().get(name, this);
	   if(levels.containsKey(accessLevelName) ){
	       return Optional.of(levels.get(accessLevelName));
	   }
	   return Optional.empty();
	}
	
	public Optional<String> findAccessLevelForMetricWithoutCache( String metricName ){
	    log.entry(metricName);
	    for( Entry<Pattern, String> entry : accessLevelMap.entrySet() ){
	        if( entry.getKey().matcher(metricName).find() && levels.containsKey( entry.getValue() ) ){
	            return log.exit(Optional.of(levels.get(entry.getValue()).name()));
	        }
	    }
        log.warn("No AccessLevel found for {}", metricName);
        return log.exit(Optional.empty());
	}

	@Override
	public Collection<RetentionLevel> getAllLevels() {
		return levels.values();
	}
	
	@Override
	public List<RetentionLevel> getAllAccessLevels() {
		List<RetentionLevel> accessLevels = new LinkedList<RetentionLevel>();
		for( Entry<Pattern, String> entry : accessLevelMap.entrySet() ){
			if( !accessLevels.contains(getLevelForName(entry.getValue()).get()) ){
				accessLevels.add(getLevelForName(entry.getValue()).get());
			}
		}
		return accessLevels;
	}

    @Override
    public List<RetentionLevel> getTopologicalSort() {
        if ( topologicalSort != null ) {
            return topologicalSort;
        }
        log.trace( "Trying to create TopologicalSort on {} ", levels );
        topologicalSort = new LinkedList<>();
        Set<RetentionLevel> unvisited = new HashSet<>( levels.values() );
        Stack<RetentionLevel> levelStack = new Stack<>();
        while ( unvisited.size() > 0 ) {
            levelStack.push( unvisited.iterator().next() );
            while ( !levelStack.isEmpty() ) {
                RetentionLevel level = levelStack.pop();
                if ( levelStack.contains( level ) ) {
                    throw new IllegalArgumentException();
                }
                if ( unvisited.contains( level ) ) {
                    levelStack.push( level );
                    unvisited.remove( level );
                    if ( levels.containsKey( level.next() ) ) {
                        levelStack.push( levels.get( level.next() ) );
                        continue;
                    }
                    levelStack.pop();
                }
                if ( !topologicalSort.contains( level ) ) {
                    topologicalSort.addFirst( level );
                }
            }
        }
        return log.exit( topologicalSort );
    }

}
