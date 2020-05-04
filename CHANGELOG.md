<h1>        7.0.0
</h1>
<h2>
  New Features
</h2>

<h1>        6.2.1
</h1>
<h2>
  New Features
</h2>

<ul>
  <li>[<a href='https://issues.alfresco.com/jira/browse/REPO-4919'>REPO-4919</a>] -         Upgrade LibreOffice from 6.1.6 to 6.3.5
</ul>

<h2>
  Security
</h2>

<h3>OpenSAML module AMP</h3>

If you run the OpenSAML module AMP file through a security Scanner like WhiteSource, Veracode SourceClear, Nexus IQ or others, 
then the following libraries will show up as having vulnerabilities.

We have verified that the vulnerabilities in these libraries cannot be exploited within the ACS repository.

<h4>not-yet-commons-ssl-0.3.9</h4>

<a href='https://vuln.whitesourcesoftware.com/vulnerability/CVE-2014-3604/'>CVE-2014-3604</a>: Certificates.java in Not Yet Commons SSL before 0.3.15 does not properly verify that the server hostname 
matches a domain name in the subject's Common Name (CN) field of the X.509 certificate, which allows man-in-the-middle 
attackers to spoof SSL servers via an arbitrary valid certificate.

Customer action required:

The vulnerable method is not directly used in the codebase so this vulnerability does not affect the ACS product.
Only customer extensions that use the method.

If your custom extension is bringing in this library, you might be affected by this vulnerability.



<h1>        6.2.0
</h1>
<h2>
  New Features
</h2>
<ul>
  <li>
    <b>Custom Transforms and Renditions</b>
    <p>Alfresco Content Services (ACS) provides a number of content
     transforms, but also allows custom transforms to be added.
    <p>It is now possible to create custom transforms that run in 
    separate processes known as T-Engines (short for Transformer
    Engines). The same engines may be used in Community and 
    Enterprise Editions. They may be directly connected to the ACS 
    repository as Local Transforms, but in the Enterprise edition there 
    is the option to include them as part of the Transform Service 
    which provides more balanced throughput and better administration 
    capabilities.
    <p>For more information see <a href='https://github.com/Alfresco/acs-packaging/blob/master/docs/custom-transforms-and-renditions.md'>Custom Transforms and Renditions</a>
  </li>
</ul>
<h2>
  Removed features
</h2>
<ul>
  <li>
    DB2 support was removed.
  </li>
</ul>

<h2>        Documentation
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20385'>MNT-20385</a>] -         Discrepancy between the compatible versions list for Outlook Integration on the Supported Platforms doc and the knowledgebase article#000014970
</li>
</ul>
                                                                
<h2>        Service Pack Request
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-16673'>MNT-16673</a>] -         Setting minimum password length for Share has no effect
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-17551'>MNT-17551</a>] -         Long rule name without spaces causes the text to overlaps on Folder Rules page and as well when you edit the rule
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-18112'>MNT-18112</a>] -         Ampersand in Wiki page name breaks search link
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-18557'>MNT-18557</a>] -         Location Of alfresco.log is Ultimately Incorrect
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-18730'>MNT-18730</a>] -         XML content encoded with UTF-16 can&#39;t be downloaded from Share&#39;s proxy
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19103'>MNT-19103</a>] -         Share Admin Tools: Link to Repository Administration Console is not always valid
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19397'>MNT-19397</a>] -         Create Folder Allows double click on Save Button resulting in Error 500
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19593'>MNT-19593</a>] -         Copy/Move dialog hidden views works on Document-Library page but not on Faceted Search Result page
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20196'>MNT-20196</a>] -         JMX Password redaction inconsistent
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20199'>MNT-20199</a>] -         Improper Output Neutralization for Logs CWE ID 117
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20200'>MNT-20200</a>] -         Improper Neutralization of Script-Related HTML Tags in a Web Page (Basic XSS) CWE ID 80
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20208'>MNT-20208</a>] -         Sensitive Cookie in HTTPS Session Without &#39;Secure&#39; Attribute CWE ID 614
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20234'>MNT-20234</a>] -         Javascript debugger causing errors when enabled 
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20296'>MNT-20296</a>] -         Notification Email when &#39;following&#39; a user displays incorrectly in Japanese 
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20305'>MNT-20305</a>] -         Oracle schema validation check failure with ojdbc7.jar version 12.1.0.2
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20314'>MNT-20314</a>] -         Untranslated strings: Title of Actions column of Table View in Document Library
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20325'>MNT-20325</a>] -         Documents with % and spaces version download issue.
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20337'>MNT-20337</a>] -         [Security] CVE-2018-16858 - LibreOffice directory traversal vulnerability
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20339'>MNT-20339</a>] -         Enterprise Admin Console - JMX Settings tool: &quot;Revert&quot; button only revert the last set of JMX properties 
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20371'>MNT-20371</a>] -         Support for IDS 1.1 in ACS 6.1
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20379'>MNT-20379</a>] -         [Security] New batch of jackson-databind vulnerabilities
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20513'>MNT-20513</a>] -         [Security] Multiple xmlrpc vulnerabilities
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20515'>MNT-20515</a>] -         [Security] CVE-2017-18197 jgraphx XXE vulnerability
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20516'>MNT-20516</a>] -         [Security] CVE-2016-2510 bsh deserialization vulnerability
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20517'>MNT-20517</a>] -         [Security] Multiple spring-core vulnerabilities
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20518'>MNT-20518</a>] -         [Security] CVE-2018-1000632 dom4j XML injection vulnerability
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20520'>MNT-20520</a>] -         [Security] Multiple cxf-core vulnerabilities
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20521'>MNT-20521</a>] -         [Security] CVE-2018-17187 proton-j MitM vulnerability
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20522'>MNT-20522</a>] -         [Security] CVE-2015-6748 jsoup XSS vulnerability
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20524'>MNT-20524</a>] -         [Security] CVE-2018-10237 guava DoS vulnerability
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20525'>MNT-20525</a>] -         [Security] Multiple tika-parsers vulnerabilities
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20529'>MNT-20529</a>] -         [Security] Multiple Bouncy Castle vulnerabilities
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20530'>MNT-20530</a>] -         [Security] CVE-2016-6814 groovy deserialization vulnerability
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20531'>MNT-20531</a>] -         [Security] CVE-2018-10936 postgresql-jdbc MitM vulnerability
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20533'>MNT-20533</a>] -         [Security] CVE-2018-11775 activemq-client MitM vulnerability
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20534'>MNT-20534</a>] -         [Security] Multiple camel-core vulnerabilities
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20535'>MNT-20535</a>] -         [Security] Multiple commons-compress vulnerabilities
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20549'>MNT-20549</a>] -         spring surf libraries are inconsistent versions between repo and share wars
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20595'>MNT-20595</a>] -         Update Hazelcast version to at least 3.11 for OpenJDK
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20670'>MNT-20670</a>] -         Renaming parent category hides its children
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20723'>MNT-20723</a>] -         Admin cannot deauthorize user - Authorization status columns missing from admin console
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20747'>MNT-20747</a>] -         [Security] CVE-2016-10750 - Hazelcast deserialization vulnerability
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20748'>MNT-20748</a>] -         [Security] CVE-2019-12086 - Jackson Databind polymorphic typing vulnerability
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20749'>MNT-20749</a>] -         [Security] Multiple dcharts-widget vulnerabilities in contained jQuery lib
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20751'>MNT-20751</a>] -         [Security] Multiple camel-jackson vulnerabilities
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20755'>MNT-20755</a>] -         Error accessing Admin Console on ACS 6.1 with External Auth configured
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20770'>MNT-20770</a>] -         Share non responsive during direct download from S3 if content store selector is also configured
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20779'>MNT-20779</a>] -         Prop cleaner job creates DB dangling references when running within close time proximity to the sync job, preventing ACS from further syncing and prop cleaning
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20821'>MNT-20821</a>] -         Wrong translation in site-profile.get_fr.properties for Site Manager(s)
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20833'>MNT-20833</a>] -         ScriptNode method createAssociation can be used where users only have consumer role
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20840'>MNT-20840</a>] -         [Security] - Persistent Cross Site Scripting
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20850'>MNT-20850</a>] -         [Security] Content can be read by malicious user, bypassing permissions
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20938'>MNT-20938</a>] -         [Security] CVE-2019-16335 - Jackson Databind polymorphic typing vulnerability
</li>
</ul>
    
<h2>        Hot Fix Request
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20507'>MNT-20507</a>] -         Some documents with special characters cannot be indexed by solr6
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20593'>MNT-20593</a>] -         [Security] Full repository access for all unauthenticated users
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20714'>MNT-20714</a>] -         [HotFix] /nodes/{nodeId}/content REST API fails for content created by a deleted user
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20734'>MNT-20734</a>] -         0kb file when using REST API nodes/{nodeID}/content in a clustered ACS
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20859'>MNT-20859</a>] -         ACS Admin Console does not display all Shards when using mTLS config between SOLR &amp; Repo
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20965'>MNT-20965</a>] -         [HotFix ]CLONE-0kb file when using REST API nodes/{nodeID}/content in a clustered ACS
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-21086'>MNT-21086</a>] -         CLONE - Some documents with special characters cannot be indexed by solr6
</li>
</ul>
                                                                                                                                                                                            
<h2>        Bug
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-18461'>MNT-18461</a>] -         CWE113 - Improper Neutralization of CRLF Sequences in HTTP Headers (&#39;HTTP Response Splitting&#39;)
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20308'>MNT-20308</a>] -         POST /authentication/versions/1/tickets incorrectly returns 403 when repository is in read-only
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20344'>MNT-20344</a>] -         No Alfresco-supplied docker image should run as root
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20407'>MNT-20407</a>] -         Rest-Api Workflow processes and task tests are failing on ACS 6.0.1.1
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20587'>MNT-20587</a>] -         Prometheus metrics are exposed without authentication
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20634'>MNT-20634</a>] -         Long rule description without spaces causes the text to overlap on when you edit the rule
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20977'>MNT-20977</a>] -         [Security] CVE-2019-12402- Commons compress vulnerability
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20978'>MNT-20978</a>] -         6.1.1: Share Webscripts page showing interpolations instead of the values
</li>
</ul>
                
<h2>        Improvement
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-19696'>MNT-19696</a>] -         REST API add order by parameter option to GET favorites
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20911'>MNT-20911</a>] -         upload tool is rounding file size when limit is set on the repository
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
    <p>Alfresco ActiveMQ Docker images: <a href='https://github.com/Alfresco/alfresco-docker-activemq'>GitHub Repo</a> <a href='https://hub.docker.com/r/alfresco/alfresco-activemq/'>DockerHub Repo</a></p>
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
<h2>        New Feature
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
                                                                
