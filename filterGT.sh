#! /bin/bash

# ./filterGT.sh 1993 1995 | more

# If we have already backed up the original file, this script will
# filter the entries to retain only the lines which are not from
# Google Translate

# exit on the first error
set -e

YEAR_START=${1:-1980}
YEAR_END=${2:-${1:-2020}}

COUNTRY="ES"
INFODIR="${INFODIR:-/data/patents/pdfpatents}"

PATTERN="Google Translate"

# "catch exit status 1" grep wrapper
c1grep() { grep "$@" || test $? = 1; }

for (( YEAR="${YEAR_START}"; YEAR<="${YEAR_END}"; YEAR++ ))
do
    YEARDIR="${INFODIR}/${COUNTRY}-${YEAR}"
    for SUFFIX in "abstract" "title" "claim" "desc"; do
        BASENAME="$COUNTRY-en-$YEAR-$SUFFIX.tab"
        ORIGFILE="$YEARDIR/$BASENAME"
        BACKUPFILE="$YEARDIR/all-$BASENAME"
        if [ -f "$BACKUPFILE" ]; then
            c1grep -v "$PATTERN" "$BACKUPFILE" > "$ORIGFILE"
        fi
    done
done
