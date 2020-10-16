#! /bin/bash

# ./match.sh NO 2018 2019 | more

# The environment variable COUNT_PDFS (set externally) determines
# whether we count all matching patents, or only those where we have
# downloaded the PDF files.

COUNTRY=${1:-HR}
YEAR_START=${2:-1980}
YEAR_END=${3:-${2:-2019}}

SCRIPTDIR="${HOME}/tmp"
FAMILYDIR="${FAMILYDIR:-/fs/bil0/europat/family}"
INFODIR="${INFODIR:-/fs/loki0/data/pdfpatents}"

for (( MATCHYEAR="${YEAR_START}"; MATCHYEAR<="${YEAR_END}"; MATCHYEAR++ ))
do
    MATCHFILE="${SCRIPTDIR}/${COUNTRY}-${MATCHYEAR}-matched.txt"
    if [ ! -s "${MATCHFILE}" ]; then
        TABFILE="${FAMILYDIR}/EN-${COUNTRY}/EN-${COUNTRY}-${MATCHYEAR}-FamilyID.tab"
        if [ -f "${TABFILE}" ]; then
            cut -f3 "${TABFILE}" | sed s/-/./g > "${MATCHFILE}"
        fi
    fi
done

YEARS="year"
TOTALS="total"
FIELDPATTERN='^[^[:space:]]+\t[^[:space:]]+\t[^[:space:]]+\t[^[:space:]]+'
PDFPATTERN='\t[^[:space:]]+$'
for (( YEAR="${YEAR_START}"; YEAR<="${YEAR_END}"; YEAR++ ))
do
    COUNTS=""
    YEARDIR="${INFODIR}/${COUNTRY}-${YEAR}"
    INFOFILE=`ls "${YEARDIR}"/*-info.txt`
    # iterate through match years in reverse order
    for (( MATCHYEAR="${YEAR_END}"; MATCHYEAR>="${YEAR_START}"; MATCHYEAR-- ))
    do
        MATCHFILE="${SCRIPTDIR}/${COUNTRY}-${MATCHYEAR}-matched.txt"
        if [ -f "${MATCHFILE}" ]; then
            if [[ ! -z "${YEARS}" ]]; then
                YEARS="${YEARS}\t${MATCHYEAR}"
            fi
            if [[ -z "${COUNT_PDFS}" ]]; then
                COUNT=`grep -cF -f "${MATCHFILE}" "${INFOFILE}"`
            else
                # find rows with a missing text field and with PDFs available
                COUNT=`grep -F -f "${MATCHFILE}" "${INFOFILE}" | cut -f4-8 | grep -vP "${FIELDPATTERN}" | grep -P "${PDFPATTERN}" | wc -l`
            fi
            COUNTS="${COUNTS}\t${COUNT}"
            if [[ "${YEAR}" = "${YEAR_END}" ]]; then
                TOTAL=`wc -l "${MATCHFILE}" | cut -d' ' -f1`
                TOTALS="${TOTALS}\t${TOTAL}"
            fi
        fi
    done

    # print output
    if [[ ! -z "${YEARS}" ]]; then
        echo -e "${YEARS}"
        YEARS=""
    fi
    echo -e "${YEAR}${COUNTS}"
done

echo -e "\n${TOTALS}"
