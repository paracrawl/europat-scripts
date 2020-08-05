#! /bin/bash

# nohup ./pdfpatent.sh NO pdf > script.out &

COUNTRY=${1:-HR}
COMMAND=${2:-report}

SCRIPTDIR=${HOME}/tmp
SCRIPT=${SCRIPTDIR}/${COMMAND}-script-`date -Iseconds`.txt

mkdir -p ${SCRIPTDIR}

for (( year=1994; year<2020; year++ ))
do
  echo ${COUNTRY} $year ${COMMAND} >> ${SCRIPT}
done

nice -n10 java -jar build/libs/pdfpatents.jar -f ${SCRIPT}

rm ${SCRIPT}
