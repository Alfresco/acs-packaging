#!/bin/bash

# Script used to check for duplicated libraries after applying all amps

Libdir=$1
lib_list=""
echo "Scanning libraries from $Libdir"

for lib1 in $(ls "$Libdir"); do
    if [[ "$lib1" =~ [0-9]+.[0-9] ]]; then
        noversion="${lib1%%"$BASH_REMATCH"*}"
        for lib2 in $(ls -I "$lib1" "$Libdir"); do
            if [[ "$lib2" = "$noversion"[0-9].* ]]; then
                lib_list="$lib_list $lib1"
            fi
        done
    fi
done

if [ -n "$lib_list" ]; then
    echo "The following libraries have more than one version: $lib_list"
    exit 1
else
    echo "There are no duplicated libraries"
fi