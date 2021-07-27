<h1>        6.0.1.8
</h1>
<h2>        Security Improvements
</h2>
<ul>
<li>[<a href='https://alfresco.atlassian.net/browse/PRODSEC-4422'>PRODSEC-4422</a>] -         An unprivileged attacker can run arbitrary code on the server, compromising it: RCE in rules' scripts for shared folders - script action execution no longer allows to execute scripts outside of Data Dictionary/Scripts
</li>
<li>[<a href='https://alfresco.atlassian.net/browse/PRODSEC-4421'>PRODSEC-4421</a>] -         An attacker user can gain full control of another user session. Stored cross site scripting (XSS) in Alfresco
</li>
<li>[<a href='https://alfresco.atlassian.net/browse/MNT-21611'>MNT-21611</a>] -         [Security] Blind Server Side Request Forgery - Removed HTML transformation pipelines that use LibreOffice
</li>
<li>[<a href='https://alfresco.atlassian.net/browse/MNT-20518'>MNT-20518</a>] -         [Security] CVE-2018-1000632 dom4j XML injection vulnerability (REPO-4514)
</li>
</ul>

<h1>        6.0.1.6
</h1>
<h2>        Hotfix Request
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-21736'>MNT-21736</a>] - Access to downloadable zip is not restricted to their owner
</li>
</ul>

<h1>        6.0.1.5
</h1>
<h2>        Hotfix Request
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-21619'>MNT-21619</a>] - Alfresco Content Services 6.0.1.4 does not run on JDK 8. JDK 8 is supported version for ACS 6.0.1.4
</li>
</ul>

<h1>        6.0.1.4
</h1>
<h2>        Hotfix Request
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-21573'>MNT-21573</a>] - Server-Side Template Injection
</li>
</ul>

<h1>        6.0.1.3
</h1>
<h2>        Hotfix Request
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20593'>MNT-20593</a>] - [Security] Full repository access for all unauthenticated users
</li>
</ul>

<h1>        6.0.1.2
</h1>
<h2>        Hotfix Request
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/REPO-4123'>REPO-4123</a>] - ACS 6.1 RC3 Distribution zip contains wti-bin.war instead of _vti_bin.war
</li>
</ul>

<h1>        6.0.1.1
</h1>
<h2>        Service Pack Request
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20296'>MNT-20296</a>] - Notification Email when 'following' a user displays incorrectly in Japanese
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20097'>MNT-20097</a>] - taggingStartupTrigger and downloadCleanerSchedulerAccessor jobs are missconfigured
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20042'>MNT-20042</a>] - Official Alfresco Docker Image Missing vti-bin war file
</li>
</ul>

<h2>        Bug
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19937'>MNT-19937</a>] - Update documentation for Tomcat 8.5's new SSL configurations to fix AOS for ACS 6.0/6.1
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20213'>MNT-20213</a>] - Unable to run using Java 8
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/ACE-5930'>ACE-5930</a>] - Stack 3 - Microsoft JDBC 6.2 Driver is not compatible with Open JDK 11
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/ACE-5890'>ACE-5890</a>] - 6.1: MariaDB ConnectorJ not upgraded
</li>
</ul>

<h2>        Improvement
</h2>
<ul>
<li>
<h4>Java 11 support</h4>
<p>ACS is now runnable with OpenJDK 11.0.1. It still remains compatible with JDK 1.8.</p>
<p>[<a href='https://issues.alfresco.com/jira/browse/REPO-3918'>REPO-3918</a>] - Stacks: Update ACS 6.0+ Stacks</p>
</li>
</ul>

<h1>        6.0.1
</h1>
<h2>
  Known issues
</h2>
<ul>
  <li>
    [<a href='https://issues.alfresco.com/jira/browse/MNT-20126'>MNT-20126</a>] - Workflow diagrams can not be rendered due to java 11 incompatibilities.<br/>
  </li>
</ul>                                                                                    
<h2>        Service Pack Request
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-16184'>MNT-16184</a>] -         Request to remove export/import from the help menu in the tenant console
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-16713'>MNT-16713</a>] -         “Link to file” in faceted search doesn’t work
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-17928'>MNT-17928</a>] -         Solr index fails for image files containing an unsupported date format in EXIF metadata
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-18099'>MNT-18099</a>] -         Clicking on Date Picker for Date Property Cause Browser Window to Scroll Up
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-18877'>MNT-18877</a>] -         Alfresco doesn&#39;t bootstrap if &#39;system.cache.disableMutableSharedCaches&#39; is set to true
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19611'>MNT-19611</a>] -         Transformation of a pdf document creates a tmp file which grows endlessly
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19630'>MNT-19630</a>] -         CMIS: Unable to call getAllVersions() if node is checked out and if binding type is WSDL
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19854'>MNT-19854</a>] -         Unable to open document&#39;s workflow from document details screen
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19906'>MNT-19906</a>] -         Commons-lang version for Share 6.0 went backwards from 2.6 to 2.1
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19923'>MNT-19923</a>] -         External Authentication fails after ticket expiration
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20015'>MNT-20015</a>] -         Alfresco has Detected a Manual Change Within the User Tracking Database
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20065'>MNT-20065</a>] -         Security: CVE-2018-1000613 Bouncy Castle
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20139'>MNT-20139</a>] -         CmisConnector returns wrong values for changeLogToken and hasMoreItems
</li>
</ul>
    
<h2>        Hot Fix Request
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19701'>MNT-19701</a>] -         Groups rest api call is very expensive on the DB CPU
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19864'>MNT-19864</a>] -         Rest API: &quot;groups/{groupname} /members&quot; endpoint does not honor more than 100 maxItems and pagination fails
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19881'>MNT-19881</a>] -         CLONE - Multi-threaded check-in and check-out throwing exception using CMIS API
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19919'>MNT-19919</a>] -         Scheduled replication job does not run as expected
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19955'>MNT-19955</a>] -         [5.2.2] User Authorisation fails and system goes to read-only after upgrade to a version &gt;= 5.2.2
</li>
</ul>
                                                                                                                                                                        
<h2>        Bug
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-15932'>MNT-15932</a>] -         Update message from delete user dialog
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19615'>MNT-19615</a>] -         Stored XSS through File Sharing and Improper Access Control in Workflows
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19634'>MNT-19634</a>] -         briefSummary contains sensitive details about deauthorized user
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19753'>MNT-19753</a>] -         License verification sometimes breaks bootstrap in a cluster
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19825'>MNT-19825</a>] -         ImageMagick docker transformer does not take arbitrary command line options.
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19833'>MNT-19833</a>] -         Download via v1 REST API loaded in memory
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19834'>MNT-19834</a>] -         ACS 6.0 Admin Tools --&gt; Administration Console Link Invalid
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19918'>MNT-19918</a>] -         ACS Deployment: Ingress Resources Should Use Default SSL-Redirect of Ingress Controller
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19946'>MNT-19946</a>] -         VERSIONS.md incorrect for latest stacks
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19981'>MNT-19981</a>] -         Node fails to bootstrap in large cluster
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19986'>MNT-19986</a>] -         Concurrency problem: First cluster startup can leave repository in a broken state
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19991'>MNT-19991</a>] -         Chart repo references should be https
</li>
</ul>
                                                                                

<h1>        6.0.0
</h1>                        
<h2>        Feature
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-16433'>MNT-16433</a>] -         Alfresco FTP server and Passive Mode clients
</li>
</ul>
                                                            
<h2>        Service Pack Request
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-14319'>MNT-14319</a>] -         FTSQueryParser parenthesis evaluation with negation
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-14859'>MNT-14859</a>] -         Products that reuse the alfresco-core artifact get an unwanted alfresco.log
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-15498'>MNT-15498</a>] -         No way to track &quot;Cancelled Workflow&quot;
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-15731'>MNT-15731</a>] -         d:date values do not save correctly when the year is below 1900
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-17162'>MNT-17162</a>] -         Transaction marking for the thumbnail.get.desc.xml descriptor throws error on delete
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-17919'>MNT-17919</a>] -         Slow query during ACL tracking
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-17976'>MNT-17976</a>] -         Hazelcast errors CONCURRENT_MAP_PUT and REDO_MAP_OVER_CAPACITY causing outages
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-18275'>MNT-18275</a>] -         Bulk import of .ai and .eps not detecting mimetypes
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-18420'>MNT-18420</a>] -         SVN 5.2.1 Enterprise mirror not available, request GIT Enterprise Mirror(s)
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-18666'>MNT-18666</a>] -         Node cleanup job can fail on Referential Integrity errors
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-18685'>MNT-18685</a>] -         [Security] Multiple Tomcat 7.0.x vulnerabilities
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-18840'>MNT-18840</a>] -         [Security] Stored XSS vulnerability in Admin Console Directory Management
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-18902'>MNT-18902</a>] -         [Security] CVE-2017-7525 - Jackson libraries deserialization vulnerability
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19035'>MNT-19035</a>] -         [Security] - Site Membership Information leakage
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19357'>MNT-19357</a>] -         Kerberos SSO Does Not Work for Share in Alfresco 5.1.4.1
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19472'>MNT-19472</a>] -         PDFBox 1.8.10 Returns Corrupt Stream Errors On Some Files During ExtractText Operations
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19474'>MNT-19474</a>] -         Activiti tables cause repo nodes in cluster to fail when started at the same time
</li>
</ul>
    
<h2>        Hot Fix Request
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19570'>MNT-19570</a>] -         Multi-threaded check-in and check-out throwing exception using CMIS API
</li>
</ul>
                                                                                                                                                                
<h2>        Bug
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-15095'>MNT-15095</a>] -         JobLockServiceTest testGetLockWithCallbackLocked fails on MS SQL
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-15097'>MNT-15097</a>] -         RepoAdminServiceImplTest testSimpleDynamicModelViaRepoAdminService fails on DB2
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-15425'>MNT-15425</a>] -         JobLockServiceTest testGetLockWithCallbackNormal fails on MS SQL DB build
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-16169'>MNT-16169</a>] -         OpenCmisQueryTest.testSimpleConjunction() failing on MariaDB, MySQL, MS SQLServer
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-17546'>MNT-17546</a>] -         sXSS in ContentGet v0 web script
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-18480'>MNT-18480</a>] -         Unused strings for workflow
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-18765'>MNT-18765</a>] -         Content Model admin security bypass
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-18929'>MNT-18929</a>] -         [Security] - CVE-2017-9801 - commons-email vulnerability
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-18982'>MNT-18982</a>] -         Remove dependancy to commons-beanutils from spring-webscripts
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19000'>MNT-19000</a>] -         [Security] - cloud Embedded XSS 
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19095'>MNT-19095</a>] -         Copyright info not correct Alfresco Software, Inc. © 2005-2017 needs to be replaced by Alfresco Software, Inc. © 2005-2018
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19124'>MNT-19124</a>] -         Alfresco admin console has copyright year 2017
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19127'>MNT-19127</a>] -         Security issues logged on Google&#39;s issue tracker for Pdfium
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19412'>MNT-19412</a>] -         [Security] CVE-2017-17485: incomplete fix for the CVE-2017-7525 deserialization flaw
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19431'>MNT-19431</a>] -         Security: CVE-2016-1000031: commons-fileupload-1.3.2.jar
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19514'>MNT-19514</a>] -         REST API: Deauthorized users 500
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19770'>MNT-19770</a>] -         [Security] Libreoffice SSRF (Forced HTTP GET)
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19956'>MNT-19956</a>] -         ACS HELM Deployment: Unprotected access to all resources
</li>
</ul>
                
<h2>        Improvement
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-13786'>MNT-13786</a>] -         Need support for newer Hazelcast 2.6 or 3.x libraries 
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-14586'>MNT-14586</a>] -         Support for Amazon Linux Operating System
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-17644'>MNT-17644</a>] -         support for outer join in Alfresco CMIS
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-17937'>MNT-17937</a>] -         Please update support to PostgreSQL 10
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-18333'>MNT-18333</a>] -         Support for Tomcat 8 on Alfresco 5.1/5.2 requested
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19123'>MNT-19123</a>] -         Provide buildable source code for ACS 5.2.2
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19513'>MNT-19513</a>] -         Stack review for Office and Windows versions
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19768'>MNT-19768</a>] -         FTR &amp; HTTPS
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19769'>MNT-19769</a>] -         DojoDependencyHandler - cached generated resources use significant amount of heap memory
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19771'>MNT-19771</a>] -         Tomcat 7 classloader serializes authentication ticket retrieval
</li>
</ul>
                                                                
