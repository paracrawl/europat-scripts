#!/bin/bash

FILE_IN=${1}
DOCENC=~/sw/europat-scripts/bin/docenc
preprocess() {
	if [ ! -z "${LOWERCASE:-}" ]; then
		echo "Preprocessing with lowercase" >&2
		lowercase
	else
		cat
	fi
}
simplify(){
	if [ -z "${REMOVE_PUNCTUATION:-}" ]; then
		echo "Removing punctuation" >&2
		remove_punctuation
	else
		cat
	fi
}
lowercase() {
	sed -e 's/./\L\0/g'
}
remove_punctuation(){
	# use sed to avoid removing \n and/or <br></br>
	sed 's/[].!?,|~'\''"()[&£$@%^*]//g'
	#tr -d '[:punct:]'
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
	| $DOCENC -0
}
#zcat $FILE_IN | cut -f3 | base64 -d | lowercase | simplify #| base64 
paste <(zcat $FILE_IN | cut -f1) \
	<(zcat $FILE_IN | cut -f2) \
	<(zcat $FILE_IN | cut -f3) \
	<(zcat $FILE_IN | cut -f4) \
	<(zcat $FILE_IN | cut -f5 | $DOCENC -d | preprocess | simplify | $DOCENC) \
       	<(zcat $FILE_IN | cut -f6 | $DOCENC -d | preprocess | simplify | $DOCENC) \
	| gzip -9c \
	> $(basename $FILE_IN .tab).tmp \
	&& mv $(basename $FILE_IN .tab){.tmp,}
