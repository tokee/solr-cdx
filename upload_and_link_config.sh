#!/bin/bash

#
# Uploads Solr configs to the cloud and ensures that the cdx collection is created and
# assigned the given config.
#
# Multiple calls to this command are safe and only affect existing ZooKeeper configs and
# SolrCloud collection if the configuration files in "config/" has been changed.
#

set -e

pushd `dirname $0` > /dev/null
source "scripts.default.conf"
if [ -s "scripts.conf" ]; then
    source "scripts.conf"
fi

SOLR_SCRIPTS=$CLOUD/solr1/server/scripts
if [ ! -d $SOLR_SCRIPTS ]; then
    >&2 echo "Error: No Solr scripts folder at $SOLR_SCRIPTS"
    exit 3
fi
SOLR="$HOST:$SOLR_BASE_PORT"
ZOOKEEPER="$HOST:$ZOO_BASE_PORT"
   
CONFIG_VERSION=`cat config/schema.xml | grep -o "config-version: .*" | cut -d\  -f2`
CONFIG="cdx_conf_${CONFIG_VERSION}"

echo "Adding config $CONFIG to ZooKeeper"
$SOLR_SCRIPTS/cloud-scripts/zkcli.sh -zkhost $ZOOKEEPER -cmd upconfig -confname $CONFIG -confdir config/

set +e
EXISTS=`curl -s "http://$SOLR/solr/admin/collections?action=LIST" | grep -o "<str>${COLLECTION}</str>"`
set -e

if [ "." == ".$EXISTS" ]; then
    
    echo "Collection $COLLECTION does not exist. Creating new $SHARDS shard collection with config $CONFIG"
    curl -s "http://$SOLR/solr/admin/collections?action=CREATE&name=${COLLECTION}&numShards=${SHARDS}&maxShardsPerNode=${SHARDS}&replicationFactor=1&collection.configName=${CONFIG}"
    
else
   
    echo "Collection $COLLECTION does not exist. Assigning config $CONFIG"
    $SOLR_SCRIPTS/cloud-scripts/zkcli.sh -zkhost $ZOOKEEPER -cmd linkconfig -collection $COLLECTION -confname $CONFIG

    echo "Reloading collection $COLLECTION"
    curl -s "http://$SOLR/solr/admin/collections?action=RELOAD&name=$COLLECTION"
    
fi
    

echo "Done. Inspect the collection at http://$HOST:$SOLR_BASE_PORT/solr/#/$COLLECTION/collection-overview"
