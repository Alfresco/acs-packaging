#!/usr/bin/env bash

shopt -s expand_aliases
source acs-packaging/dev/aliases

# Function to perform cURL request with timeout
function perform_curl() {
    local url=$1
    local max_retries=$2
    local retry_delay=$3
    local expectedStatusCode=$4

    local retry=0
    local exit_code=0

    while [ $retry -lt $max_retries ]; do
        status_code=$(curl --write-out %{http_code} --silent --output /dev/null $url)

        exit_code=$?
        if [[ ("$exit_code" -eq 0) && (-z "$expectedStatusCode" || "$status_code" -eq "$expectedStatusCode") ]]; then
            echo "Webpage is available"
            return 0
        else
            echo "Webpage not available (retry: $((retry+1)))"
            sleep $retry_delay
            retry=$((retry+1))
        fi
    done

    echo "Maximum number of retries reached"
    return $exit_code
}

function cleanup() {
    echo "When testing locally, uncomment cleanup method code"
    #Uncomment when testing locally, will cleanup script leftovers (due to starting background processes)
#    # Stop the environment and Tomcat server by sending termination signal
#    kill -SIGINT $1 &
#    kill -SIGINT $2 &
#    #Kill java processes - Tomcat if is up
#    kill $(ps aux | grep '[j]ava' | awk '{print $2}')
#    #Kill docker compose processes, can be needed in case of long image downloads
#    kill $(ps aux | grep '[d]ocker-compose' | awk '{print $2}')
#
#    envKill
#    echo yes | envRm
}

echo "=========================== Starting Tomcat deployment test ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex

pushd "$(dirname "${BASH_SOURCE[0]}")/../../../"

#Start environment

#envUp
#envUp_pid=$!
perform_curl "http://localhost:8983/solr/" 120 10
exit_code=$?

if [[ ("$exit_code" -ne 0) ]]; then
  cleanup envUp_pid
  exit 7
fi

#Start Tomcat with Repository + Share + api-explorer
(entT &)
entT_pid=$!

# Call the function with desired parameters
perform_curl "http://localhost:8080/alfresco/" 120 10 200
exit_code=$?

cleanup envUp_pid entT_pid

popd
set +vex
echo "=========================== Ending api-explorer deployment test =========================="

exit $exit_code