#!/bin/bash
TEXT=$1
#cat $TEXT | sed -e 's/[][.!?,'\''/\\"()^*]\+//g' | cat
cat $TEXT | sed 's/[].!?,|~'\''"()[&£$@%^*]//g' | cat
#cat $TEXT | sed 's/[].!?,'\''/\\"()[&$@%^*]//g' | cat
