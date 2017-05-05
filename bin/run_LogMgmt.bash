#!/bin/bash

if [ -z ${JAVA_HOME+x} ]; then JAVA_HOME=/usr/java/jdk1.8.0; fi
SCRIPT_DIR=$(dirname `which $0`)
LIB_DIR="${SCRIPT_DIR}/../target/lib/"

for i in ${LIB_DIR}*.jar; do
    CLASSPATH=$CLASSPATH:$i
done

COMMAND="$JAVA_HOME/bin/java -cp $CLASSPATH mil.nga.logmgmt.LogMgmt"

if [ ! -z "$1" ] ; then
    COMMAND+=" -propertiesFile=$1"
fi
if [ ! -z "$2" ] ; then
    COMMAND+=" -serverGroup=$2"
fi
if [ ! -z "$3" ] ; then
    COMMAND+=" -customPrefix=$3"
fi
if [ ! -z "$4" ] ; then
    COMMAND+=" -baseOverride=$4"
fi

