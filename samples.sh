#! /bin/bash

# ./samples.sh ES 2010 &

# exit on the first error
set -e

COUNTRY=${1:-HR}
YEAR_START=${2:-1800}
YEAR_END=${3:-${2:-2021}}

if [ "$#" -gt 3 ]; then
    shift 3
else
    shift "$#"
fi

INFODIR="${INFODIR:-/data/patents/pdfpatents}"
DSTDIR="${INFODIR}/sample_patents"

mkdir -p "${DSTDIR}"

for (( YEAR="${YEAR_END}"; YEAR>="${YEAR_START}"; YEAR-- ))
do
    YEARDIR="${COUNTRY}-${YEAR}"
    SRCDIR="${INFODIR}/${YEARDIR}/sample"

    if [ -d "${SRCDIR}" ]
    then
        tar -Pp --transform "flags=r;s|${SRCDIR}|${YEARDIR}|" \
            -czf "${DSTDIR}/${YEARDIR}.tgz" "${SRCDIR}"
    fi
done
