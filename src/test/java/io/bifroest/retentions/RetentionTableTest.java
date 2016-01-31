package io.bifroest.retentions;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class RetentionTableTest {

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
}
