#!/bin/bash
set -euo pipefail
file=${1}
lang=${2:-fr}
nparts=${3:-10}
mode=${4:-test} # test or align
if [ "$1" = "--help" ]; then
	echo -e $'\n'"Split large file that refuses to go through the pipeline into pieces and either process fully (if tab input) or align only (if bleualign-input)"$'\n'
	echo General usage:
	echo . split-and-align.sh file lang nparts mode
	echo -e "where:"$'\n'
	echo mode = test/align/translate
	echo nparts = number of splits to perform
	echo . split-and-align.sh FR-EN-2020-Claim-1-bleualign-input.tab.gz fr 25 align
	echo . split-and-align.sh FR-EN-2020-Claim-1.tab fr 25 translate
	echo -e ". split-and-align.sh FR-EN-2020-Claim-1.tab fr 25 test"$'\n'
	exit 1
fi

	
if [[ $file == *.gz ]] ; then
	size=$(( 1 + ($(zcat $file | wc -l) / $nparts ) ))
else
	size=$(( 1 + ($(cat $file | wc -l) / $nparts ) ))
fi

echo Processing File:"$file" Split size: "$size" lines
if [ "$mode" = "test" ]; then
	echo File:"$file" Split size: "$size" lines
	exit 0
fi

if [ "$mode" = "align" ]; then
	NAME=$(basename "$file" -bleualign-input.tab.gz)
	mkdir -p "$NAME" 
	gzip -d $file && \
	split -l $size -a 1 $(basename $file .gz) && \
	mv x? "$NAME"/ && cd "$NAME" 
	sbatch -J align"$NAME" ~/sw/europat-scripts/align-cpu.sh $lang /rds/project/rds-48gU72OtDNY/europat/models/$lang-en/translate.sh -- $(ls x?)
else
	NAME=$(basename "$file" .tab)
	mkdir -p "$NAME" 
	split -l $size -a 1 $file && \
	mv x? "$NAME"/ && cd "$NAME" 
	sbatch -J translate"$NAME" ~/sw/europat-scripts/pipeline.sh $lang /rds/project/rds-48gU72OtDNY/europat/models/$lang-en/translate.sh -- $(ls x?)
fi

