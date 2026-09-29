package org.alfresco.elasticsearch.upgrade;

/** ACS-12862: credentials a permission-filtered search runs as. */
record SearchUser(String username, String password)
{}
