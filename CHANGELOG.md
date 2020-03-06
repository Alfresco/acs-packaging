<h1>        6.1.0.8
</h1>
<h2>
  Hot Fix Request 
</h2>
<ul>
  <li>[<a href='https://issues.alfresco.com/jira/browse/MNT-21394'>MNT-21394</a>] -         Edit in Microsoft Office from ADW opens MS Excel documents then immediately prompts that a newer version exists.
  </li>
</ul>

<h1>        6.1.0.1
</h1>
<h2>
  Security Improvements
</h2>
<ul>
  <li>[<a href='https://issues.alfresco.com/jira/browse/REPO-4226'>REPO-4226</a>] -         [Security] New batch of jackson-databind vulnerabilities
  </li>
</ul>

<h1>        6.1.0
</h1>
<h2>
  New Features
</h2>
<ul>
  <li>
    <h4>Alfresco Identity Service (SSO):</h4>
    <p>SSO using the <a href='https://github.com/Alfresco/alfresco-identity-service'>Alfresco Identity Service</a> is supported by the ACS V1 REST APIs.
    <p>Other components in ACS, such as Share and protocol access, do not (yet) support the Alfresco Identity Service.
  </li>
  <li>
    <h4>ActiveMQ:</h4>
    Alfresco ActiveMQ Docker images: <a href='https://github.com/Alfresco/alfresco-docker-activemq'>GitHub Repo</a> <a href='https://hub.docker.com/r/alfresco/alfresco-activemq/'>DockerHub Repo</a></p>
  </li>
  <li>
    <h4>Transform Service:</h4>
    <p>The <a href='docs/transform-services.md'>Transform Service</a> performs transformations for
    Alfresco Content Services remotely in scalable containers. By default it is disabled for the zip distribution
    but enabled for docker-compose and helm deployments.</p>
  </li>
  <li>
    <h4>AWS Deployment:</h4>
    <p>ACS can now be deployed on AWS EKS using helm charts.</p>
    <p>This can be done using the <a href='https://github.com/Alfresco/acs-deployment-aws'>ACS on AWS deployment project</a>.</p>
  </li>
  <li>
    <h4>Alfresco Benchmark Framework:</h4>
    <p>The benchmark framework project provides a way to run highly scalable, easy-to-run Java-based load and benchmark tests on an Alfresco instance.</p>
    <p>It comprises the following: <a href='https://github.com/Alfresco/alfresco-bm-manager'>Alfresco BM Manager</a> and Alfresco BM Drivers.</p> 
    <p>The currently provided drivers are:</p>
      <ul>
        <li><a href='https://github.com/Alfresco/alfresco-bm-load-data'>Alfresco Benchmark Load Data</a></li>
        <li><a href='https://github.com/Alfresco/alfresco-bm-rest-api'>Alfresco Benchmark Rest Api</a></li>
        <li><a href='https://github.com/Alfresco/alfresco-bm-load-users'>Alfresco Benchmark Load Users</a></li>
      </ul>	 
  </li>
  <li>
    <h4>Java 11 support</h4>
    <p>ACS is now runnable with OpenJDK 11.0.1. It still remains compatible with JDK 1.8.</p>
  </li>
  <li>
    <h4>Jave Profiling with YourKit</h4>
    <p>Integrated YourKit agent in the ACS docker image.</p>
    <p>Documented instructions on how to activate and connect with the YourKit profiller to the JVM inside the container will follow soon.</p>
  </li>
</ul>
<h2>
  Removed features
</h2>
<ul>
  <li>
    NTLM v1 was removed. "Passthru" authentication subsystem type is no longer available.
  </li>
  <li>
    CIFS was removed.
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


<h2>        Service Pack Request
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-15977'>MNT-15977</a>] -         &#39;Create document (folder) from template&#39; does not sort nor is scrollable
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-16608'>MNT-16608</a>] -         Manage Aspects does not work on Advanced Search Results page (folders)
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-16713'>MNT-16713</a>] -         “Link to file” in faceted search doesn’t work
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-18099'>MNT-18099</a>] -         Clicking on Date Picker for Date Property Cause Browser Window to Scroll Up
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-18324'>MNT-18324</a>] -         datetime property cannot be set using &#39;set property&#39; action in rule
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-18453'>MNT-18453</a>] -         Error message appears on People finder page when subscription service disabled
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-18514'>MNT-18514</a>] -         Multiple XSS vulnerabilities
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-18816'>MNT-18816</a>] -         Date Picker via Document Properties resets the folder hierarchy in Alfresco Share.
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19173'>MNT-19173</a>] -         Data-list title not readable because of high contrast in CSS theme.
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19236'>MNT-19236</a>] -         Node Browser - Select Store button not working in IE 11
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19306'>MNT-19306</a>] -         Dragging and dropping content in the breadcrumb path in the document library not working in 5.2.2
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19374'>MNT-19374</a>] -         Different UI versioning behaviour
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19449'>MNT-19449</a>] -         Web Preview from Filmstrip View option returns double slash in URL
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19519'>MNT-19519</a>] -         Deleting a file with its link asset in the same folder return wrong message
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19740'>MNT-19740</a>] -         WebDav login throws NullPointerException/HTTP 500 error when Kerberos SSO is used
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19762'>MNT-19762</a>] -         [Security] PDFBox Vulnerabilities
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19791'>MNT-19791</a>] -         [Security] Share error page shows stack trace in page source - information leakage
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19854'>MNT-19854</a>] -         Unable to open document&#39;s workflow from document details screen
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19859'>MNT-19859</a>] -         Possible to Create User called &#39;System&#39;
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19906'>MNT-19906</a>] -         Commons-lang version for Share 6.0 went backwards from 2.6 to 2.1
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19923'>MNT-19923</a>] -         External Authentication fails after ticket expiration
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19980'>MNT-19980</a>] -         Aspect.cm_attachable Displayed when Editing a Meeting Agenda Datalist
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20042'>MNT-20042</a>] -         Official Alfresco Docker Image Missing vti-bin war file
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20097'>MNT-20097</a>] -         taggingStartupTrigger and downloadCleanerSchedulerAccessor jobs are misconfigured
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20120'>MNT-20120</a>] -         Alfresco JS-API fails to return URL with SSO mode on
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20126'>MNT-20126</a>] -         Activiti: No workflow images generated in Java 11
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20162'>MNT-20162</a>] -         The “Shared Files” Button Label for the Spanish (es) Locale Doesn’t Display Correctly
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20222'>MNT-20222</a>] -         French Encoding Problem of Move to and Copy to on Search Page
</li>
</ul>
                                                                                                                                                                            
<h2>        Bug
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-14338'>MNT-14338</a>] -         Server exception should be handled better in document details page and document library page
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-15932'>MNT-15932</a>] -         Update message from delete user dialog
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19615'>MNT-19615</a>] -         Stored XSS through File Sharing and Improper Access Control in Workflows
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19634'>MNT-19634</a>] -         briefSummary contains sensitive details about deauthorized user
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19937'>MNT-19937</a>] -         Update documentation for Tomcat 8.5&#39;s new SSL configurations to fix AOS for ACS 6.0/6.1
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19981'>MNT-19981</a>] -         Node fails to bootstrap in large cluster
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19986'>MNT-19986</a>] -         Concurrency problem: First cluster startup can leave repository in a broken state
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20170'>MNT-20170</a>] -         Wrong label in faceted search title, slash is missed in Spanish translation
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20176'>MNT-20176</a>] -         Delete site fails with 403 for users from SITE_ADMINISTRATORS group
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20221'>MNT-20221</a>] -         Security: postgresql-42.2.1 (CVE-2018-10936)
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
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-18982'>MNT-18982</a>] -         Remove dependency to commons-beanutils from spring-webscripts
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
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19770'>MNT-19770</a>] -         [Security] LibreOffice SSRF (Forced HTTP GET)
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
<li>[<a href='https://issues.alfresco.com/jira/browse/REPO-3944'>REPO-3934</a>] -         Renditions: Switches for TransformServer and Local Transforms
</li>
</ul>
                                                                
