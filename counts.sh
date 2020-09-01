#! /bin/bash

# ./counts.sh NO 2018 2019 | more

COUNTRY=${1:-HR}
YEAR_START=${2:-1994}
YEAR_END=${3:-2019}

SCRIPTDIR="${HOME}/tmp"
INFODIR="/fs/loki0/data/pdfpatents"

TEXT_PATTERN='^[^[:space:]]+\t[^[:space:]]+\t[^[:space:]]+\t[^[:space:]]+'
for (( YEAR="${YEAR_START}"; YEAR<="${YEAR_END}"; YEAR++ ))
do
    YEARDIR="${INFODIR}/${COUNTRY}-${YEAR}"
    if [ ! -d "${YEARDIR}" ]; then
        echo "${YEAR}: no results"
        continue
    fi
    INFOFILE=`ls "${YEARDIR}"/*-info.txt`

    # find entries with parts as text
    ENTRIES=`wc -l ${INFOFILE} | cut -d' ' -f1`
    TITLECOUNT=`cut -f4 "${INFOFILE}" | grep "${COUNTRY}" | wc -l`
    ABSTRACTCOUNT=`cut -f5 "${INFOFILE}" | grep "${COUNTRY}" | wc -l`
    CLAIMCOUNT=`cut -f6 "${INFOFILE}" | grep "${COUNTRY}" | wc -l`
    DESCRIPTIONCOUNT=`cut -f7 "${INFOFILE}" | grep "${COUNTRY}" | wc -l`
    PDFCOUNT=`cut -f8 "${INFOFILE}" | grep "${COUNTRY}" | wc -l`

    # find rows with parts as text and with PDFs available
    TITLEMATCH=`cut -f4-8 "${INFOFILE}" | grep -vP "${TEXT_PATTERN}" | cut -f1,5  | grep -P "${COUNTRY}.*${COUNTRY}" | wc -l`
    ABSTRACTMATCH=`cut -f4-8 "${INFOFILE}" | grep -vP "${TEXT_PATTERN}" | cut -f2,5  | grep -P "${COUNTRY}.*${COUNTRY}" | wc -l`
    CLAIMMATCH=`cut -f4-8 "${INFOFILE}" | grep -vP "${TEXT_PATTERN}" | cut -f3,5  | grep -P "${COUNTRY}.*${COUNTRY}" | wc -l`
    DESCRIPTIONMATCH=`cut -f4-8 "${INFOFILE}" | grep -vP "${TEXT_PATTERN}" | cut -f4,5  | grep -P "${COUNTRY}.*${COUNTRY}" | wc -l`
    PDFMATCH=`cut -f4-8 "${INFOFILE}" | grep -vP "${TEXT_PATTERN}" | cut -f5  | grep -P "${COUNTRY}" | wc -l`
    PDFDOWNLOADS=`ls "${YEARDIR}" | grep -c "\-1.pdf"`

    # print output
    printf "${YEAR}: ${ENTRIES} entries, ${PDFCOUNT} with PDFs\n"
    printf "%6d / %-5d titles with PDFs\n" "${TITLEMATCH}" "${TITLECOUNT}"
    printf "%6d / %-5d abstracts with PDFs\n" "${ABSTRACTMATCH}" "${ABSTRACTCOUNT}"
    printf "%6d / %-5d claims with PDFs\n" "${CLAIMMATCH}" "${CLAIMCOUNT}"
    printf "%6d / %-5d descriptions with PDFs\n" "${DESCRIPTIONMATCH}" "${DESCRIPTIONCOUNT}"
    printf "%6d / %-5d PDFs downloaded\n" "${PDFDOWNLOADS}" "${PDFMATCH}"
done
