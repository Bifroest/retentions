package com.goodgame.profiling.graphite_retentions;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.collections4.iterators.FilterIterator;

import com.goodgame.profiling.commons.model.Interval;
import com.goodgame.profiling.commons.model.Metric;
import com.goodgame.profiling.commons.serialize.json.JSONSerializable;
import java.util.Objects;
import org.json.JSONArray;
import org.json.JSONObject;

public class MetricSet extends AbstractSet<Metric> implements JSONSerializable {
    private final String name;
    private final long startTimestamp;
    private final long step;
    private final double[] values;

    public MetricSet( String name, Interval interval, long step ) {
        if ( interval.start() % step != 0 ) {
            throw new IllegalArgumentException( String.format(
                    "start (%d) must divide the length of interval(%d)",
                    step,
                    interval.start()
                    ) );
        }
        if ( interval.end() % step != 0 ) {
            throw new IllegalArgumentException( String.format(
                    "end (%d) must divide the length of interval(%d)",
                    step,
                    interval.end()
                    ) );
        }
        if ( ( interval.end() - interval.start() ) / step > Integer.MAX_VALUE ) {
            throw new IndexOutOfBoundsException( String.format(
                    "requested interval is too large! (%d > %d)",
                    ( interval.end() - interval.start() ) / step,
                    Integer.MAX_VALUE
                    ) );
        }

        this.name = name;
        this.startTimestamp = interval.start();
        this.step = step;
        this.values = new double[(int)( ( interval.end() - interval.start() ) / step )];
        for ( int i = 0; i < this.values.length; i++ ) {
            this.values[i] = Double.NaN;
        }
    }

    private MetricSet( String name, long startTimestamp, long step, double[] values ) {
        this.name = name;
        this.startTimestamp = startTimestamp;
        this.step = step;
        this.values = values;
    }

    public void setValue( int index, double value ) {
        this.values[index] = value;
    }

    @Override
    public boolean add( Metric e ) {
        if ( e.timestamp() % step != 0 ) {
            throw new IllegalArgumentException( String.format(
                    "step(%d) must divide timestamp(%d)",
                    step,
                    e.timestamp()
                    ) );
        }
        int index = (int)( ( e.timestamp() - this.startTimestamp ) / this.step );
        if ( index < 0 || this.values.length <= index ) {
            throw new IllegalArgumentException( String.format(
                    "timestamp(%d) outside declared interval",
                    e.timestamp()
                    ) );
        }
        this.values[index] = e.value();
        return true;
    }

    public double[] values() {
        return values;
    }

    public long step() {
        return this.step;
    }

    public Interval interval() {
        return new Interval( startTimestamp, startTimestamp + ( step * values.length ) );
    }

    @Override
    public Iterator<Metric> iterator() {
        return new FilterIterator<>(
                new Iterator<Metric>() {
                    private int nextIndex;

                    @Override
                    public boolean hasNext() {
                        return nextIndex < values.length;
                    }

                    @Override
                    public Metric next() {
                        Metric ret = new Metric( name, startTimestamp + nextIndex * step, values[nextIndex] );
                        nextIndex++;
                        return ret;
                    }
                }, metric -> !Double.isNaN( metric.value() ) );
    }

    @Override
    public int size() {
        // The cast to int is safe, because values is an array, which
        // cannot be larger than Integer.MAX_VALUE
        return (int)Arrays.stream( values ).filter( value -> !Double.isNaN( value ) ).count();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + Objects.hashCode(this.name);
        hash = 23 * hash + (int) (this.startTimestamp ^ (this.startTimestamp >>> 32));
        hash = 23 * hash + (int) (this.step ^ (this.step >>> 32));
        hash = 23 * hash + Arrays.hashCode(this.values);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MetricSet other = (MetricSet) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (this.startTimestamp != other.startTimestamp) {
            return false;
        }
        if (this.step != other.step) {
            return false;
        }
        if (!Arrays.equals(this.values, other.values)) {
            return false;
        }
        return true;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject result = new JSONObject();
        result.put( "name", name );
        result.put( "startTimestamp", startTimestamp );
        result.put( "step", step );
        JSONArray jsonValues = new JSONArray();
        for (int i = 0; i < values.length; i++) {
            if ( Double.isNaN( values[i] ) ) {
                jsonValues.put( JSONObject.NULL );
            } else {
                jsonValues.put(i, values[i]);
            }
        }
        result.put( "values", jsonValues );
        return result;
    }
    
    public static MetricSet fromJSON( JSONObject json ) {
        JSONArray jsonValues = json.getJSONArray( "values" );
        double[] values = new double[jsonValues.length()];
        for (int i = 0; i < values.length; i++) {
            if ( jsonValues.isNull(i) ) {
                values[i] = Double.NaN;
            } else {
                values[i] = jsonValues.getDouble(i);
            }
        }
        return new MetricSet(
                json.getString( "name" ),
                json.getInt( "startTimestamp"),
                json.getInt( "step" ),
                values );
    }
}
