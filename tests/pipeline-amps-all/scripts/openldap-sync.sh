#!/bin/sh

. "./common.func"

IDENTITY_SERVICE_URL="${IDENTITY_SERVICE_URL:-http://localhost:8085}"
ACS_BASE_URL="${ACS_BASE_URL:-http://localhost:8080}"
ACS_ADMIN_USERNAME="${ACS_ADMIN_USERNAME:-admin}"
ACS_ADMIN_PASSWORD="${ACS_ADMIN_PASSWORD:-admin}"
ACS_AUTH="$ACS_ADMIN_USERNAME:$ACS_ADMIN_PASSWORD"

$(dns_query)

TOKEN=$(get_admin_token $IDENTITY_SERVICE_URL)

curl --insecure -v --silent --show-error -X POST "$IDENTITY_SERVICE_URL/auth/admin/realms/alfresco/user-storage/12428c8b-dd48-45c7-ad39-d99c745f0002/sync?action=triggerFullSync" \
        -H "Authorization: Bearer $TOKEN" \
        -d 'client_id=alfresco&grant_type=password&username=%7B%7BadminUserName%7D%7D&password=admin'

curl --insecure -v --silent --show-error -X POST "$ACS_BASE_URL/alfresco/s/enterprise/admin/admin-sync" -u "$ACS_AUTH"
