root/enterpriseprojects/overlays/alfresco-platform

This maven project is for the alfresco enterprise platform without the share extensions AMP.

build the war file with
 
 mvn install -Penterprise
 
 the war file will be available in the "target" directory.
 
 To run an alfresco repository (without the share extensions amp)
 
 use mvn install -Prun -DskipTests