package org.alfresco.tas;

import java.util.Arrays;

public enum DatabaseType
{
    POSTGRES_DB("posgres"),
    MYSQL_DB("mysql");

    private final String type;

    DatabaseType(String type)
    {
        this.type = type;
    }

    public String getType()
    {
        return this.type;
    }

    public static DatabaseType from(String type)
    {
        return Arrays.stream(DatabaseType.values())
                     .filter(engine -> engine.getType().equals(type.toLowerCase()))
                     .findFirst()
                     .orElseThrow(() -> new IllegalArgumentException("Database of type + '" + type + "' not defined."));
    }
}
