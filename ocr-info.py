#!/usr/bin/env pypy
import sys
import site
site.addsitedir('src/tmxutil')
from contextlib import closing
from tmxutil import TMXReader, TMXWriter

with open(sys.argv[1], 'r') as fh:
    hashes = frozenset(line.strip() for line in fh if line.strip() != '')

with closing(TMXReader(sys.stdin)) as fin, TMXWriter(sys.stdout) as fout:
    for n, record in enumerate(fin):
        record['x-medium'] = {'text' if next(iter(record.get('bifixer-hash')), '').strip() in hashes else 'ocr'}
        fout.write(record)

        if n % 100_000 == 0:
            print(n, file=sys.stderr)