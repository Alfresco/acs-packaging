#!/bin/bash

# Extracts value for an option from a commit message where the commit message consists of a
# whitespace separated list of options (or other words). The options have to follow a format:
# [option1=value1] [option2=value2]
function extract_option {
    option="$1"
    commit_message="$2"

	# Split commit message on white spaces
    set -f; IFS=" "
    arr=($commit_message)
    set +f; unset IFS

    for ((i=0; i<${#arr[@]}; i++)); do

 		# Check the token is of expected format - "[option=anyValue]"
        pattern="^\[$option=.*\]$"
        token=${arr[i]}
        if [[ $token =~ $pattern ]]
        then
  	    	# Strip leading "[option=" and trailing "]"
            echo $token | sed "s/\[$option=\(.*\)\]$/\1/"
            break
        fi
    done
}

