#! /bin/bash

# ./match.sh NO 2018 2019 | more

# The environment variable COUNT_PDFS (set externally) determines
# whether we count all matching patents, or only those where we have
# downloaded the PDF files.

COUNTRY=${1:-HR}
YEAR_START=${2:-1994}
YEAR_END=${3:-2019}

SCRIPTDIR="${HOME}/tmp"
FAMILYDIR="/fs/bil0/europat/family"
INFODIR="/fs/loki0/data/pdfpatents"

for (( MATCHYEAR="${YEAR_START}"; MATCHYEAR<="${YEAR_END}"; MATCHYEAR++ ))
do
    MATCHFILE="${SCRIPTDIR}/${COUNTRY}-${MATCHYEAR}-matched.txt"
    if [ ! -s "${MATCHFILE}" ]; then
        TABFILE="${FAMILYDIR}/EN-${COUNTRY}/EN-${COUNTRY}-${MATCHYEAR}-FamilyID.tab"
        if [ -f "${TABFILE}" ]; then
            grep -o "${COUNTRY}[0-9A-Z\-]*" "${TABFILE}" | sed s/-/./g > "${MATCHFILE}"
        fi
    fi
done

YEARS="year"
TOTALS="total"
for (( YEAR="${YEAR_START}"; YEAR<="${YEAR_END}"; YEAR++ ))
do
    COUNTS=""
    YEARDIR="${INFODIR}/${COUNTRY}-${YEAR}"
    if [[ -z "${COUNT_PDFS}" ]]; then
        INFOFILE=`ls "${YEARDIR}"/*-info.txt`
    else
        INFOFILE="${SCRIPTDIR}/${COUNTRY}-${YEAR}-pdfs.txt"
        ls "${YEARDIR}" | grep '\-1.pdf' | cut -d'-' -f1-3 | sed s/-/./g > "${INFOFILE}"
    fi
    # iterate through match years in reverse order
    for (( MATCHYEAR="${YEAR_END}"; MATCHYEAR>="${YEAR_START}"; MATCHYEAR-- ))
    do
        MATCHFILE="${SCRIPTDIR}/${COUNTRY}-${MATCHYEAR}-matched.txt"
        if [ -f "${MATCHFILE}" ]; then
            if [[ ! -z "${YEARS}" ]]; then
                YEARS="${YEARS}\t${MATCHYEAR}"
            fi
            COUNT=`grep -F -f "${MATCHFILE}" "${INFOFILE}" | wc -l`
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
