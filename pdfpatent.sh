#! /bin/bash

# nohup ./pdfpatent.sh NO pdf > script.out &

COUNTRY=${1:-HR}
COMMAND=${2:-report}
YEAR_START=${3:-1994}
YEAR_END=${4:-2019}

if [ "$#" -gt 4 ]; then
    shift 4
else
    shift "$#"
fi

SCRIPTDIR="${HOME}/tmp"
SCRIPT="${SCRIPTDIR}/${COMMAND}-script-`date -Iseconds`.txt"

mkdir -p "${SCRIPTDIR}"

for (( YEAR="${YEAR_START}"; YEAR<="${YEAR_END}"; YEAR++ ))
do
    echo "${COUNTRY}" "${YEAR}" "${COMMAND}" >> "${SCRIPT}"
done

nice -n10 java -jar build/libs/pdfpatents.jar -f "${SCRIPT}" "$@"

rm "${SCRIPT}"
