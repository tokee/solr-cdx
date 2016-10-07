#!/bin/bash

#
# Shuts down the whole local cloud.
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

SOLR_PORT=$SOLR_BASE_PORT
for S in `seq 1 $SOLRS`; do
    if [ ! -d solr$S ]; then
        err "Expected a Solr-instalation at $CLOUD/solr$S but found none. Please run ./install_cloud.sh"
    fi
    solr$S/bin/solr stop
    SOLR_PORT=$(( SOLR_PORT + 1 ))
done
   
# Be sure to shut down the ZooKeepers last
for Z in `seq 1 $ZOOS`; do
    if [ ! -d zoo$Z ]; then
        err "Expected a ZooKeeper-instalation at $CLOUD/zoo$S but found none. Please run ./install_cloud.sh"
    fi
    zoo$Z/bin/zkServer.sh stop
done

popd > /dev/null
echo "Done."
