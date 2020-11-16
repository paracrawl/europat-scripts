#! /bin/bash

# nohup ./pdfyears.sh prepdf 1970 1979 > script.out &

COMMAND=${1:-report}
YEAR_START=${2:-1980}
YEAR_END=${3:-${2:-2019}}

if [ "$#" -gt 3 ]; then
    shift 3
else
    shift "$#"
fi

COUNTRIES="${COUNTRIES:-HR NO PL ES FR GR}"
SCRIPTDIR="${HOME}/tmp"
SCRIPT="${SCRIPTDIR}/${COMMAND}-script-`date -Iseconds`.txt"

mkdir -p "${SCRIPTDIR}"

for (( YEAR="${YEAR_END}"; YEAR>="${YEAR_START}"; YEAR-- ))
do
    for COUNTRY in ${COUNTRIES}
    do
        echo "${COUNTRY}" "${YEAR}" "${COMMAND}" >> "${SCRIPT}"
    done
done

nice -n10 java -jar build/libs/pdfpatent.jar -f "${SCRIPT}" "$@"

rm "${SCRIPT}"
