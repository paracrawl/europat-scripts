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

set -eou pipefail

# How many bleualign?
THREADS=${THREADS:-4}

ROOT=$(dirname $(realpath $0))

# Add the local bin to my path. This contains marian-decoder,
# binaries from https://github.com/jelmervdl/bitextor/tree/doctools
# bleualign_cpp from https://github.com/jelmervdl/bleualign-cpp/tree/processed-text-2
# and a self-contained jar with classbase.jar.
export PATH=$ROOT/bin:$PATH

# Note subword nmt is not installed in bin, you'll have to
# pip3 install it yourself at the moment
export BPE_BIN="subword-nmt apply-bpe"
export MARIAN_BIN=marian-decoder

export SLANG=$1
shift

export GPU=$1
shift

case $SLANG in
	de)
		MODEL="$ROOT/DC01311513/nmt/v1/translate.sh"
		;;
	fr)
		MODEL="$ROOT/DC01311514/nmt/v1/translate.sh"
		;;
	dummy)
		SLANG=de
		MODEL="$ROOT/debug-model/translate.sh"
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
	# Remove trailing newline from input (to not cause an empty
	# document at the end), suffix each line with a null byte,
	# which will indicate where a document starts. Then inside each
	# document replace the paragraphs and br tags with newslines.
	# docenc will then group all of those into base64 encoded chunks.
	awk 'NR > 1 { print prev } { prev=$0 } END { ORS=""; print }' \
	| sed -r 's/$/\x0/g' \
	| sed -r 's/<br\/>|<\/p><p>/\n/g' \
	| sed -r 's/<\/?p>//g' \
	| docenc -0
}

preprocess () {
	b64filter java -jar $ROOT/bin/classbase.jar \
		-C  $ROOT/resource/classbase.json \
		-I /dev/stdin \
		-L ${1^^} \
		-S
}

translate () {
	b64filter foldfilter -w 1000 -- $MODEL --quiet-translation
}

for file in $*; do
	paste \
		<(cat $file | col 1) \
		<(cat $file | col 3) \
		<(cat $file | col 2 | document_to_base64) \
		<(cat $file | col 4 | document_to_base64) \
		<(cat $file | col 2 | document_to_base64 | preprocess $SLANG | translate) \
		<(cat $file | col 4 | document_to_base64 | preprocess en) \
	| tee >(gzip -9c > $(basename $file .tab)-bleualign-input.tab.gz) \
	| parallel \
		--halt 2 \
		--pipe \
		-k \
		-l 1 \
		-j $THREADS \
		bleualign_cpp --bleu-threshold 0.2 \
	| gzip \
	> $(basename $file .tab)-aligned.gz
done
