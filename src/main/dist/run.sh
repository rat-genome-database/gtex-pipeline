#!/usr/bin/env bash
#
# GTex loading pipeline
#
. /etc/profile
APPNAME=GtexPipeline
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`

APPDIR=/home/rgddata/pipelines/$APPNAME
cd $APPDIR

DB_OPTS="-Dspring.config=$APPDIR/../properties/default_db.xml"
LOG4J_OPTS="-Dlog4j.configuration=file://$APPDIR/properties/log4j.properties"
declare -x "GTEX_PIPELINE_OPTS=$DB_OPTS $LOG4J_OPTS"
bin/$APPNAME "$@" > run.log 2>&1

mailx -s "[$SERVER] GTex Pipeline Run" mtutaj@mcw.edu < $APPDIR/logs/status.log
