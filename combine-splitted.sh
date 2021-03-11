#!/bin/bash
set -euo pipefail
DIR=${1:-"FR-EN-2020-Claim-1"}
TYPE=${2:-aligned}
if [[ $1 == "--help" ]];then
	echo -e "Recombine processed split files into one file and clean-up processing dir"$'\n'
	echo -e "Usage:"$'\n'
	echo -e "./combine-splitted.sh DIRNAME type"$'\n'
	echo "type = aligned/translated"
	exit 0
fi
cd "$DIR"
echo Processing dir: $DIR
n_parts=20 #$(ls x? | wc -l)
ls -lht > "$DIR"-file-sizes.txt
if [[ $TYPE == aligned ]]; then
	NAME="$DIR"-aligned.gz
	ls *-aligned.* >  "$DIR"-alignment-errors-"$n_parts"parts.txt
	cat x?-aligned | gzip -9c > "$NAME"
	mv "$NAME" ../
	rm $(ls x?-aligned | sed -r "s/-aligned\.????????//g") x?-aligned x?-aligned.????????
else
	if [[ $TYPE == translated ]]; then
		NAME="$DIR"-translated.gz
		ls *-translated.gz.* >  "$DIR"-translation-errors-"$n_parts"parts.txt
		zcat x?-translated.gz | gzip -9c > "$NAME"
		mv "$NAME" ../
		rm $(ls x?-translated.gz | sed -r "s/-translated\.gz\.????????//g") x?-translated.gz x?-translated.gz.*
		NAME="$DIR"-aligned.gz
		ls *-aligned.gz.* >  "$DIR"-alignment-errors-"$n_parts"parts.txt
		zcat x?-aligned.gz | gzip -9c > "$NAME"
		mv "$NAME" ../
		rm x?-aligned.gz x?-aligned.gz.????????
	fi
fi
cd ../
