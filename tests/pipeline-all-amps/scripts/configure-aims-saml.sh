#!/bin/sh
# Author: mmuller
# Original script from https://git.alfresco.com/platform-services/alfresco-dbp-test/blob/master/sso/scenario3/scripts/configure-share.sh
# Removed the CSRF stuff because I disabled it on ACS
. "./common.func"

ACS_BASE_URL="${ACS_BASE_URL:-http://localhost:8080}"
IDENTITY_SERVICE_URL="${IDENTITY_SERVICE_URL:-http://localhost:8085}"
ACS_ADMIN_USERNAME="${ACS_ADMIN_USERNAME:-admin}"
ACS_ADMIN_PASSWORD="${ACS_ADMIN_PASSWORD:-admin}"
ACS_AUTH="$ACS_ADMIN_USERNAME:$ACS_ADMIN_PASSWORD"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin}"
IDP_CERTIFICATE=saml-deployment.cert
REPO_HOST="${REPO_HOST:-localhost}"
AOS_PORT="${AOS_PORT:-8080}"
SHARE_PORT="${SHARE_PORT:-8080}"

create_client() {
  sp=$1

  log_info "Create Client for $sp"
  # Get the token
  log_info "IDENTITY_SERVICE_URL: $IDENTITY_SERVICE_URL"
  TOKEN=$(get_admin_token $IDENTITY_SERVICE_URL $ADMIN_USERNAME $ADMIN_PASSWORD)

  # log_info "TOKEN: $TOKEN"
  # log_info $REPO_HOST
  log_info "Set host ip, share port, aos port"
  sed -e "s#@@REPO_HOST@@#$REPO_HOST#g" -e "s#@@AOS_PORT@@#$AOS_PORT#g" -e "s#@@SHARE_PORT@@#$SHARE_PORT#g" config-files/$sp-saml-client.json > config-files/$sp-saml-client-edit.json

  # Create a client in AIS
  STATUS_CODE=$(curl -o /dev/null -w "%{http_code}" -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" --request POST --data @config-files/$sp-saml-client-edit.json "$IDENTITY_SERVICE_URL/auth/admin/realms/alfresco/clients")
  if [ $STATUS_CODE -eq "201" ]; then
    log_info "Client successfully created"
  elif [[ $STATUS_CODE -eq "409" ]]; then
    log_info "Client already exists. Status code: $STATUS_CODE"
  else
    log_error "Could not create the client in AIS. Status code: $STATUS_CODE"
  fi

  log_info "Get the client id"
  ALLCLIENTS=$(curl -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" --request GET "$IDENTITY_SERVICE_URL/auth/admin/realms/alfresco/clients")
  ID=$(echo $ALLCLIENTS | jq -r '.[] | select (.clientId == "'$sp'-saml") | .id')

  log_info "Get the X509 certificate from AIS and save it"
  PROVIDERS=$(curl -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" --request GET "$IDENTITY_SERVICE_URL/auth/admin/realms/alfresco/clients/$ID/installation/providers/saml-idp-descriptor")
  export CERT_SIG=$(sed -ne '/dsig:X509Certificate/{s/.*<dsig:X509Certificate>\(.*\)<\/dsig:X509Certificate>.*/\1/p;q;}' <<< "$PROVIDERS")
  cat <<EOF > saml-deployment.cert
-----BEGIN CERTIFICATE-----
$CERT_SIG
-----END CERTIFICATE-----
EOF
}


# Upload IdP Certificate
upload_certificate() {
  sp=$1
  certificate=$2

  log_info "Uploading IdP certificate $certificate for $sp ..."

  STATUS_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$ACS_BASE_URL/alfresco/s/enterprise/admin/admin-saml-idp-cert-upload/$sp" -u "$ACS_AUTH" \
    -H 'Connection: keep-alive' \
    -H 'Cache-Control: no-cache' \
    -F "file=@$certificate" --insecure --compressed)
  log_info "ACS_AUTH $ACS_AUTH "
  # log_info "STATUS_CODE: $STATUS_CODE"
  if [ $STATUS_CODE -eq "200" ]; then
    log_info "IdP certificate for $sp has been uploaded successfully."
  else
    log_error "Couldn't upload IdP certificate for $sp. Status code: $STATUS_CODE"
  fi
}

$(dns_query)

# Create Share client
create_client "share"
# Upload IdP Certificate for Share
upload_certificate "share" "$IDP_CERTIFICATE"

# Create REST API client
# create_client "rest-api"
# Upload IdP Certificate for Rest-api
# upload_certificate "rest-api" "$IDP_CERTIFICATE"

# Create AOS client
create_client "aos"
# Upload IdP Certificate for AOS
upload_certificate "aos" "$IDP_CERTIFICATE"

