#!/usr/bin/env python3
import gzip
import sys

CHAR_TAB = ord('\t')

fhs = [gzip.open(name, 'w') for name in sys.argv[1:]]

for line in sys.stdin.buffer:
    pos = 0
    while line[pos] == CHAR_TAB:
        pos += 1
    
    if pos >= len(fhs):
        print(f"No {pos}th file", file=sys.stderr)
        continue
    fhs[pos].write(line)

for fh in fhs:
    fh.close()

