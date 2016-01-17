package io.bifroest.retentions;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import io.bifroest.commons.model.Metric;
import io.bifroest.commons.statistics.aggregation.ValueAggregation;

public interface RetentionConfiguration {
    ValueAggregation findFunctionForMetric( String name );
    Optional<RetentionTable> findAccessTableForMetric( String name, long timestamp );
    Optional<RetentionLevel> findAccessLevelForMetric( String name );
    Optional<RetentionLevel> getNextLevel( RetentionLevel level );
    Optional<RetentionLevel> getLevelForName( String levelname );
    Collection<RetentionLevel> getAllLevels();
    List<RetentionLevel> getAllAccessLevels();
    List<RetentionLevel> getTopologicalSort();

    default ValueAggregation findFunctionForMetric( Metric metric ) {
        return findFunctionForMetric( metric.name() );
    }

    default Optional<RetentionTable> findAccessTableForMetric( Metric metric ) {
        return findAccessTableForMetric( metric.name(), metric.timestamp() );
    }

    default Optional<RetentionLevel> findAccessLevelForMetric( Metric metric ) {
        return findAccessLevelForMetric( metric.name() );
    }

    default Optional<RetentionLevel> getLevelForMetric( Metric metric ) {
        return getLevelForName( metric.name() );
    }

}
