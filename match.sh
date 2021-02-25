#! /bin/bash

# ./match.sh NO 2018 2019 | more
# ./match.sh PL 1980 2020 title | more

# exit on the first error
set -e

# The environment variable COUNT_PDFS (set externally) determines
# whether we count all matching patents, or only those where we have
# downloaded the PDF files.

COUNTRY=${1:-HR}
YEAR_START=${2:-1980}
YEAR_END=${3:-${2:-2020}}
FIELD=${4}

SCRIPTDIR="${HOME}/tmp"
FAMILYDIR="${FAMILYDIR:-/fs/bil0/europat/family}"
INFODIR="${INFODIR:-/data/patents/pdfpatents}"

# "catch exit status 1" grep wrapper
c1grep() { grep "$@" || test $? = 1; }

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
SINGLEFIELDPATTERN='^[^[:space:]]+'
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
            if [[ ! -z "${FIELD}" ]]; then
                # find matches for a specific field
                case "${FIELD}" in
                    "title")
                        INDEX=4
                        ;;
                    "abstract")
                        INDEX=5
                        ;;
                    "claims")
                        INDEX=6
                        ;;
                    "description")
                        INDEX=7
                        ;;
                    *)
                        echo -e "Unknown field ${FIELD}"
                        exit 1
                        ;;
                esac
                COUNT=`c1grep -F -f "${MATCHFILE}" "${INFOFILE}" | cut -f"${INDEX}" | c1grep -cP "${SINGLEFIELDPATTERN}"`
            elif [[ -z "${COUNT_PDFS}" ]]; then
                COUNT=`c1grep -cF -f "${MATCHFILE}" "${INFOFILE}"`
            else
                # find rows with a missing text field and with PDFs available
                COUNT=`c1grep -F -f "${MATCHFILE}" "${INFOFILE}" | cut -f4-8 | c1grep -vP "${FIELDPATTERN}" | c1grep -P "${PDFPATTERN}" | wc -l`
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
