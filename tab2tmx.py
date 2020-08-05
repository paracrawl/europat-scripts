#!/usr/bin/env python3
import sys
import time
from contextlib import contextmanager
from xml.sax.saxutils import escape, quoteattr
from itertools import combinations


__VERSION__ = 0.1

class XMLWriter(object):
	def __init__(self, fh):
		self.fh = fh
		self.stack = []
		self.indent = '  '

	def _write_formatted(self, line):
		self.fh.write(self.indent * len(self.stack) + line)

	def open(self, name: str, attributes: dict = dict()):
		attr_str = ''.join(f' {attr_name}={quoteattr(str(attr_value))}' for attr_name, attr_value in attributes.items())
		self._write_formatted(f'<{name}{attr_str}>\n')
		self.stack.append(name)

	def close(self):
		name = self.stack.pop()
		self._write_formatted(f'</{name}>\n')

	def write(self, text):
		self._write_formatted(escape(str(text).rstrip() + '\n'))

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
#   'domains': {str},		
# 	'translations': {
# 		str: {
# 			'urls': {str},
# 			'text': str
# 		}
# 	}
# }


class TMXWriter(object):
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

	def write(self, unit):
		with self.writer.element('tu', {'tuid': unit['id'], 'datatype': 'Text'}) as tu:
			if 'score' in unit:
				with tu.element('prop', {'type': 'score-aligner'}) as prop:
					prop.write(unit['score'])

			if 'domains' in unit:
				for domain in unit['domains']:
					with tu.element('prop', {'type': 'domain'}) as prop:
						prop.write(domain)

			for lang, translation in unit['translations'].items():
				with tu.element('tuv', {'xml:lang': lang}) as tuv:
					if 'urls' in translation:
						for url in translation['urls']:
							with tuv.element('prop', {'type': 'source-document'}) as prop:
								prop.write(url)

					with tuv.element('seg') as seg:
						seg.write(translation['text'])


class TabReader(object):
	def __init__(self, fh, src_lang, trg_lang):
		self.fh = fh
		self.src_lang = src_lang
		self.trg_lang = trg_lang

	def __enter__(self):
		return self

	def __exit__(self, *args):
		pass

	def __iter__(self):
		return self.records()

	def records(self):
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


class DomainLabeler(object):
	def __init__(self):
		self.lut = dict()

	def load(self, fh):
		for line in fh:
			src_id, src_date, _, _, src_lang, domain_codes = line.split('\t', 5)
			self.lut[(src_lang, src_id)] = set(domain_codes.split(', '))

	def annotate(self, unit):
		keys = {
			(lang, url)
			for lang, translation in unit['translations'].items()
			for url in translation['urls']
		}

		return {
			**unit,
			'domains': set.union(*(self.lut[key] for key in keys & self.lut.keys()))
		}


def text_key(unit):
	return tuple(translation['text'] for translation in unit['translations'].values())


def deduplicate(reader, key):
	prev=None
	for unit in reader:
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


if __name__ == '__main__':
	labeler = DomainLabeler()

	with open(sys.argv[3], 'r') as fh:
		labeler.load(fh)

	# print(labeler.lut.keys())

	with TabReader(sys.stdin, sys.argv[1], sys.argv[2]) as reader:
		with TMXWriter(sys.stdout) as writer:
			for unit in deduplicate(sorted(reader, key=text_key), key=text_key):
				writer.write(labeler.annotate(unit))
