### Alfresco repository cluster with load balancer for testing

This folder contains a sample setup for a cluster with 3 alfressco repository nodes in a cluster,
following the reference docker-compose file from the acs-deployment project.

To configure the sticky session with **ip_hash** method, just comment/uncomment the ```ip_hash;``` line 
from /ngnix/alfresco.conf

This docker-compose is used in the _ACS Packaging Cluster_ bamboo build plan 
https://bamboo.alfresco.com/bamboo/browse/PLAT-APCLUST

Note that at the moment, we expect CMIS test to fail without sticky session set. 
And the TAS REST API tests also use CMIS - see REPO-4250

See also REPO-3932 for details.

