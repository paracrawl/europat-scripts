#!/usr/bin/env python3
#
# Tool to convert between tab, txt and tmx formatting, filtering & adding
# annotations.
#
# Example: convert tab to tmx, add ipc codes and ipc groups
#
#   gzip -cd DE-EN-2001-Abstract-aligned.gz \
#	| ./tab2tmx.py -o tmx -l de en -d \
#		--ipc DE-EN-2001-Metadata.tab \
#		--ipc-group ipc-groups.tab \
# 	| gzip -9c > DE-EN-2001-Abstract.tmx.gz
#
#
# Example: filter tmx by IPC group
#
# 	gzip -cd DE-EN-2001-Abstract.tmx.gz \
#	| ./tab2tmx.py -o tmx \
#		--filter-ipc-group IV \
#	| gzip -9c > DE-EN-2001-Abstract-IV-only.tmx.gz
#
#
# Example: retrieve only DE text from tmx
# 
# 	gzip -cd DE-EN-2001-Abstract.tmx.gz \
#	| ./tab2tmx.py -o txt --output-languages de \
#	| gzip -9c > de.txt.gz
#
#
# You can combine annotating and filtering (allowing you to filter .tab files)
# if you have the metadata file as well.
#

__VERSION__ = 1.0

import csv
import sys
import time
from abc import ABC, abstractmethod
from contextlib import contextmanager
from io import BufferedReader
from xml.sax.saxutils import escape, quoteattr
from xml.etree.ElementTree import iterparse
from itertools import combinations
from pprint import pprint
from typing import Callable, List, Optional, Any, Iterator, Set

class XMLWriter(object):
	def __init__(self, fh):
		self.fh = fh
		self.stack = []
		self.indent = '  '

	def open(self, name: str, attributes: dict = dict()):
		if self.stack:
			self.stack[-1] = (self.stack[-1][0], True)

		attr_str = ''.join(' {}={}'.format(attr_name, quoteattr(str(attr_value))) for attr_name, attr_value in attributes.items())
		self.fh.write('\n{}<{}{}>'.format(self.indent * len(self.stack), name, attr_str))

		self.stack.append((name, False))

	def close(self):
		name, has_children = self.stack.pop()
		if has_children:
			self.fh.write('\n{}</{}>'.format(self.indent * len(self.stack), name))
		else:
			self.fh.write('</{}>'.format(name))

	def write(self, text: Any):
		self.fh.write(escape(str(text).rstrip()))

	@contextmanager
	def element(self, name: str, attributes: dict = dict()):
		self.open(name, attributes)
		yield self
		self.close()

	def __enter__(self) -> 'XMLWriter':
		self.fh.write('<?xml version=\"1.0\"?>')
		return self

	def __exit__(self, type, value, traceback):
		if type is None:
			while len(self.stack):
				self.close()

# unit = {
# 	'score': float,
# 	'translations': {
# 		str: {
# 			'urls': {str},
#   		'ipcs': {str},
#			'ics_groups': {str},
# 			'text': str
# 		}
# 	}
# }

class Reader(ABC):
	def __iter__(self) -> Iterator[dict]:
		return self.records()

	@abstractmethod
	def records(self) -> Iterator[dict]:
		pass


class Writer(ABC):
	def __enter__(self):
		return self

	def __exit__(self, *args):
		pass

	@abstractmethod
	def write(self, unit: dict):
		pass


class TMXReader(Reader):
	def __init__(self, fh):
		self.fh = fh

	def records(self) -> Iterator[dict]:
		path = []
		stack = []
		
		def enter(element):
			stack.append(element)
			path.append(element.tag)

		def exit(element):
			removed = stack.pop()
			assert removed == element
			path.pop()
			if stack:
				# Remove element from parent to keep the internal tree empty
				stack[-1].remove(removed)

		unit: dict = None
		translation: dict = None

		lang_key = '{http://www.w3.org/XML/1998/namespace}lang'
		
		for event, element in iterparse(self.fh, events={'start', 'end'}):
			if event == 'start':
				enter(element)

				if path == ['tmx', 'body', 'tu']:
					unit = {
						'id': element.get('tuid'),
						'translations': {}
					}
				elif path == ['tmx', 'body', 'tu', 'tuv']:
					translation = {
						'urls': set(),
						'ipcs': set(),
						'ipc_groups': set(),
						'text': None
					}
			elif event == 'end':
				if path == ['tmx', 'body', 'tu']:
					yield unit
				elif path == ['tmx', 'body', 'tu', 'prop']:
					if element.get('type') == 'score-aligner':
						unit['score'] = float(element.text.strip())
				elif path == ['tmx', 'body', 'tu', 'tuv']:
					unit['translations'][element.attrib[lang_key]] = translation
					translations = None
				elif path == ['tmx', 'body', 'tu', 'tuv', 'prop']:
					if element.get('type') == 'source-document':
						translation['urls'].add(element.text.strip())
					elif element.get('type') == 'ipc':
						translation['ipcs'].add(element.text.strip())
					elif element.get('type') == 'ipc-group':
						translation['ipc_groups'].add(element.text.strip())
				elif path == ['tmx', 'body', 'tu', 'tuv', 'seg']:
					translation['text'] = element.text.strip()

				exit(element)


class TMXWriter(Writer):
	def __init__(self, fh):
		self.fh = fh
		
	def __enter__(self):
		self.writer = XMLWriter(self.fh)
		self.writer.__enter__()
		self.writer.open('tmx', {'version': 1.4})
		with self.writer.element('header', {
			'o-tmf': 'PlainText',
			'creationtool': 'tab2tmx',
			'creationtoolversion': __VERSION__,
			'datatype': 'PlainText',
			'segtype': 'sentence',
			'creationdate': time.strftime("%Y%m%dT%H%M%S"),
			'o-encoding': 'utf-8'
			}) as header:
			pass
		self.writer.open('body')
		return self

	def __exit__(self, *args):
		self.writer.__exit__(*args)

	def _write_prop(self, name, value):
		if value is None:
			return
		elif isinstance(value, (list, set)):
			for val in value:
				self._write_prop(name, val)
		else:
			with self.writer.element('prop', {'type': name}) as prop:
				prop.write(value)

	def write(self, unit):
		with self.writer.element('tu', {'tuid': unit['id'], 'datatype': 'Text'}):
			self._write_prop('score-aligner', unit.get('score'))
			for lang, translation in unit['translations'].items():
				with self.writer.element('tuv', {'xml:lang': lang}):
					self._write_prop('source-document', translation.get('urls'))
					self._write_prop('ipc', translation.get('ipcs'))
					self._write_prop('ipc-group', translation.get('ipc_groups'))
					with self.writer.element('seg'):
						self.writer.write(translation['text'])



class TabReader(Reader):
	def __init__(self, fh, src_lang, trg_lang):
		self.fh = fh
		self.src_lang = src_lang
		self.trg_lang = trg_lang

	def records(self) -> Iterator[dict]:
		for n, line in enumerate(self.fh):
			src_url, trg_url, src_text, trg_text, score = line.split('\t')
			yield {
				'id': n,
				'score': float(score),
				'translations': {
					self.src_lang: {
						'urls': {src_url},
						'text': src_text
					},
					self.trg_lang: {
						'urls': {trg_url},
						'text': trg_text
					}
				}
			}


class TabWriter(Writer):
	def __init__(self, fh, languages = []):
		self.fh = fh
		self.languages = languages

	def __enter__(self):
		self.writer = csv.writer(self.fh, delimiter='\t')

	def write(self, unit: dict):
		if not self.languages:
			self.languages = list(unit['translations'].keys())

		self.writer.writerow(
			  [next(iter(unit['translations'][lang]['urls'])) for lang in self.languages]
			+ [unit['translations'][lang]['text'] for lang in self.languages])


class TxtWriter(Writer):
	def __init__(self, fh, language: str):
		self.fh = fh
		self.language = language

	def write(self, unit: dict):
		print(unit['translations'][self.language]['text'], file=self.fh)


class IPCLabeler(object):
	def __init__(self, paths: List[str] = []):
		self.lut = dict()
		for path in paths:
			with open(path, 'r') as fh:
				self.load(fh)

	def load(self, fh):
		for line in fh:
			src_id, _, _, _, src_lang, src_ipcs, trg_id, _, _, _, trg_lang, trg_ipcs = line.split('\t', 11)
			self.lut[(src_lang.lower(), src_id)] = set(ipc.strip() for ipc in src_ipcs.split(','))
			self.lut[(trg_lang.lower(), trg_id)] = set(ipc.strip() for ipc in trg_ipcs.split(','))

	def annotate(self, unit: dict) -> dict:
		for lang, translation in unit['translations'].items():
			keys = self.lut.keys() & {(lang.lower(), url) for url in translation['urls']}
			translation['ipcs'] = set().union(*(self.lut[key] for key in keys))
		return unit


class IPCGroupLabeler(object):
	def __init__(self, paths: List[str] = []):
		self.patterns = []
		for path in paths:
			with open(path, 'r') as fh:
				self.load(fh)

	def load(self, fh):
		for line in fh:
			prefix, group, label = line.split('\t', 2)
			self.patterns.append((prefix.strip(), group.strip(), label.strip()))

		# Sort with most specific on top
		self.patterns.sort(key=lambda pattern: (-len(prefix), prefix))

	def find_group(self, ipc_code: str) -> Optional[str]:
		for prefix, group, label in self.patterns:
			if ipc_code.startswith(prefix):
				return group

	def annotate(self, unit: dict) -> dict:
		for lang, translation in unit['translations'].items():
			translation['ipc_groups'] = set(group for group in map(self.find_group, translation['ipcs']) if group is not None)
		return unit


def text_key(unit: dict) -> tuple:
	return tuple(translation['text'] for translation in unit['translations'].values())


def deduplicate(reader: Iterator[dict], key: Callable[[dict], Any]) -> Iterator[dict]:
	prev=None
	for unit in sorted(reader, key=key):
		if prev is None:
			prev = unit
		elif key(prev) == key(unit):
			for lang, translation in unit['translations'].items():
				prev['translations'][lang]['urls'] |= translation['urls']
		else:
			yield prev
			prev = unit

	if prev:
		yield prev


def unit_pred_intersection(key: str, values: Set[str]) -> Callable[[dict], bool]:
	return lambda unit: any(translation[key] & values for translation in unit['translations'].values())


def autodetect(fh) -> Optional[str]:
	xml_signature = b'<?xml '
	if fh.peek(len(xml_signature)).startswith(xml_signature):
		return 'tmx'
	elif b'\t' in fh.peek(64):
		return 'tab'
	else:
		return None


if __name__ == '__main__':
	from argparse import ArgumentParser

	parser = ArgumentParser(description='Annotate, filter and convert tmx files')
	parser.add_argument('-i', '--input-format', choices=['tmx', 'tab'])
	parser.add_argument('-o', '--output-format', choices=['tmx', 'tab', 'txt'], required=True)
	parser.add_argument('-l', '--input-languages', nargs=2)
	parser.add_argument('--output-languages', nargs='+')
	parser.add_argument('-d', '--deduplicate', action='store_true')
	parser.add_argument('--ipc', dest='ipc_meta_files', action='append')
	parser.add_argument('--ipc-group', dest='ipc_group_files', action='append')
	parser.add_argument('--filter-ipc', nargs='+')
	parser.add_argument('--filter-ipc-group', nargs='+')

	args = parser.parse_args()

	fin = sys.stdin
	fout = sys.stdout

	reader: Reader = None
	writer: Writer = None

	# Autodetect input format if we're lazy
	if not args.input_format:
		args.input_format = autodetect(fin.buffer)

	# Parameter validation, early warning system
	if args.input_format == 'tab' and not args.input_languages:
		print("Tab format requires a --input-languages X Y option.", file=sys.stderr)
		sys.exit(1)

	if args.output_languages == 'txt' and (not args.output_languages or len(args.output_languages) != 1):
		print("Use --output-languages X to select which language. When writing txt, it can only write one language at a time.", file=sys.stderr)
		sys.exit(1)

	# Create reader
	if args.input_format == 'tmx':
		reader = TMXReader(fin)
	elif args.input_format == 'tab':
		reader = TabReader(fin, *args.input_languages)
	elif not args.input_format:
		print(f"Use --input-format tab or tmx to specify input format.", file=sys.stderr)
		sys.exit(1)
	else:
		print(f"Unsupported input format: {args.input_format}", file=sys.stderr)
		sys.exit(1)

	# Create writer
	if args.output_format == 'tmx':
		writer = TMXWriter(fout)
	elif args.output_format == 'tab':
		writer = TabWriter(fout, args.output_languages)
	elif args.output_format == 'txt':
		writer = TxtWriter(fout, args.output_languages[0])
	else:
		print(f"Unsupported output format: {args.output_format}", file=sys.stderr)
		sys.exit(1)

	# Optional filter & annotation steps for reader
	if args.ipc_meta_files:
		reader = map(IPCLabeler(args.ipc_meta_files).annotate, reader)

	if args.ipc_group_files:
		reader = map(IPCGroupLabeler(args.ipc_group_files).annotate, reader)

	if args.deduplicate:
		reader = deduplicate(reader, key=text_key)

	if args.filter_ipc:
		reader = filter(unit_pred_intersection('ipcs', set(args.filter_ipc)), reader)
		
	if args.filter_ipc_group:
		reader = filter(unit_pred_intersection('ipc_groups', set(args.filter_ipc_group)), reader)

	# Main loop. with statement for writer so it can write header & footer
	with writer:
		for unit in reader:
			writer.write(unit)