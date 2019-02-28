# <Work in progress> Project to start a standard cluster 

## Current limitations:
1. only works for starting up two alfresco nodes in the cluster
2. no clustered share!
3. only one solr6 is working - that connects to alfresconode1!
4. no load balancer!

## There is a lot of room for improvement here!
This project was done as a learning exercise for docker/docker-compose

### Configuration:  
1 shared alf_data local volume  
1 postgres DB  
2 nodes(each with its own alfresco, share)  
2 solr6 nodes - each one connected to one of the alfresco nodes  
1 ngnix load balancer  

### Steps:
1. clone this project
2. clean your local docker environmet of unnecessary images/volumes/process
3. then run ``` docker-compose up postgres alfresconode1 solr6node1```
4. go to localhost:8181/alfresco - upload a cluster enabled license for 6.0 **EA**
5. open another terminal and run: ``` docker-compose up alfresconode2```
6. go to localhost:8182/alfresco and check that this node is in the cluster - It should be
7. open another terminal and run: ``` docker-compose stop alfresconode1```
8. then, in the same terminal, run: ```docker-compose up alfresconode1```
9. go to localhost:8181/alfresco again and check that both nodes are in the cluster