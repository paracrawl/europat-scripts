#!/bin/bash
pigz -cd $1 |
split - ${1%.gz}. \
	-a 3 \
	-d \
	-l 1000000 \
	--filter='pigz -c > $FILE'
