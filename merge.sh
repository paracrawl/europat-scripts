#!/bin/bash
set -euo pipefail
N=${N:-4}

combine() {
	local target=$1
	local name=$(basename $target -bleualign-input.tab.gz)
	local -a parts=()
	if [ -f $target ]; then
		return 0
	fi
	for i in $(seq 0 $((N-1))); do
		part=${name}-${i}-bleualign-input.tab.gz
		if [ ! -f $part ]; then
			echo "Part not found: $part" >&2
			return 1
		fi
		parts=(${parts[@]:-} $part)
	done
	zcat ${parts[@]} | gzip -9c > $target.$$
	mv $target.$$ $target
	#rm ${parts[@]}
}

for target in $@; do
	combine $target || true
done

