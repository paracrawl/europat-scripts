#!/bin/bash

file=${1:-ES-EN-1997-Description-bleualign-input.tab.gz}
echo Processing file: $(basename $file -bleualign-input-tab.gz).tab
ls  $(basename $file .tab.gz)-?-aligned.* >  $(basename $file -bleualign-input.tab.gz)-alignment-errors.txt
cat $(basename $file .tab.gz)-?-aligned | gzip -9c > $(basename $file -bleualign-input.tab.gz)-aligned.gz
rm  $(basename $file .tab.gz)-?-aligned  $(basename $file .tab.gz)-?.tab $(basename $file .tab.gz)-?-aligned.* $(basename $file .tab.gz).tab
