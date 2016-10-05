#!/bin/bash
set -e
cd "$(dirname "$0")"

#
# Uploads and assigns the Solr config files to an existing collection in SolrCloud
#

SHARDS=1
COLLECTION="cdx8"

SOLR_SCRIPTS=/home/te/tmp/sumfresh/sites/aviser/solrcloud/solr/machine1/server/scripts
SOLR="localhost:50001"
ZOOKEEPER="localhost:2181"
   
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
    

echo "Done"
