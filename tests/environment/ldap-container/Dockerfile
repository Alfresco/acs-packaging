FROM osixia/openldap:1.1.5

ADD importData.ldif /importData.ldif
ADD startup.sh /container/service/slapd/startup.sh
ADD startup.sh /container/environment/99-default/service/slapd/startup.sh
ADD default.yaml.startup /container/environment/99-default/default.yaml.startup

# Expose default ldap and ldaps ports

EXPOSE 389 636