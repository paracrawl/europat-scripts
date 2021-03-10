import os
import sys
i=1
for line in sys.stdin:
    if len(line.strip().split('\t')) < 5:
        print(i, ": ", line)
    i +=1

