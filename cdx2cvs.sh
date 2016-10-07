#!/bin/bash

EXPECTED_HEADER=" CDX N b a m s k r M S V g"
CSV_HEADER="id,url,date,ourl,mime,response,newdigest,redirect,offset,arc,sdomain,path"

>&2 echo "Warning: This script has been deprecated as it is very slow"
>&2 echo "and does not escape backslashes in URLs."
>&2 echo "Consult README.md for a Java alternative."

function ispresentc() {
    if [ "-" != "$1" -a "." != ".$1" ]; then
        echo -n "$1,"
    else
        echo -n ","
    fi
}
function ispresent() {
    if [ "-" != "$1" -a "." != ".$1" ]; then
        echo -n "$1"
    fi
}

# 20110225190956 -> 2011-02-25T19:09:56Z
function convert_date() {
    local IN="$1"
    echo "${IN:0:4}-${IN:4:2}-${IN:6:2}T${IN:8:2}:${IN:10:2}:${IN:12:2}Z"
}


# Expects CDX & CSV
function convert_csv() {
    if [ -s "$CSV" ]; then
        echo "Skipping existing output file $CSV"
        return
    fi
    echo "$CDX -> $CSV"

    echo "$CSV_HEADER" > "$CSV"
    while IFS= read -r LINE; do
        # In:   N b a m s k r M S V g
        # Solr: A b e a m s c k r V v D d g M n
        # Mix:  N b a m s k r V g sdomain path
        # Name: url,date,ourl,mime,response,newdigest,redirect,offset,arc 

        # http://stackoverflow.com/questions/1469849/how-to-split-one-string-into-multiple-strings-separated-by-at-least-one-space-in
        IFS=' ' read -ra TOKENS < <(echo "$LINE")
        # id=url+date
        ispresent ${TOKENS[5]} >> "$CSV"
        ispresentc ${TOKENS[1]} >> "$CSV"

        ispresentc ${TOKENS[0]} >> "$CSV" # N
        ispresentc `convert_date ${TOKENS[1]}` >> "$CSV" # b
        ispresentc ${TOKENS[2]} >> "$CSV" # a
        ispresentc ${TOKENS[3]} >> "$CSV" # m
        ispresentc ${TOKENS[4]} >> "$CSV" # s
        ispresentc ${TOKENS[5]} >> "$CSV" # k
        ispresentc ${TOKENS[6]} >> "$CSV" # r

        ispresentc ${TOKENS[9]} >> "$CSV" # V
        ispresentc ${TOKENS[10]} >> "$CSV" #G

        echo -n ${TOKENS[0]} | cut -d\) -f1 | tr -d '\n' >> "$CSV"  # sdomain
        echo -n "," >> "$CSV"
        local PATHF=`echo -n ${TOKENS[0]} | cut -d\) -f2`
        echo -n ${PATHF:1} >> "$CSV" # path 

        echo "" >> "$CSV"
    done < <(less "$CDX" | sed 's/,/\\,/g' | tail -n +2)
}

for CDX in $@; do
    if [ -d "$CDX" ]; then
        echo "Error: Unable to read CDX file $CDX" 1>&2
        continue;
    fi
    HEADER=`less "$CDX" | head -n 1`
    if [ "$HEADER" != "$EXPECTED_HEADER" ]; then
        echo "Error: Expected header \"$EXPECTED_HEADER\" in $CDX but got \"$HEADER\"" 1>&2
        continue
    fi
    CSV=${CDX%.*}.csv
    convert_csv
done
