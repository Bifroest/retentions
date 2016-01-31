package io.bifroest.retentions;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.lang.annotation.Retention;
import java.util.Optional;

import io.bifroest.commons.model.Interval;
import org.junit.Test;

public class RetentionTableTest {

    @Test
    public void testMalformedTableNameCannotCreateTable() {
        RetentionConfiguration configuration = mock(RetentionConfiguration.class);
        String malformedTableName = "garfield";

        assertThat(RetentionTable.canCreateTable(malformedTableName, configuration), is(false));
        verifyZeroInteractions(configuration);
    }

    @Test
    public void testUnconfiguredTablesCannotBeCreated() {
        RetentionConfiguration configuration = mock(RetentionConfiguration.class);

        String wellformedTableName = "g" + "level" + RetentionTable.SEPARATOR_OF_MADNESS + "42";
        when(configuration.getLevelForName("level")).thenReturn(Optional.empty());

        assertThat(RetentionTable.canCreateTable(wellformedTableName, configuration), is(false));
    }

    @Test
    public void testConfiguredAndWellFormedNameCanBeCreated() {
        String someLevelName = "precise";
        int someSecondsPerDataPoint = 20;
        int someBlockNumber = 10;
        int someBlockSize = 40;
        String noNextLevel = null;

        RetentionLevel retentionLevel = new RetentionLevel(someLevelName, someSecondsPerDataPoint, someBlockNumber, someBlockSize, noNextLevel);
        String wellformedTableName = "g" + "level" + RetentionTable.SEPARATOR_OF_MADNESS + "42";

        RetentionConfiguration configuration = mock(RetentionConfiguration.class);
        when(configuration.getLevelForName("level")).thenReturn(Optional.of(retentionLevel));

        assertThat(RetentionTable.canCreateTable(wellformedTableName, configuration), is(true));
    }

    @Test
    public void computesTableNames() {
        String someLevelName = "precise";
        int someSecondsPerDataPoint = 20;
        int someBlockNumber = 10;
        int someBlockSize = 40;
        String noNextLevel = null;
        int someBlockIndex = 42;

        RetentionLevel retentionLevel = new RetentionLevel(someLevelName, someSecondsPerDataPoint, someBlockNumber, someBlockSize, noNextLevel);

        RetentionTable subject = new RetentionTable(retentionLevel, someBlockIndex);

        assertThat(subject.tableName(), is("g" + someLevelName + RetentionTable.SEPARATOR_OF_MADNESS + String.valueOf(someBlockIndex)));
    }

    @Test
    public void hasAnAssignedInterval() {
        String someLevelName = "precise";
        long someSecondsPerDataPoint = 20;
        long someBlockNumber = 10;
        long someBlockSize = 40;
        String noNextLevel = null;
        long someBlockIndex = 42;

        RetentionLevel retentionLevel = new RetentionLevel(someLevelName, someSecondsPerDataPoint, someBlockNumber, someBlockSize, noNextLevel);

        RetentionTable subject = new RetentionTable(retentionLevel, someBlockIndex);

        assertThat(subject.getInterval(), is(new Interval(someBlockIndex * someBlockSize, someBlockIndex * someBlockSize + someBlockSize)));
    }

    @Test
    public void containsTheRightValues() {
        String someLevelName = "precise";
        long someSecondsPerDataPoint = 20;
        long someBlockNumber = 10;
        long someBlockSize = 40;
        String noNextLevel = null;
        long someBlockIndex = 42;

        RetentionLevel retentionLevel = new RetentionLevel(someLevelName, someSecondsPerDataPoint, someBlockNumber, someBlockSize, noNextLevel);

        RetentionTable subject = new RetentionTable(retentionLevel, someBlockIndex);

        assertThat(subject.contains(someBlockIndex * someBlockSize - 1), is(false)); // just 1 before start
        assertThat(subject.contains(someBlockIndex * someBlockSize), is(true)); // start is inclusive
        assertThat(subject.contains(someBlockIndex * someBlockSize + someBlockSize - 1), is(true)); // just 1 before end
        assertThat(subject.contains(someBlockIndex * someBlockSize + someBlockSize), is(false)); // end is exclusive
    }
}
