#!/bin/bash
# Break a (locally coppied) -bleualign-input.tab.gz file and align it
file=${1}
lang=${2:-fr}
mode=${3:-test} # or test
nparts=10
size=$(( 1 + ($(zcat $file | wc -l) / $nparts ) ))
if [ "$mode" != "test" ]; then
	gzip -d $file
	split -l $size -a 1 $(basename $file .gz)
	mv xa $(basename $file -bleualign-input.tab.gz)-bleualign-input-0.tab
	mv xb $(basename $file -bleualign-input.tab.gz)-bleualign-input-1.tab
	mv xc $(basename $file -bleualign-input.tab.gz)-bleualign-input-2.tab
	mv xd $(basename $file -bleualign-input.tab.gz)-bleualign-input-3.tab
	mv xe $(basename $file -bleualign-input.tab.gz)-bleualign-input-4.tab
	mv xf $(basename $file -bleualign-input.tab.gz)-bleualign-input-5.tab
	mv xg $(basename $file -bleualign-input.tab.gz)-bleualign-input-6.tab
	mv xh $(basename $file -bleualign-input.tab.gz)-bleualign-input-7.tab
	mv xi $(basename $file -bleualign-input.tab.gz)-bleualign-input-8.tab
	mv xj $(basename $file -bleualign-input.tab.gz)-bleualign-input-9.tab
	echo File:"$file" Split size: "$size" lines
	sbatch ~/sw/europat-scripts/align-cpu.sh $lang ../../models/$lang-en/translate.sh -- $(ls $(basename $file -bleualign-input.tab.gz)-bleualign-input-?.tab)
else
	echo File:"$file" Split size: "$size" lines
fi

