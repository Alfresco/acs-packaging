package org.alfresco.tas;

import java.util.Arrays;

public enum DatabaseType
{
    POSTGRESQL_DB("postgresql", "org.postgresql.Driver", "jdbc:postgresql://postgres:5432/alfresco"),
    MYSQL_DB("mysql", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://mysql:3306/alfresco"),
    MARIA_DB("mariadb", "org.mariadb.jdbc.Driver", "jdbc:mariadb://mariadb:3306/alfresco"),
    ORACLE_DB("oracle", "oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@repodb:1521/PDB1");

    private final String type;
    private final String driver;
    private final String url;

    DatabaseType(String type, String driver, String url)
    {
        this.type = type;
        this.driver = driver;
        this.url = url;
    }

    public String getType()
    {
        return this.type;
    }

    public String getDriver()
    {
        return driver;
    }

    public String getUrl()
    {
        return url;
    }

    public static DatabaseType from(String type)
    {
        return Arrays.stream(DatabaseType.values())
                     .filter(database -> database.getType().equals(type.toLowerCase()))
                     .findFirst()
                     .orElseThrow(() -> new IllegalArgumentException("Database of type + '" + type + "' not defined."));
    }
}
