#!/bin/bash

# Script used to check for duplicated libraries after applying all amps

# List of library names (separated by space) for which the version is not easy to identify with simple pattern matching,
# usually libraries with versions that don't follow naming/versioning standards.
# e.g. geronimo-jms_2.0_spec-1.0-alpha-2.jar
#      geronimo-jms_1.1_spec-1.1.1.jar
WHITELIST="geronimo-jta"

lib_dir=$1
multiple_version_lib_list=""

echo "Scanning libraries from $lib_dir"
if [[ -n "$WHITELIST" ]]; then
    echo "Skipping libraries which match the whitelist, check for false positives:"
fi

for current_lib in $(ls "$lib_dir"); do
   if [[ "$current_lib" =~ [0-9]+.[0-9] ]]; then
       noversion_lib="${current_lib%%"$BASH_REMATCH"*}"

       skip=false
       for exclude_lib in $WHITELIST; do
           if [[ "$noversion_lib" =~ "$exclude_lib" ]]; then
               skip=true
               break
           fi
       done
       if [[ "$skip" = true ]]; then
           echo "Skip $current_lib"
           continue
       fi

       for other_lib in $(ls --ignore="$current_lib" "$lib_dir"); do
           if [[ "$other_lib" = "$noversion_lib"[0-9].* ]]; then
               multiple_version_lib_list="$multiple_version_lib_list $current_lib"
           fi
       done
   fi
done

if [[ -n "$multiple_version_lib_list" ]]; then
   >&2 echo "[ERROR] The following libraries have more than one version: $multiple_version_lib_list"
   exit 1
else
   echo "[INFO] There are no duplicated libraries."
fi