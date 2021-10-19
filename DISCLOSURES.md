please refer to the main version onf this file in: https://github.com/Alfresco/acs-packaging 

### October 2021

The latest releases of Alfresco products alfresco-content-services and Share User Interface contains fixes of three security vulnerabilities, two of them have been reported by external security researchers, see the credits below.

The first issue reported was an evasion in the XSS prevention filter used to validate HTML input coming from the client's browser. The XSS prevention filter have been replaced with the specialized library from OWASP: Java HTML Sanitizer https://github.com/OWASP/java-html-sanitizer/  likely preventing other possible future evasions being discovered. This issue was assigned provisional number CVE-2021-41791 Cross-Site Scripting (XSS). Affected versions include:
org.alfresco:share - versions from (including) 5.0.x.x up to (including) 5.2.7.11
org.alfresco:share - versions from (including) 6.0.x.x up to (including) 6.2.2.4
org.alfresco:community-share - prior to (including) 7.0
org.alfresco:share - versions from (including) 6.1.x.x up to (including) 6.1.1.2
org.alfresco:share -versions from (including)  6.0.x.x up to (including) 6.0.1.2
org.alfresco:share - versions 7.0.1 , 7.0.0.2 , 7.0.0.1 , 7.0

The second issue fixed was in the business logic around script execution. Additional validation was implemented preventing edge cases that could lead to an escalation of privileges in the sanboxed script execution itself. This issue was assigned provisional number CVE-2021-41790. Affected versions include:
org.alfresco:alfresco-content-services - versions from (including) 5.0.x.x up to (including)  5.2.7.11
org.alfresco:alfresco-content-services - versions from (including) 6.0.0.x up to (including)  6.0.1.9
org.alfresco:alfresco-content-services - versions from (including) 6.1.0.x up to (including)  6.1.1.10
org.alfresco:alfresco-content-services - versions from (including) 6.2.0.x up to (including)  6.2.2.18
org.alfresco:alfresco-content-services - versions 7.0.0.2 , 7.0.0.1 , 7.0
org.alfresco:alfresco-content-services - versions from (including) 7.0.1.0 up to (including) 7.0.1.2

The third issue fixed is an Blind SSRF that could be executed through a specific document format transformation. The issue has been fixed by disabling the specific tranformation through configuration. This issue was assigned provisional number CVE-2021-41792. Affected versions include:
org.alfresco:alfresco-content-services - versions from (including) 5.0.x.x up to (including) 5.2.7.11
org.alfresco:alfresco-content-services - versions from (including) 6.0.0.x up to (including) 6.0.1.9
org.alfresco:alfresco-content-services - versions from (including) 6.1.0.x up to (including) 6.1.1.10
org.alfresco:alfresco-content-services - versions from (including) 6.2.0.x up to (including) 6.2.2.18
org.alfresco:alfresco-transform-services - prior to (including) 1.3

Credits:
For CVE-2021-41791 and CVE-2021-41792 we want to thank Jack Misiura and Stefano Lanaro at The Missing Link Australia for reporting them and to professionally collaborate in the controlled disclosure. All the above issues were discovered during security testing and so far we do not have any evidence that they have ever been attempted to be exploited in a production environment.

