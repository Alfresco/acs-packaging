# Component Versions

This file lists the recommended components for this Service Pack. Use it along with the [Supported Platforms Matrix](http://docs.alfresco.com/7.0/concepts/supported-platforms-ACS.html) and the [Alfresco Documentation](https://docs.alfresco.com/7.0/concepts/ch-upgrade.html) to apply selective component upgrades.

#### Alfresco Content Services @project.version@

##### Alfresco Applications
| Component | Recommended | Also Available |
|---|---|---|
| Alfresco Content Services | @project.version@ |
| Alfresco Enterprise Repository ± | @dependency.alfresco-enterprise-repo.version@ |
| Alfresco Share | @dependency.alfresco-enterprise-share.version@ |
| Alfresco Search Services | 2.0.14 |
| Alfresco Insight Engine | 2.0.14 |
| Alfresco Desktop Sync Service | @alfresco.desktop-sync.version@ |
| Alfresco Transform Service | @dependency.alfresco-transform-service.version@ |
| Alfresco Transform Core | @dependency.alfresco-transform-core.version@ |
| Alfresco Shared File Store | @dependency.alfresco-transform-service.version@ |
| Document Transformation Engine | @dependency.alfresco-transform-core.version@ |
| Alfresco File Transfer Receiver | 7.0.0 |
| Alfresco Module Management Tool | @dependency.alfresco-community-repo.version@ |
| Alfresco Spring Encryptor | @dependency.alfresco-spring-encryptor.version@ |
± Based on Alfresco Community Repository @dependency.alfresco-community-repo.version@

##### Alfresco Modules and Integrations
| Component                                        | Recommended                                    | Also Available |
|--------------------------------------------------|------------------------------------------------|----------------|
| Alfresco Share Services                          | @dependency.alfresco-community-repo.version@   |
| Alfresco Google Docs Integration                 | @alfresco.googledrive.version@                 |
| Alfresco Governance Services (Repository)        | @dependency.alfresco-enterprise-repo.version@  |
| Alfresco Governance Services (Share)             | @dependency.alfresco-enterprise-share.version@ |
| Alfresco Intelligence Services                   | @alfresco.ais.version@                         |
| Alfresco Office Services                         | @alfresco.aos-module.version@                  |
| Alfresco Content Connector for Salesforce        | @alfresco.salesforce-connector.version@        |
| Alfresco S3 Connector                            | @alfresco.s3connector.version@                 |
| Alfresco Content Connector for Azure             | @alfresco.azure-connector.version@             |
| Alfresco Outlook Integration                     | @alfresco.outlook.version@                     |
| Alfresco Connector for Hyland Experience Insight | @alfresco.hxinsight-connector.version@         |
| Alfresco SDK                                     | 5.0                                            | 4.2            |

##### Microsoft Office Suite
| Component | Recommended | Also Available |
|---|---|---|
| MS Office | Microsoft 365 | 2016 |

##### Operating Systems
| Component | Recommended | Also Available |
|---|---|---|
| Red Hat Enterprise Linux | 8.4 | 8.2 |
| Windows Server | 2019 |
| CentOS | CentOS 7.9 (2009) | CentOS 8.3 |
| Ubuntu | 20.04 | 18.04 |
| Amazon Linux | 2 | |

##### Databases
| Component | Recommended | Also Available |
|---|---|---|
| MySQL | 8.0 | |
| MS SQL Server | 2022 | 2019 |
| Oracle | 23c | |
| PostgreSQL | 16.6 | 15.10, 14.15 |
| MariaDB | 10.11 | 10.6, 10.5 |
| Amazon Aurora PostgreSQL | 15.5 | |

##### Database Connectors
| Component | Recommended | Also Available |
|---|---|---|
| MySQL connector | @dependency.mysql.version@ |
| MariaDB Java Client | 2.7.2 |
| PostgreSQL | @dependency.postgresql.version@ |
| Oracle JDBC ojdbc8 | 19.11.0.0 | 19.3.0.0, 12.2.0.1 |
| Microsoft JDBC Driver | 9.2.1.jre11 | 8.4.1.jre11 |

##### Application Servers
| Component | Recommended | Also Available |
|---|---|---|
| Tomcat | 10.1.X |

##### Client Operating Systems
| Component | Recommended | Also Available |
|---|---|---|
| Windows x64 | 10 | 7 |
| Mac OSX | 10.12 |

##### Client browsers
| Component | Recommended | Also Available |
|---|---|---|
| Mozilla Firefox | 86 |
| Microsoft Edge | Latest |
| IE | 11 |
| Chrome | 89 |
| Safari | 14 |

##### Java
| Component | Recommended | Also Available |
|---|---|---|
| OpenJDK | 17.0.4 |  |
| Amazon Corretto | 17 |  |

##### Message Broker
| Component | Recommended | Also Available |
|---|---|---|
| ActiveMQ | 5.18.3 | 5.17.6 |
