#!/bin/bash
set -euo pipefail

column () {
	cut -d$'\t' -f$1
}

for f in $*; do
	BASENAME=${f##*/}
	L1=${BASENAME%%-*}
	pigz -cd $f \
	| while read -r doc_id label rest; do
		if [[ $label != "__label__${L1,,}" ]]; then
			echo "http://europat-ocr.ikhoefgeen.nl/#${BASENAME%%.*}/$doc_id" $label $rest
		fi
	done
done

