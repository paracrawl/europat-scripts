#! /bin/bash

# ./backup.sh 1993 1995 | more

# Back up the original files (containing text from Google Translate)
# and make the backup copies non-readable.

# exit on the first error
set -e

YEAR_START=${1:-1980}
YEAR_END=${2:-${1:-2020}}

COUNTRY="ES"
INFODIR="${INFODIR:-/data/patents/pdfpatents}"

for (( YEAR="${YEAR_START}"; YEAR<="${YEAR_END}"; YEAR++ ))
do
    YEARDIR="${INFODIR}/${COUNTRY}-${YEAR}"
    for SUFFIX in "abstract" "title" "claim" "desc"; do
        BASENAME="$COUNTRY-en-$YEAR-$SUFFIX.tab"
        ORIGFILE="$YEARDIR/$BASENAME"
        BACKUPFILE="$YEARDIR/all-$BASENAME"
        if [ -f "$ORIGFILE" ]; then
            cp -n "$ORIGFILE" "$BACKUPFILE"
            chmod 400 "$BACKUPFILE"
        fi
    done
done
