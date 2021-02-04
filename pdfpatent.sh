#! /bin/bash

# nohup ./pdfpatent.sh pdf NO 2018 > script.out &

# exit on the first error
set -e

COMMAND=${1:-report}
COUNTRY=${2:-HR}
YEAR_START=${3:-1980}
YEAR_END=${4:-${3:-2020}}

if [ "$#" -gt 4 ]; then
    shift 4
else
    shift "$#"
fi

SCRIPTDIR="${HOME}/tmp"
SCRIPT="${SCRIPTDIR}/${COMMAND}-script-`date -Iseconds`.txt"

mkdir -p "${SCRIPTDIR}"

for (( YEAR="${YEAR_END}"; YEAR>="${YEAR_START}"; YEAR-- ))
do
    echo "${COUNTRY}" "${YEAR}" "${COMMAND}" >> "${SCRIPT}"
done

nice -n10 java -jar build/libs/pdfpatent.jar -f "${SCRIPT}" "$@"

rm "${SCRIPT}"
