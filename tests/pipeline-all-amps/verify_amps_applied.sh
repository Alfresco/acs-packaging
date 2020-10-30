#!/usr/bin/env bash
echo "=========================== Starting Verify Amps Applied Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"

## Get List of Modules and Versions from /discovery endpoint

export ACS_URL="http://localhost:8080/alfresco/api/discovery"
export USERNAME="admin"
export PASSWORD="admin"

declare -a modules_id_installed=()

function getModulesInstalled() {
  response=$(curl -s -u $USERNAME:$PASSWORD $ACS_URL)
  modules_id_installed=$(echo $response | jq '.entry.repository.modules[] | ."id"')
}

#
# For a module name get the version mentioned in the /discovery endpoint
#
function getVersionInstalled() {
  MODULE_NAME=$1
  response=$(curl -s -u $USERNAME:$PASSWORD $ACS_URL)
  module_version_installed=$(echo "$response" | jq --arg MODULE_NAME $MODULE_NAME -r ".entry.repository.modules[] | select(.id==\"$MODULE_NAME\") .version")
  echo "$module_version_installed"
}

#
# Change extension of the amp file and unzip it
#
function unzip_amp() {
  amp_name=$1
  archive_name=($amp_name".zip")

  cp "$amp_name.amp" "$archive_name"
  unzip -q $archive_name -d $amp_name
}

#
# Get property from module.properties file
#
function getProperty() {
    PROP_KEY=$1
    PROP_VALUE=$(cat $PROPERTY_FILE | grep "$PROP_KEY" | cut -d'=' -f2)
    echo "$PROP_VALUE"
}

#
# Check if module is in the list of installed modules
#
function isModuleInstalled() {
    module_id=$1
    module_version=$2
    found=false
    for module in ${modules_id_installed}; do
      module=$(echo "$module" | cut -d "\"" -f 2)  # remove quotes from module name
      if [[ "$module" == "$module_id" ]]; then
        found=true
        echo $module" has been installed"
        module_version_installed=$(getVersionInstalled $module)
        if [[ "$module_version" == "$module_version_installed" ]]; then
          echo "Correct version $module_version has been installed"
        else
          echo "Version $module_version_installed has been installed, but should have been $module_version"
        fi
      fi
    done
    if [[ $found == false ]]; then
      echo $module_id" has NOT been installed"
    fi
}

#
# Delete zip files and unzipped folders
#
function cleanUp() {
    for amp in *.amp; do
      amp_name="${amp%.*}"
      rm -rf $amp_name
      rm -rf $amp_name".zip"
    done
}

#
# Get List of expected modules to be installed
#
function getExpectedModules() {
  cd ./tests/pipeline-all-amps/repo/target/amps

  ## Iterate through amp files
  for amp in *.amp; do
    amp_name="${amp%.*}"
    unzip_amp $amp_name

    PROPERTY_FILE=$amp_name"/module.properties"

    module_id=$(getProperty "module.id")
    module_version=$(getProperty "module.version")

    isModuleInstalled $module_id $module_version
  done

  cleanUp

  cd ../../../../../
}

getModulesInstalled
getExpectedModules

popd
echo "=========================== Finishing Verify Amps Applied Script =========================="
