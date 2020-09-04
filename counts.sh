#! /bin/bash

# ./counts.sh NO 2018 | more
# (PDF_PAGE_LIMIT=10 ./counts.sh HR 1996)

COUNTRY=${1:-HR}
YEAR_START=${2:-1994}
YEAR_END=${3:-${2:-2019}}

SCRIPTDIR="${HOME}/tmp"
INFODIR="${INFODIR:-/fs/loki0/data/pdfpatents}"
PDF_PAGE_LIMIT="${PDF_PAGE_LIMIT:-25}"

mkdir -p "${SCRIPTDIR}"

TEXT_PATTERN='^[^[:space:]]+\t[^[:space:]]+\t[^[:space:]]+\t[^[:space:]]+'
for (( YEAR="${YEAR_START}"; YEAR<="${YEAR_END}"; YEAR++ ))
do
    YEARDIR="${INFODIR}/${COUNTRY}-${YEAR}"
    if [ ! -d "${YEARDIR}" ]; then
        echo "${YEAR}: no results"
        continue
    fi
    INFOFILE=`ls "${YEARDIR}"/*-info.txt`
    TMPFILE="${SCRIPTDIR}/`date -Idate`-${COUNTRY}-${YEAR}-info.txt"

    # count downloaded entries in the main language
    FILE_PREFIX="${COUNTRY}-${COUNTRY}-${YEAR}"
    TITLE_DOWNLOADS=`find "${YEARDIR}" -maxdepth 1 -type f -name "${FILE_PREFIX}-title.tab" | xargs wc -l | cut -d' ' -f1`
    ABSTRACT_DOWNLOADS=`find "${YEARDIR}" -maxdepth 1 -type f -name "${FILE_PREFIX}-abstract.tab" | xargs wc -l | cut -d' ' -f1`
    CLAIM_DOWNLOADS=`find "${YEARDIR}" -maxdepth 1 -type f -name "${FILE_PREFIX}-claim.tab" | xargs wc -l | cut -d' ' -f1`
    DESCRIPTION_DOWNLOADS=`find "${YEARDIR}" -maxdepth 1 -type f -name "${FILE_PREFIX}-desc.tab" | xargs wc -l | cut -d' ' -f1`
    PDF_DOWNLOADS=`ls "${YEARDIR}" | grep -c "\-1.pdf"`

    # count info file entries and text in the main language
    ENTRY_COUNT=`wc -l "${INFOFILE}" | cut -d' ' -f1`
    TITLE_COUNT=`cut -f4 "${INFOFILE}" | grep "${COUNTRY}" | wc -l`
    ABSTRACT_COUNT=`cut -f5 "${INFOFILE}" | grep "${COUNTRY}" | wc -l`
    CLAIM_COUNT=`cut -f6 "${INFOFILE}" | grep "${COUNTRY}" | wc -l`
    DESCRIPTION_COUNT=`cut -f7 "${INFOFILE}" | grep "${COUNTRY}" | wc -l`
    PDF_COUNT=`cut -f8 "${INFOFILE}" | grep "${COUNTRY}" | wc -l`

    # locate entries with PDFs and lacking at least one text part; filter by PDF page limit
    cut -f4- "${INFOFILE}" | grep -vP "${TEXT_PATTERN}" | awk -F "\t" -v limit="${PDF_PAGE_LIMIT}" '($6 > 0) && ($6 <= limit)' > "${TMPFILE}"

    # count filtered entries in the main language
    PDF_MATCH=`wc -l "${TMPFILE}" | cut -d' ' -f1`
    TITLE_MATCH=`cut -f1 "${TMPFILE}" | grep "${COUNTRY}" | wc -l`
    ABSTRACT_MATCH=`cut -f2 "${TMPFILE}" | grep "${COUNTRY}" | wc -l`
    CLAIM_MATCH=`cut -f3 "${TMPFILE}" | grep "${COUNTRY}" | wc -l`
    DESCRIPTION_MATCH=`cut -f4 "${TMPFILE}" | grep "${COUNTRY}" | wc -l`

    # print output
    printf "${COUNTRY} ${YEAR}: ${ENTRY_COUNT} entries\n"
    printf "%6d / %-5d PDFs are wanted\n" "${PDF_MATCH}" "${PDF_COUNT}"
    printf "%6d / %-5d titles have wanted PDFs\n" "${TITLE_MATCH}" "${TITLE_COUNT}"
    printf "%6d / %-5d abstracts have wanted PDFs\n" "${ABSTRACT_MATCH}" "${ABSTRACT_COUNT}"
    printf "%6d / %-5d claims have wanted PDFs\n" "${CLAIM_MATCH}" "${CLAIM_COUNT}"
    printf "%6d / %-5d descriptions have wanted PDFs\n" "${DESCRIPTION_MATCH}" "${DESCRIPTION_COUNT}"
    printf "%6d titles downloaded\n" "${TITLE_DOWNLOADS}"
    printf "%6d abstracts downloaded\n" "${ABSTRACT_DOWNLOADS}"
    printf "%6d claims downloaded\n" "${CLAIM_DOWNLOADS}"
    printf "%6d descriptions downloaded\n" "${DESCRIPTION_DOWNLOADS}"
    printf "%6d PDFs downloaded\n" "${PDF_DOWNLOADS}"

    rm "${TMPFILE}"
done
