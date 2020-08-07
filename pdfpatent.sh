#! /bin/bash

# nohup ./pdfpatent.sh NO pdf > script.out &

COUNTRY=${1:-HR}
COMMAND=${2:-report}
YEAR_START=${3:-1994}
YEAR_END=${4:-2019}

SCRIPTDIR=${HOME}/tmp
SCRIPT=${SCRIPTDIR}/${COMMAND}-script-`date -Iseconds`.txt

mkdir -p ${SCRIPTDIR}

for (( year=${YEAR_START}; year<=${YEAR_END}; year++ ))
do
    echo ${COUNTRY} $year ${COMMAND} >> ${SCRIPT}
done

nice -n10 java -jar build/libs/pdfpatents.jar -f ${SCRIPT}

rm ${SCRIPT}
