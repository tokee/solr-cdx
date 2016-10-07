#!/bin/bash

#
# Starts up a previously installed cloud.
# This command does not affect an already running cloud.
#

pushd `dirname $0` > /dev/null
source "scripts.default.conf"
if [ -s "scripts.conf" ]; then
    source "scripts.conf"
fi

function err() {
    >&2 echo "$2"
    exit $1
}

function log() {
    >&2 echo "$1"
}
    
if [ ! -d $CLOUD ]; then
    err 1 "Error: No cloud available. Please run ./install_cloud.sh first"
fi
pushd $CLOUD > /dev/null
OK=true
# Be sure to start the ZooKeepers first

for Z in `seq 1 $ZOOS`; do
    if [ ! -d zoo$Z ]; then
        err "Expected a ZooKeeper-instalation at $CLOUD/zoo$S but found none. Please run ./install_cloud.sh"
    fi
    zoo$Z/bin/zkServer.sh start
done

SOLR_PORT=$SOLR_BASE_PORT
for S in `seq 1 $SOLRS`; do
    if [ ! -d solr$S ]; then
        err "Expected a Solr-instalation at $CLOUD/solr$S but found none. Please run ./install_cloud.sh"
    fi
    solr$S/bin/solr -m $SOLR_MEM -cloud -s `pwd`/solr$S/server/solr/ -p $SOLR_PORT -z $HOST:$ZOO_BASE_PORT -h $HOST
    SOLR_PORT=$(( SOLR_PORT + 1 ))
done

popd > /dev/null

echo "Done. Solr admin UI available at http://$HOST:$SOLR_BASE_PORT/solr/"
