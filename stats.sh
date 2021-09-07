#/bin/bash
set -euo pipefail

split-lines() {
	sed -r 's/<br\/>|<\/p><p>/\n/g' \
	| sed -r 's/<\/?p>//g'
}

col() {
	cut -d$'\t' -f$1
}

stats () {
	printf "%s\t%d\t%d\t%d\n" \
		"$1" \
		$(< "$1" wc -l) \
		$(< "$1" col 2 | split-lines | wc -l) \
		$(< "$1" col 4 | split-lines | wc -l)
}

for file in "$@"; do
	stats "$file"
done

