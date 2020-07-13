#!/bin/sh

. "./common.func"

IDENTITY_SERVICE_URL="${IDENTITY_SERVICE_URL:-http://localhost:8085}"
LDAP="${LDAP:-openldap}"

$(dns_query)

# log_info "id url $IDENTITY_SERVICE_URL"
TOKEN=$(get_admin_token $IDENTITY_SERVICE_URL)

log_info "Set openLDAP host"
sed -e "s#@@LDAP@@#$LDAP#g" config-files/ldap-auth-defn.json > config-files/ldap-auth-defn-edit.json

curl --insecure -v --silent --show-error -X POST "$IDENTITY_SERVICE_URL/auth/admin/realms/alfresco/components" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Accept: application/json" \
        -H "Content-Type: application/json" \
        --data "@./config-files/ldap-auth-defn-edit.json"
