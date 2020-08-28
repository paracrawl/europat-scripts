#! /bin/bash

# ./match.sh NO 2018 2019 | more

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
for (( YEAR="${YEAR_START}"; YEAR<="${YEAR_END}"; YEAR++ ))
do
    COUNTS=""
    YEARDIR="${INFODIR}/${COUNTRY}-${YEAR}"
    for (( MATCHYEAR="${YEAR_END}"; MATCHYEAR>="${YEAR_START}"; MATCHYEAR-- ))
    do
        MATCHFILE="${SCRIPTDIR}/${COUNTRY}-${MATCHYEAR}-matched.txt"
        if [ -f "${MATCHFILE}" ]; then
            if [[ ! -z "${YEARS}" ]]; then
                YEARS="${YEARS}\t${MATCHYEAR}"
            fi
            COUNT=`grep -F -f "${MATCHFILE}" "${YEARDIR}"/*-info.txt | wc -l`
            COUNTS="${COUNTS}\t${COUNT}"
        fi
    done

    # print output
    if [[ ! -z "${YEARS}" ]]; then
        echo -e "${YEARS}"
        YEARS=""
    fi
    echo -e "${YEAR}${COUNTS}"
done
