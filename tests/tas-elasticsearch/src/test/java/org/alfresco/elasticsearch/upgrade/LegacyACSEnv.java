package org.alfresco.elasticsearch.upgrade;

abstract class LegacyACSEnv extends BaseACSEnv
{
    protected LegacyACSEnv(Config cfg)
    {
        super(cfg);
    }

    public abstract ACSEnv upgradeToCurrent();
}
