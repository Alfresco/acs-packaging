# Micrometer

This folder contains information about Alfresco Repository integration with
[micrometer](https://micrometer.io/) and useful files.

Alfresco repository now exposes an endpoint `/alfresco/s/prometheus` that is
compatible with [Prometheus](https://prometheus.io/) scraping.
This end point is read-only and does not currently require authentication. In
kubernetes environments - when using the Helm chart Alfresco provides - access
to this endpoint is prohibited at the ingress level. That means that your
prometheus installation should run whithin the same cluster and access the
repository service directly (by-passing the ingress).

Prometheus uses a pulling pattern on the specified endpoints and is able to
provide aggregation of time series events that help monitoring our application.

Several types of metrics are exposed by the repository:

* JVM metrics
* DB (mybatis) metrics
* REST API metrics
* Authentication metrics (tickets/sessions and users)
* ElasticSearch metrics

All of these and specific details about each gathered metric can be controlled
with system properties:

```properties
# Metrics reporting
metrics.enabled=false
metrics.dbMetricsReporter.enabled=false
metrics.dbMetricsReporter.query.enabled=false
metrics.dbMetricsReporter.query.statements.enabled=false
metrics.jvmMetricsReporter.enabled=false
metrics.restMetricsReporter.enabled=false
metrics.restMetricsReporter.path.enabled=false
metrics.authenticationMetricsReporter.enabled=false
metrics.elasticSearchMetricsReporter.enabled=false
metrics.elasticSearchMetricsReporter.cache.expireAfterWrite.seconds=120
metrics.elasticSearchMetricsReporter.cache.refreshAfterWrite.seconds=60
```

> This feature is enterprise only!

We have defined the main property `metrics.enabled` which defaults to `false`.
If turned off, the web-script API will return 404.

:warning: In case you want to enable REST metrics with path enabled, make sure
your Prometheus instance can cope with the amount of time series that will be
created, or ensure relabeling is done in scrapping configuration to reduce the
amount of time series. In the example below, `metricRelabelings` are used to
extract the version, model and endpoint of the REST API from the `servicePath`
label and then drop the initial label, thus avoiding the creation of huge
numbers of time series.

Once the Alfresco endpoint is configured to expose metrics, Prometheus must be
configured to scrape the endpoint. The following is an example of a Prometheus
configuration that scrapes the Alfresco endpoint:

```yaml
scrape_configs:
  - job_name: 'alfresco'
    metrics_path: '/alfresco/s/prometheus'
    static_configs:
      - targets: ['alfresco-repository:8080']
    metric_relabel_configs:
    - source_labels: [servicePath]
      separator: ;
      regex: /alfresco/api/-default-/(private|public)/([^/]+)/versions/([0-9]+(\.[0-9]+)*)/([^/]+)(/.*)?
      target_label: alfresco_rest_api_version
      replacement: $3
      action: replace
    - source_labels: [servicePath]
      separator: ;
      regex: /alfresco/api/-default-/(private|public)/([^/]+)/versions/([0-9]+(\.[0-9]+)*)/([^/]+)(/.*)?
      target_label: alfresco_rest_api_model
      replacement: $2
      action: replace
    - source_labels: [servicePath]
      separator: ;
      regex: /alfresco/api/-default-/(private|public)/([^/]+)/versions/([0-9]+(\.[0-9]+)*)/([^/]+)(/.*)?
      target_label: alfresco_rest_api_endpoint
      replacement: $5
      action: replace
    - separator: ;
      regex: servicePath
      replacement: $1
      action: labeldrop
```

If you're using Prometheus Operator, you can use the following
[ServiceMonitor](https://github.com/prometheus-operator/prometheus-operator/blob/main/Documentation/api.md#monitoring.coreos.com/v1.ServiceMonitorSpec):

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: alfresco-repo-micrometer
  namespace: acs
spec:
  endpoints:
    - metricRelabelings:
        - action: replace
          regex: >-
            /alfresco/api/-default-/(private|public)/([^/]+)/versions/([0-9]+(\.[0-9]+)*)/([^/]+)(/.*)?
          replacement: $3
          sourceLabels:
            - servicePath
          targetLabel: alfresco_rest_api_version
        - action: replace
          regex: >-
            /alfresco/api/-default-/(private|public)/([^/]+)/versions/([0-9]+(\.[0-9]+)*)/([^/]+)(/.*)?
          replacement: $2
          sourceLabels:
            - servicePath
          targetLabel: alfresco_rest_api_model
        - action: replace
          regex: >-
            /alfresco/api/-default-/(private|public)/([^/]+)/versions/([0-9]+(\.[0-9]+)*)/([^/]+)(/.*)?
          replacement: $5
          sourceLabels:
            - servicePath
          targetLabel: alfresco_rest_api_endpoint
        - action: labeldrop
          regex: servicePath
      path: /alfresco/s/prometheus
      port: http
  jobLabel: alfresco-repo-micrometer
  namespaceSelector:
    any: false
  selector:
    matchLabels:
      app.kubernetes.io/component: alfresco-repository
      app.kubernetes.io/instance: acs
```

> `metadata.namespace` and `selector.matchLabels` should be adjusted to your own
  deployment.

## Grafana Dashboards

This folder also contains dashboards that you can import in your
[Grafana](https://grafana.com/) app to visualize various metrics exposed by
Alfresco through a Prometheus aggregation server.

Feel free to update or add new Dashboards or queries that others may find useful:

* [Database Metrics](/docs/micrometer/Alfresco_Enterprise_Database_Dashboard-1714473792698.json)
* [Alfresco REST API Metrics](/docs/micrometer/Alfresco_Enterprise_REST_API_Dashboard-1714475951666.json)
* [Alfresco JVM Metrics](/docs/micrometer/Alfresco_Enterprise_JVM_Dashboard-1714472967588.json)
* [Alfresco Servlet Metrics](/docs/micrometer/Alfresco_Enterprise_Servlet_Dashboard-1714473338586.json)
* [Alfresco Authentication Metrics](/docs/micrometer/Alfresco_Enterprise_Authentication_Dashboard.json)

> The Alfresco JVM metrics dashboard have been largely inspired from the "JVM
> micrometer" dashboard available at [Grafana
> Dashboards](https://grafana.com/dashboards/4701). One can choose to reuse this
> original dashboard but be aware that Alfresco micrometer implementation does
> not populate the `application` label this dashboard expects.
