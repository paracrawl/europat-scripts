#!/usr/bin/env pypy
import sys
import gzip
from collections import defaultdict
from pprint import pprint
from concurrent.futures import ThreadPoolExecutor


def magic_open(filename):
	if filename.endswith('.gz'):
		return gzip.open(filename, 'rt')
	else:
		return open(filename, 'r')

def read_file_index(index_path):
	file_index = dict()

	with magic_open(index_path) as fh:
		n = 0
		current = None
		
		for line in fh:
			if line.startswith('/'):
				n = 0
				current = line.strip()
				continue
			
			file_index[line.strip()] = (current, n)
			n += 1
	return file_index

def read_mapping(fh):
	return [
		dict(zip(
			['foreign_id', 'foreign_family', 'en_id', 'en_family'],
			line.strip().split('\t')))
		for line in fh
	]

def read_foreign_text(tab_path, mappings, dest, column=3):
	foreign_index = defaultdict(list)
	for mapping in mappings:
		foreign_index[mapping['foreign_id']].append(mapping)

	print(f"Reading {len(foreign_index)} entries from {tab_path}", file=sys.stderr)
	with magic_open(tab_path) as fh:
		for line in fh:
			parts = line.rstrip('\n').split('\t')
			for mapping in (foreign_index.get(parts[0], []) or foreign_index.get(patch_id(parts[0]), [])):
				mapping[dest] = parts[column]

# Read English text
def patch_id(en_id):
	return f'{en_id[0:3]}0{en_id[3:]}'

def read_english_text(file_index, mappings):
	for mapping in mappings:
		mapping['en_file'] = file_index.get(mapping['en_id'], None) or file_index.get(patch_id(mapping['en_id'])) or ('', 0)

	fh = None
	n = -1

	for mapping in sorted(mappings, key=lambda mapping: mapping['en_file']):
		if not mapping['en_file'][0]:
			continue

		if not fh or fh.name != mapping['en_file'][0]:
			if fh:
				fh.close()
			print(f"Opening {mapping['en_file'][0]}", file=sys.stderr)
			fh = magic_open(mapping['en_file'][0])
			n = -1

		print(f"Scanning for line {mapping['en_file'][1]} (currently at {n})", file=sys.stderr)
		while n < mapping['en_file'][1]:
			line = next(fh) # fh.readline()
			n += 1

		assert n == mapping['en_file'][1]
		
		try:
			line_id, _, line_text = line.rstrip('\n').split('\t', maxsplit=3)
		except:
			raise Exception(f'Could not unpack line {n} of {fh.name} `{line.rstrip()}`')

		if not line_id == mapping['en_id'] and not line_id == patch_id(mapping['en_id']):
			print(f"Line {n} has {line_id}, not matching {mapping['en_id']}", file=sys.stderr)
			sys.exit(1)

		mapping['en_text'] = line_text


def read_english_text_worker(en_file, mappings):
	n = -1
	with magic_open(en_file) as fh:
		for mapping in mappings:
			print(f"Scanning for {mapping['en_file']} (currently at {n})", file=sys.stderr)
			while n < mapping['en_file'][1]:
				line = next(fh) # fh.readline()
				n += 1

			assert n == mapping['en_file'][1]

			try:
				line_id, _, line_text = line.rstrip('\n').split('\t', maxsplit=3)
			except:
				raise Exception(f'Could not unpack line {n} of {fh.name} `{line.rstrip()}`')

			if not line_id == mapping['en_id'] and not line_id == patch_id(mapping['en_id']):
				raise Exception(f"Line {n} has {line_id}, not matching {mapping['en_id']}")

			mapping['en_text'] = line_text


def read_english_text_parallel(file_index, mappings):
	mappings_by_file = defaultdict(list)

	for mapping in mappings:
		mapping['en_file'] = file_index.get(mapping['en_id']) or file_index.get(patch_id(mapping['en_id'])) or ('', 0)

		if mapping['en_file'][0]:
			mappings_by_file[mapping['en_file'][0]].append(mapping)

	workers = []

	with ThreadPoolExecutor(max_workers=16) as executor:
		for en_file, mappings in mappings_by_file.items():
			mappings.sort(key=lambda mapping: mapping['en_file'])
			executor.submit(read_english_text_worker, en_file, mappings)


def filter_empty(mappings, columns):
	filtered = [
		mapping 
		for mapping in mappings
		if not any(mapping.get(col, '') == '' for col in columns)
	]
	print(f"{len(filtered)} of {len(mappings)} records remain after filtering", file=sys.stderr)
	return filtered


def main():
	mappings = read_mapping(sys.stdin)

	# Columns coming from read_mapping()
	columns = ['foreign_id', 'foreign_family', 'en_id', 'en_family']
	
	file_index = read_file_index(sys.argv[1])

	with ThreadPoolExecutor(max_workers=16) as executor:
		for filename in sys.argv[2:]:
			if ':' in filename:
				filename, column = filename.split(':', maxsplit=1)
			else:
				column = '3' # default for Europat data for some reason

			columns.append(filename)
			executor.submit(read_foreign_text, filename, mappings, column=int(column) - 1, dest=filename)

	# Early removal of empty mappings to save them from going through English
	# lookup which is generally pretty slow because of the amount of data
	mappings = filter_empty(mappings, columns)

	# Moved this to after the local file handling because it's often a lot slower
	# and now we can first do some filtering.
	read_english_text_parallel(file_index, mappings)
	
	# Column coming from read_english_text_parallel
	columns.append('en_text')

	mappings = filter_empty(mappings, columns)

	# original order
	for mapping in mappings:
		print("\t".join([
			mapping[col]
			for col in columns
		]))

if __name__ == '__main__':
	main()
