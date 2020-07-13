#!/bin/sh

. "./common.func"

ACS_URL="${ACS_URL:-http://localhost:8080}"
ACS_ADMIN_USERNAME="${ACS_ADMIN_USERNAME:-admin}"
ACS_ADMIN_PASSWORD="${ACS_ADMIN_PASSWORD:-admin}"
ACS_AUTH="$ACS_ADMIN_USERNAME:$ACS_ADMIN_PASSWORD"

# $(dns_query)

# log_info "Enable Records Managment using RM REST API" -X POST -u
curl -s -o /dev/null -w "%{http_code}" -u "$ACS_AUTH" \
  --header 'Content-Type: application/json' \
  --header 'Accept: application/json' \
  -d '{"title": "Records Management"}' "$ACS_URL/alfresco/api/-default-/public/gs/versions/1/gs-sites?skipAddToFavorites=false"
