#!/bin/bash
set -euo pipefail

N=${N:-4}

for file in "$@"; do
	name=$(basename $file .tab)
	split \
		--number=r/$N \
		--additional-suffix=.tab \
		--numeric-suffixes \
		--suffix-length=1 \
		$1 \
		${name}-
	ls -1d ${name}-?.tab
done

