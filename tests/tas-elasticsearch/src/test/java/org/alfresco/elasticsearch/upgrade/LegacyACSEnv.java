package org.alfresco.elasticsearch.upgrade;

import org.testcontainers.containers.GenericContainer;

abstract class LegacyACSEnv extends BaseACSEnv
{
    protected LegacyACSEnv(Config cfg)
    {
        super(cfg);
    }

    public abstract ACSEnv upgradeToCurrent();
}
