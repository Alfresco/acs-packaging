#!/bin/bash

declare -a REPOS=("alfresco-community-repo" "alfresco-enterprise-repo" "alfresco-enterprise-share" "acs-packaging" "acs-community-packaging")
declare -a STATUSES=("in_progress" "queued" "requested" "waiting")
BRANCH='hack/hackathon-demo'
NL=$'\n'

while getopts 'm:b:h' OPTION
do
    case $OPTION in
        m)
            if [ "$OPTARG" = "pat" ]; then
              echo "You chose Github Personal Access Token as the authentication mode."
              if [ -n "$GITHUB_PAT" ]; then
                PROVIDER="curl"
              else
                echo "Missing GITHUB_PAT variable"
                exit 1
              fi
            fi
            if [ "$OPTARG" = "cli" ]; then
              echo "You chose Github CLI as the authentication mode."
              gh auth login
              PROVIDER='gh api'
            fi
            ;;
        b)
            echo "$OPTARG"
            BRANCH="$OPTARG"
            ;;
        h)
              cat <<EOF
Usage: $0 -m cli -b develop
This script verifies that there are no ongoing workflows for a specific branch across several ACS projects that are linked together for release purposes.


-h                   prints usage information.
-m                   required flag to specify the authentication mode.
                     The possible values are: [pat, cli].
-b                   optional flag to specify a branch (default: hack/hackathon-demo).
EOF
            exit 0
            ;;
        ?)
            exit 0
            ;;
        *) echo "Invalid option $REPLY";;
    esac
done

echo "Branch: $BRANCH"

for REPO in "${REPOS[@]}"
do
    for STATUS in "${STATUSES[@]}"
    do
        if [ "$PROVIDER" = "curl" ]; then
          PLAIN_RESPONSE=$(curl -s \
            -H "Authorization: Bearer $GITHUB_PAT" \
            -H "Accept: application/vnd.github+json" \
            https://api.github.com/repos/Alfresco/"$REPO"/actions/runs?branch="$BRANCH"'&'status="$STATUS")
        fi
        if [ "$PROVIDER" = "gh api" ]; then
          PLAIN_RESPONSE=$(gh api \
            -H "Accept: application/vnd.github+json" \
            https://api.github.com/repos/Alfresco/"$REPO"/actions/runs?branch="$BRANCH"'&'status="$STATUS")
        fi
        MESSAGE=$(jq -n "$PLAIN_RESPONSE" | jq '.message')
        if [ "$MESSAGE" = "\"Bad credentials\"" ]; then
          echo "Error: $MESSAGE"
          exit 1
        fi
        STATUS_RESPONSE=$(jq -n "$PLAIN_RESPONSE" | jq '(.workflow_runs[])? | "\(.html_url) \(.status)"')
        if [ -n "$STATUS_RESPONSE" ]; then
            RESPONSE="${RESPONSE} ${NL} ${STATUS_RESPONSE}"
            unset STATUS_RESPONSE
        fi
    done
    if [ -n "$RESPONSE" ]; then
        RESULT="${RESULT} ${REPO} ${RESPONSE} ${NL}${NL}"
        unset RESPONSE
    fi
done
if [ -n "$RESULT" ]; then
    echo "$RESULT"
    exit 1
else
    echo "There are currently no requested workflows for the specified input."
    exit 0
fi