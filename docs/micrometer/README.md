## Micrometer
This folder contains information about Alfresco Repository integration with [micrometer](https://micrometer.io/) 
and useful files.

Alfresco repository now exposes an endpoint ```/alfresco/s/prometheus``` that is compatible with the 
[Prometheus](https://prometheus.io/) scarping and point. 
This end point is read-only and does not currently require authentication. 
Prometheus uses a pulling pattern on the specified end points and is able to provide 
aggregation of time series events that help in monitoring our application.

We expose, for now, three types of metrics:
1. JVM metrics
2. DB (mybatis) metrics
3. REST API metrics

All of these and specific details about each gathered metrics can be controlled with system properties:
```
# Metrics reporting
metrics.enabled=true
metrics.dbMetricsReporter.enabled=true
metrics.dbMetricsReporter.query.enabled=true
metrics.dbMetricsReporter.query.statements.enabled=false
metrics.jvmMetricsReporter.enabled=true
metrics.restMetricsReporter.enabled=true
metrics.restMetricsReporter.path.enabled=false
```

This feature is enterprise only;


### Grafana Dashboards

This folder also contains dashboards that you can import in your [Grafana](https://grafana.com/) app to visualize 
various metrics exposed by Alfresco through a Prometheus aggregation server.

Feel free to update or add new Dashboards or queries that others may find useful.
* [DB Metrics](/docs/micrometer/AlfrescoDBMetricsDashboard.json)

#### JVM metrics

If you want to see basic JVM metrics in a dashboard in Grafana, there are already predefined dashboards.
Here is an example: https://grafana.com/dashboards/4701 . You could import this url directly in your Grafana.

