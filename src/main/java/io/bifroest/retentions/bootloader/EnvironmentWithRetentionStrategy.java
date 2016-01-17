package io.bifroest.retentions.bootloader;

import io.bifroest.retentions.RetentionConfiguration;

import io.bifroest.commons.boot.interfaces.Environment;

public interface EnvironmentWithRetentionStrategy extends Environment {

    RetentionConfiguration retentions();

}
