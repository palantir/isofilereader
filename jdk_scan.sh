#!/bin/bash

# This script finds local JDKs and adds their vars, plugins are failing to do so

for dir in ~/.gradle/gradle-jdks/*/; do
    major_version_dir=${dir%*/}
    major_version=${major_version_dir##*/}

    #echo "export JAVA_HOME${major_version}=$dir"
    folder_name=$(basename "$dir")
    major_version=$(echo $folder_name | grep -o -E '[0-9]+' | head -1)
    echo $major_version
    declare "JAVA_${major_version}_HOME=${dir}"

    # Export the new variable to the environment
    eval export JAVA_${major_version}_HOME
done