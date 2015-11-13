package com.goodgame.profiling.graphite_retentions;

import org.junit.Test;

import com.goodgame.profiling.commons.model.Interval;
import com.goodgame.profiling.commons.model.Metric;
import static org.junit.Assert.*;

public class MetricSetTest {
    @Test( timeout = 1000 )
    public void test() {
        MetricSet subject = new MetricSet( "foo", new Interval( 0, 2 ), 1 );
        subject.setValue( 0, 1 );
        subject.setValue( 1, Double.NaN );
        for ( @SuppressWarnings( "unused" ) Metric metric : subject ) {
        }
    }

    @Test
    public void testThatItStaysEqualDuringJSONSerialization() {
        MetricSet subject = new MetricSet( "foo", new Interval( 0, 10 ), 1 );
        subject.setValue( 0, 1 );
        subject.setValue( 1, Double.NaN );
        subject.setValue( 5, 5 );
        MetricSet subjectAfterSerialization = MetricSet.fromJSON( subject.toJSON() );
        assertEquals( subject, subjectAfterSerialization );
        assertEquals( subject.size(), subjectAfterSerialization.size() );
        assertEquals( subject.interval(), subjectAfterSerialization.interval() );
    }
}
