package io.bifroest.retentions.bootloader;

import io.bifroest.retentions.RetentionConfiguration;

public interface EnvironmentWithMutableRetentionStrategy extends EnvironmentWithRetentionStrategy {

    void setRetentions( RetentionConfiguration retentions );

}
