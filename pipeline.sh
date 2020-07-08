#!/bin/bash

#
# In each of the models a translate.sh file is expected, something that looks like:
#
#   #!/bin/bash
#   set -euo pipefail
# 
#   cd $(dirname $0)
#   
#   $BPE_BIN \
#     -c deen.bpe \
#     | $MARIAN_BIN "$@" \
#       -d $GPU \
#       --vocabs train.bpe.de.json train.bpe.en.json \
#       -m model.NMT104080k1060k1080k.npz \
#     | sed -r 's/\@\@ //g' \
#     | sed -r 's/\@\@$//g'
#

# How many bleualign?
THREADS=${THREASD:-4}

export BPE_BIN="subword-nmt apply-bpe"
export MARIAN_BIN=marian-decoder

export SLANG=$1
shift

export GPU=$1
shift

case $SLANG in
	de)
		MODEL="DC01311513/nmt/v1"
		;;
	fr)
		MODEL="DC01311514/nmt/v1"
		;;
	*)
		echo "Supported languages: de, fr" 1>&2
		exit 1
		;;
esac

col () {
	cut -d$'\t' -f$1
}

document_to_base64() {
	sed -r 's/$/\x0/g' \
	| sed -r 's/<br\/>|<\/p><p>/\n/g' \
	| sed -r 's/<\/?p>//g' \
	| docenc -0
}

translate () {
	b64filter foldfilter -w 1000 $MODEL/translate.sh --quiet-translation
	#b64filter foldfilter -w 1000 tee >(gzip -9c > $(basename $file .tab)-translate.txt.gz)
}

for file in $*; do
	paste \
		<(cat $file | col 1) \
		<(cat $file | col 3) \
		<(cat $file | col 2 | document_to_base64) \
		<(cat $file | col 4 | document_to_base64) \
		<(cat $file | col 2 | document_to_base64 | translate) \
	| tee >(gzip -9c > $(basename $file .tab)-bleualign-input.tab.gz) \
	| parallel \
		--halt 2 \
		--pipe \
		-k \
		-l 1 \
		-j $THREADS \
		bleualign --bleu-threshold 0.2 \
	| gzip \
	> $(basename $file .tab)-aligned.gz
done
