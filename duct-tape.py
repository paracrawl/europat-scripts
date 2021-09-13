#!/usr/bin/env python3
import os
import re
import sys
import re
from base64 import b64decode, b64encode

REMOVE_HEADER = False

patterns = [
	r'Pr?ix dur? fas[éc]i[oc]u[tl]e(\s*:)+[\s\dG°\|]+ francs?[\.,\s]*[s:]?',
	r'([\'*]\s*)?[\dLbgo£\.]{2,3}',
	r'\[[\d\./]{3,}\]',
	r'\d+ addition n[o°] [\d\.\,]+',
	r'NO/EP\d{2,}',
]

replacements = [
	(r'Ã¥', 'å'), # NO
	(r'Å¡', 'š'), # HR
	(r'[1t]\.[1t]\. (\d+)', r't.t. \1'), # HR t.t. is ocr'ed as 1.1.
	(r'Â¡um|Â¡Â¡m|µ', 'μ'), # Weird micro in Spanish
	(r'\bef al*\b', 'et al'), # Spanish OCR does not expect el al references?
	#(r'(\d+\'?(,\d+\'?)?[a-z]?(-\(?R\))?(-[A-Za-z0-9]+)*-[A-Za-z]+)', r'<<FORMULA[\1]>>'), # needs regex
]

header_patterns = [
	r'^F \d+ F \d+/\d+$',
	r'^\(?\d\d\) ',
	r'Patentstyret',
	r'NORGE|NORWAY',
	r'^Norwegian Industrial Property Office$',
	r', (FR|NO|DE|JP|JP-Japan).?$',
	r'^States: ([A-Z]{2}\s*;\s*)*([A-Z]{2});?$',
	r'^\d{4}.\d{2}.\d{2}, EP, \d+$',
	r'^[\d\s\.]+$',
	r'([Ss]trasse|Postboks|Norway)',
]

def is_trash(line):
	return any(re.fullmatch(pattern, line) for pattern in patterns)

def is_header(line):
	return any(re.match(pattern, line) for pattern in header_patterns)


def process_document(lines):
	# Remove header
	if REMOVE_HEADER:
		i = 0
		skipped = 0
		while i + skipped < len(lines):
			if is_header(lines[i + skipped]):
				i += skipped + 1
				skipped = 0
			else: #if is_header(lines[i + skipped]):
				skipped += 1

			if skipped > 3:
				break

		lines = lines[i:]

	i = 0
	while i < len(lines):
		n = i
		while n < len(lines) and is_trash(lines[n]):
			n += 1	
		if n - i > 0: # > 1, but there's also a lot of single line trash
			lines = lines[:i] + lines[n:]

		if i < len(lines):
			for pattern, replacement in replacements:
				lines[i] = re.sub(pattern, replacement, lines[i])

		i += 1

	return lines


def read_documents(fh):
	for line in fh:
		yield b64decode(line.rstrip()).decode().split('\n')


def write_document(fh, lines):
	fh.write(b64encode('\n'.join(lines).encode()) + b'\n')


def main():
	if len(sys.argv) > 1 and sys.argv[1] == '--base64':
		for lines in read_documents(sys.stdin.buffer):
			lines = process_document(lines)
			write_document(sys.stdout.buffer, lines)
	else:
		lines = [line.rstrip() for line in sys.stdin]
		lines = process_document(lines)
		print('\n'.join(lines))

main()
