#!/bin/bash

#
# Installs a sample SolrCloud with ZooKeeper on the local machine.
#
# The installer does not override existing files and can be executed
# safely multiple times.
#

pushd `dirname $0` > /dev/null
source "scripts.default.conf"
if [ -s "scripts.conf" ]; then
    source "scripts.conf"
fi


function log() {
    echo "$1"
}

function loge() {
    >&2 echo "$1"
}

function download() {
    URL="$1"
    FILE=${URL##*/}
    
    if [ ! -s "$FILE" ]; then
        if [ -s "../$FILE" ]; then
            loge "    - Copying $FILE from parent folder"
            cp ../$FILE .
        else
            loge "    - Downloading $URL"
            curl "$URL" > "$FILE"
        fi
    fi

    BASE=`echo "$FILE" | sed -e 's/.tgz//' -e 's/.tar.gz//'`
    if [ ! -d "$BASE" ]; then
        loge "    - Unpacking $FILE"
        tar -xzovf "$FILE" > /dev/null
    fi
    echo "$BASE"
}


log "Installing cloud in folder $CLOUD"
mkdir -p $CLOUD
pushd $CLOUD > /dev/null

log " - Installing SolrCloud"
SOLR=`download "$SOLR_URL"`
for S in `seq 1 $SOLRS`; do
    log "   - Solr $S"
    if [ ! -d solr$S ]; then
        log "     - Copying Solr files for instance $S"
        cp -r $SOLR solr$S
    fi
done

log " - Installing ZooKeeper ensemble"
ZOO=`download "$ZOO_URL"`
ZPORT=$ZOO_BASE_PORT
for Z in `seq 1 $ZOOS`; do
    log "   - ZooKeeper $Z"
    if [ ! -d zoo$Z ]; then
        log "     - Copying ZooKeeper files for instance $Z"
        cp -r $ZOO zoo$Z
    fi
    ZCONF="zoo$Z/conf/zoo.cfg"
    if [ ! -s "$ZCONF" ]; then
        log "     - Creating new setup for ZooKeeper $Z"
        echo "dataDir=`pwd`/zoo$Z/data" >> "$ZCONF"
        echo "clientPort=$ZPORT" >> "$ZCONF"
    fi
    ZPORT=$(( ZPORT + 1 ))
done

popd > /dev/null
