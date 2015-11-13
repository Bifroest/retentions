package com.goodgame.profiling.graphite_retentions.bootloader;

import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.kohsuke.MetaInfServices;

import com.goodgame.profiling.commons.boot.interfaces.Subsystem;
//import com.goodgame.profiling.commons.exception.ConfigurationException;
import com.goodgame.profiling.commons.statistics.units.SI_PREFIX;
import com.goodgame.profiling.commons.statistics.units.TIME_UNIT;
import com.goodgame.profiling.commons.statistics.units.parse.TimeUnitParser;
import com.goodgame.profiling.commons.statistics.units.parse.UnitParser;
import com.goodgame.profiling.commons.systems.SystemIdentifiers;
import com.goodgame.profiling.commons.systems.configuration.ConfigurationObserver;
import com.goodgame.profiling.commons.systems.configuration.EnvironmentWithJSONConfiguration;
import com.goodgame.profiling.commons.systems.configuration.InvalidConfigurationException;
import com.goodgame.profiling.graphite_retentions.MutableRetentionConfiguration;
import com.goodgame.profiling.graphite_retentions.RetentionConfiguration;
import com.goodgame.profiling.graphite_retentions.RetentionLevel;
import com.goodgame.profiling.graphite_retentions.RetentionTable;

@MetaInfServices
public class RetentionSystem<E extends EnvironmentWithJSONConfiguration & EnvironmentWithMutableRetentionStrategy> implements Subsystem<E> {

    private static final Logger log = LogManager.getLogger();

    private static final UnitParser parser = new TimeUnitParser( SI_PREFIX.ONE, TIME_UNIT.SECOND );

    @Override
    public String getSystemIdentifier() {
        return SystemIdentifiers.RETENTION;
    }

    @Override
    public Collection<String> getRequiredSystems() {
        return Collections.emptyList();
    }

    @Override
    public void configure( JSONObject configuration ) {
        // empty
    }

    @Override
    public void boot( final E environment ) throws Exception {
        environment.setRetentions( createRetentions( environment.getConfiguration() ) );

        environment.getConfigurationLoader().subscribe( new ConfigurationObserver() {

            @Override
            public void handleNewConfig( JSONObject config ) {
                try {
                    environment.setRetentions( createRetentions( config ) );
                } catch( InvalidConfigurationException e ) {
                    log.error( e );
                }
            }

        } );
    }

    @Override
    public void shutdown( E environment ) {
        // Nothing to shutdown
    }

    private static void checkTableNamePart( String partDesc, String tableNamePart, boolean force ) throws InvalidConfigurationException {
        if ( tableNamePart.isEmpty() ) {
            throw new InvalidConfigurationException( partDesc + " " + tableNamePart + " cannot be empty" );
        }

        if ( tableNamePart.length() > 19 ) {
            throw new InvalidConfigurationException( partDesc + " " + tableNamePart + " cannot belonger than 8 characters" );
        }
        if ( !StringUtils.isAlphanumeric( tableNamePart ) ) {
            throw new InvalidConfigurationException( partDesc + " " + tableNamePart + " must be alphanumerical" );
        }
        if ( !tableNamePart.toLowerCase().equals( tableNamePart ) ) {
            throw new InvalidConfigurationException( partDesc + " " + tableNamePart + " must not contain capital letters" );
        }
        if ( !force && tableNamePart.contains( RetentionTable.SEPARATOR_OF_MADNESS ) ) {
            throw new InvalidConfigurationException( partDesc + " " + tableNamePart + " cannot contain " + RetentionTable.SEPARATOR_OF_MADNESS );
        }
        if ( !force && tableNamePart.contains( "X" ) ) {
            throw new InvalidConfigurationException( partDesc + " " + tableNamePart + " cannot contain X or it might form "
                    + RetentionTable.SEPARATOR_OF_MADNESS );
        }

        if ( !force && tableNamePart.contains( "0" ) ) {
            throw new InvalidConfigurationException( partDesc + " " + tableNamePart + " cannot contain 0 or it might form "
                    + RetentionTable.SEPARATOR_OF_MADNESS );
        }
    }

    private static RetentionConfiguration createRetentions( JSONObject config ) throws InvalidConfigurationException {
        JSONObject retention = config.getJSONObject( "retention" );
        
        MutableRetentionConfiguration retentions = new MutableRetentionConfiguration();

        JSONObject levels = retention.getJSONObject( "levels" );

        JSONArray names = levels.names();

        if ( names == null || names.length() == 0 ) {
            throw new InvalidConfigurationException( "No retention levels specified" );
        }
        
        for ( int i = 0; i < names.length(); i++ ) {
            JSONObject level = levels.getJSONObject( names.getString( i ) );
            String name = names.getString( i ).toLowerCase();
            boolean force = level.optBoolean( "force" );
            log.debug( "name: {}, force: {}", name, force );
            checkTableNamePart( "Retention Level Name", name, force );
            long frequency = parser.parse( level.getString( "frequency" ) ).longValue();
            long blockSize = parser.parse( level.getString( "blockSize" ) ).longValue();
            long blocks = level.getLong( "blocks" ) + 1; // Add head
            String next = level.optString( "next", null );
            if ( next != null ) {
                next = next.toLowerCase();
            }
            retentions.addLevel( new RetentionLevel( name, frequency, blocks, blockSize, next ) );
        }

        retentions.getTopologicalSort();

        JSONArray patterns = retention.getJSONArray( "patterns" );
        for ( int i = 0; i < patterns.length(); i++ ) {
            JSONObject pattern = patterns.getJSONObject( i );
            String regex = pattern.getString( "pattern" );
            if ( pattern.has( "method" ) ) {
                log.debug( "Adding function {} for regex {}", pattern.getString( "method" ), regex );
                retentions.addFunctionEntry( regex, pattern.getString( "method" ) );
            }
            if ( pattern.has( "accessLevel" ) ) {
                log.debug( "Adding access level {} for regex {}", pattern.getString( "accessLevel" ).toLowerCase(), regex );
                retentions.addAccessLevelEntry( regex, pattern.getString( "accessLevel" ).toLowerCase() );
            }

        }

        return retentions;
    }

}
