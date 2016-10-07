#!/bin/bash

#
# Indexes CDX files to Solr, using the solr-cdx server to convert from basic CDX to Solr CVS
#
# Limitation: Does not handle file names with spaces.
#

pushd `dirname $0` > /dev/null
source "scripts.default.conf"
if [ -s "scripts.conf" ]; then
    source "scripts.conf"
fi

export BASE_URL="http://$HOST:$SOLR_BASE_PORT/solr/$COLLECTION/update/csv?&separator=,&escape=\\&commit=true&stream.url=http://$HOST:$SOLRCDX_PORT/convert?stream.file="

function err() {
    >&2 echo "$2"
    exit $1
}

# http://stackoverflow.com/questions/3915040/bash-fish-command-to-print-absolute-path-to-a-file
function realpath {
    echo $(cd $(dirname $1); pwd)/$(basename $1)
}
export -f realpath

function index() {
    local FILE="$1"
    if [ ! -s "$FILE" ]; then
        >&2 echo "Error: The file $FILE does not exist"
        return
    fi
    local URL="$BASE_URL`realpath $FILE`"
    echo "Calling $URL"
    curl -s "$URL"
}
export -f index

FC=`echo "$@" | wc -w`
IT=$INDEX_THREADS
if [ $INDEX_THREADS -gt $FC ]; then
    IT=$FC
fi
echo "Indexing $FC CDX files using $IT threads"
echo -n "$@" | xargs -P $INDEX_THREADS -n 1 -I {} -d\  bash -c 'index "{}"'
echo ""
echo "Done. Inspect the collection at http://$HOST:$SOLR_BASE_PORT/solr/#/$COLLECTION/collection-overview"
