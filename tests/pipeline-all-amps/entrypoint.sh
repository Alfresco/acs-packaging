#!/bin/sh

cp ./app.config.json /tmp/app.config.json
cp ./index.html /tmp/index.html

if [ "${APP_WITH_PROCESS}" = "true" ]; then
  cat ./assets-override/app.extensions.json > ./assets/app.extensions.json
fi

if [ -n "${APP_CONFIG_AUTH_TYPE}" ];then
  sed -e "s/\"authType\": \".*\"/\"authType\": \"${APP_CONFIG_AUTH_TYPE}\"/g" \
    -i /tmp/app.config.json && \
  cat /tmp/app.config.json > ./app.config.json
fi

if [ -n "${APP_CONFIG_IDENTITY_HOST}" ]; then
  replace="\/"
  encodedIdentity=${APP_CONFIG_IDENTITY_HOST//\//$replace}
  sed -e "s/\"identityHost\": \".*\"/\"identityHost\": \"$encodedIdentity\"/g" \
    -i /tmp/app.config.json && \
  cat /tmp/app.config.json > ./app.config.json
fi

if [ -n "${APP_CONFIG_OAUTH2_HOST}" ];then
  replace="\/"
  encoded=${APP_CONFIG_OAUTH2_HOST//\//$replace}
  sed -e "s/\"host\": \".*\"/\"host\": \"${encoded}\"/g" \
    -i /tmp/app.config.json && \
  cat /tmp/app.config.json > ./app.config.json
fi

if [ -n "${APP_CONFIG_OAUTH2_CLIENTID}" ];then
  sed -e "s/\"clientId\": \".*\"/\"clientId\": \"${APP_CONFIG_OAUTH2_CLIENTID}\"/g" \
    -i /tmp/app.config.json && \
  cat /tmp/app.config.json > ./app.config.json
fi

if [ -n "${APP_CONFIG_ALFRESCO_REPO_NAME}" ];then
  sed -e "s/\"alfrescoRepositoryName\": \".*\"/\"alfrescoRepositoryName\": \"${APP_CONFIG_ALFRESCO_REPO_NAME}\"/g" \
    -i /tmp/app.config.json && \
  cat /tmp/app.config.json > ./app.config.json
fi

if [ -n "${APP_CONFIG_OAUTH2_IMPLICIT_FLOW}" ];then
 sed "/implicitFlow/s/true/${APP_CONFIG_OAUTH2_IMPLICIT_FLOW}/" \
    -i /tmp/app.config.json && \
  cat /tmp/app.config.json > ./app.config.json
fi

if [ -n "${APP_CONFIG_OAUTH2_SILENT_LOGIN}" ];then
 sed "/silentLogin/s/true/${APP_CONFIG_OAUTH2_SILENT_LOGIN}/" \
    -i /tmp/app.config.json && \
  cat /tmp/app.config.json > ./app.config.json
fi

if [ -n "${APP_CONFIG_OAUTH2_REDIRECT_SILENT_IFRAME_URI}" ];then
  replace="\/"
  encoded=${APP_CONFIG_OAUTH2_REDIRECT_SILENT_IFRAME_URI//\//$replace}
  sed -e "s/\"redirectSilentIframeUri\": \".*\"/\"redirectSilentIframeUri\": \"${encoded}\"/g" \
    -i /tmp/app.config.json && \
  cat /tmp/app.config.json > ./app.config.json
fi

if [ -n "${APP_CONFIG_OAUTH2_REDIRECT_LOGIN}" ];then
  replace="\/"
  encoded=${APP_CONFIG_OAUTH2_REDIRECT_LOGIN//\//$replace}
  sed -e "s/\"redirectUri\": \".*\"/\"redirectUri\": \"${encoded}\"/g" \
    -i /tmp/app.config.json && \
  cat /tmp/app.config.json > ./app.config.json
fi

if [ -n "${APP_CONFIG_OAUTH2_REDIRECT_LOGOUT}" ];then
  replace="\/"
  encoded=${APP_CONFIG_OAUTH2_REDIRECT_LOGOUT//\//$replace}
  sed -e "s/\"redirectUriLogout\": \".*\"/\"redirectUriLogout\": \"${encoded}\"/g" \
    -i /tmp/app.config.json && \
  cat /tmp/app.config.json > ./app.config.json
fi

if [[ -n "${APP_CONFIG_BPM_HOST}" ]]
then
  replace="\/"
  encoded=${APP_CONFIG_BPM_HOST//\//$replace}
  sed -e "s/\"bpmHost\": \".*\"/\"bpmHost\": \"${encoded}\"/g" \
    -i /tmp/app.config.json && \
  cat /tmp/app.config.json > ./app.config.json
fi

if [[ -n "${APP_CONFIG_ECM_HOST}" ]]
then
  replace="\/"
  encoded=${APP_CONFIG_ECM_HOST//\//$replace}
  sed -e "s/\"ecmHost\": \".*\"/\"ecmHost\": \"${encoded}\"/g" \
    -i /tmp/app.config.json && \
  cat /tmp/app.config.json > ./app.config.json
fi

if [ -n "${APP_CONFIG_PROVIDER}" ];then
  sed -e "s/\"providers\": \".*\"/\"providers\": \"${APP_CONFIG_PROVIDER}\"/g" \
    -i /tmp/app.config.json && \
  cat /tmp/app.config.json > ./app.config.json
fi

if [[ $BASE_PATH ]]; then
  replace="\/"
  encoded=${BASE_PATH//\//$replace}
  sed -ri 's%href=".?/"%href="'$encoded'"%g' /tmp/index.html && \
  cat /tmp/index.html > ./index.html
fi

if [ -n "${APP_BASE_SHARE_URL}" ];then
  replace="\/"
  encoded=${APP_BASE_SHARE_URL//\//$replace}
  sed -e "s/\"baseShareUrl\": \".*\"/\"baseShareUrl\": \"${encoded}\"/g" \
    -i /tmp/app.config.json && \
  cat /tmp/app.config.json > ./app.config.json
fi

if [[ $SERVER_PATH ]]; then
  mkdir -p .$SERVER_PATH
  cp -R * .$SERVER_PATH
  replace="\/"
  encoded=${SERVER_PATH//\//$replace}
  sed -ri 's%href=".?/"%href="'$encoded/'"%g' /tmp/index.html && \
  cat /tmp/index.html > .$SERVER_PATH/index.html
fi

if [ -n "${APP_CONFIG_APPS_DEPLOYED}" ];then
  sed -e "s/\"alfresco-deployed-apps\": \[.*\]/\"alfresco-deployed-apps\": ${APP_CONFIG_APPS_DEPLOYED}/g" \
    -i /tmp/app.config.json && \
  cat /tmp/app.config.json > ./app.config.json
fi

nginx -g "daemon off;"
