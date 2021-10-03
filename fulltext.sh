#! /bin/bash

# collect together the patent text for the given years into a tgz file

# ./fulltext.sh HR &
# ./fulltext.sh ES 2010 &

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
TMPDIR="${TMPDIR:-/tmp}"
TMPFILE="${TMPDIR}/${COUNTRY}.tar"
DSTDIR="${INFODIR}/fulltext"
DSTFILE="${DSTDIR}/${COUNTRY}_text.tgz"

mkdir -p "${DSTDIR}"

# create the empty tarball
> "${TMPFILE}"
for (( YEAR="${YEAR_START}"; YEAR<="${YEAR_END}"; YEAR++ ))
do
    YEARDIR="${COUNTRY}-${YEAR}"
    SRCDIR="${INFODIR}/${YEARDIR}"
    SRCFILE="${SRCDIR}/${YEARDIR}-text.tab"

    if [ -r "${SRCFILE}" ]
    then
        # append the file to the tarball, stripping the path
        tar -Pp --transform "flags=r;s|${SRCDIR}/||" \
            -rf "${TMPFILE}" "${SRCFILE}"
    fi
done

# compress the tarball
gzip -9 < "${TMPFILE}" > "${DSTFILE}"
rm "${TMPFILE}"
