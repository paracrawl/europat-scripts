#!/usr/bin/env python3
import os
import sys
from glob import glob
from collections import defaultdict

def find_ranges(numbers):
	it = iter(sorted(numbers))
	ranges = []
	
	try:
		while True:
			current = next(it)
			if ranges and ranges[-1][1] == current - 1:
				ranges[-1] = (ranges[-1][0], current)
			else:
				ranges.append((current, current))
	except StopIteration:
		return ranges

def format_ranges(ranges):
	return ','.join(
		f'{start}' if start == end else f'{start}-{end}'
		for start, end in ranges
	)

text = defaultdict(set)

families = defaultdict(set)

for f in glob('text/??-????*.tab'):
	f = os.path.basename(f)
	lang, year = f[0:2], int(f[3:7])
	text[lang].add(year)

for f in glob('families/??-EN-[0-9][0-9][0-9][0-9]-*.tab'):
	f = os.path.basename(f)
	lang, year = f[0:2], int(f[6:10])
	families[lang].add(year)

for lang, years in text.items():
	ranges = find_ranges(years & families[lang])
	if len(sys.argv) > 1 and sys.argv[1] == 'sbatch':
		print(f'sbatch -J align-{lang.lower()} --array={format_ranges(ranges)} ~/src/europat-scripts/align.slurm {lang.lower()}')
	else:
		print(f"{lang}\t{format_ranges(ranges)}")
