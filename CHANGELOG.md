<h1>        6.1.0
</h1>
<h2>
  New Features (TODO)
</h2>
<ul>
  <li>
    ActiveMQ:
  </li>
  <li>
    Transform Service:
  </li>
</ul>
<h2>
  Deprecations
</h2>
<ul>
  <li>
    TransformService and RenditionService: All Java APIs related to TransformService and RenditionService have been deprecated; the ability to perform arbitrary transformations will be phased out as the new DBP Transform Service takes effect.  Renditions can be triggered using the existing repository REST API but will be processed asynchronously using the new services.<br/>
  </li>
</ul>
<h2>
  Known issues
</h2>
<ul>
  <li>
    Due to the changes to the RenditionService the Media Management AMP is not supported yet.<br/>
  </li>
  <li>
    [<a href='https://issues.alfresco.com/jira/browse/MNT-20126'>MNT-20126</a>] - Workflow diagrams can not be rendered due to java 11 incompatibilities.<br/>
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
                                                                
