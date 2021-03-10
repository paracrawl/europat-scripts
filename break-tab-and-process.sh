#!/bin/bash

file=${1}
lang=${2:-fr}
mode=${3:-test} # or test
nparts=10
size=$(( 1 + ($(cat $file | wc -l) / $nparts ) ))
if [ "$mode" != "test" ]; then
	split -l $size -a 1 $file && \
	mv xa $(basename $file .tab)-0.tab && \
	mv xb $(basename $file .tab)-1.tab && \
	mv xc $(basename $file .tab)-2.tab && \
	mv xd $(basename $file .tab)-3.tab && \
	mv xe $(basename $file .tab)-4.tab && \
	mv xf $(basename $file .tab)-5.tab && \
	mv xg $(basename $file .tab)-6.tab && \
	mv xh $(basename $file .tab)-7.tab && \
	mv xi $(basename $file .tab)-8.tab && \
	mv xj $(basename $file .tab)-9.tab && \
#	mv xk $(basename $file .tab)-a.tab
#	mv xl $(basename $file .tab)-b.tab
	echo File:"$file" Split size: "$size" lines
	for file_n in $(seq 0 $(( $nparts - 1 ))); do
		sbatch -J "$lang"en"$file_n"$mode ~/sw/europat-scripts/pipeline.sh $lang ../../models/$lang-en/translate.sh -- $(basename $file .tab)-$file_n.tab
		echo scheduling job for file: $(basename $file .tab)-$file_n.tab 
	done
else
	echo File:"$file" Split size: "$size" lines
	echo "Scheduling jobs: "sbatch -J "$lang"en$mode ~/sw/europat-scripts/pipeline.sh $lang ../../../models/$lang-en/translate.sh -- $(basename $file .tab)-0\-\>$(( $nparts - 1 )).tab
fi
sleep 2
