#!/usr/bin/env python3
import sys
import re
from collections import Counter
from pprint import pprint


TOKEN_PATTERN = re.compile(r'(([\{-\~\[-\` -\&\(-\+\:-\@\/])|(?:(?<![0-9])([\.,]))|(?:([\.,])(?![0-9]))|(?:(?<=[0-9])(-)))')


def tokenize(sentence):
	pos = 0
	for match in re.finditer(TOKEN_PATTERN, sentence):
		if match.start() > pos:
			yield sentence[pos:match.start()]

		if match.group() != ' ':
			yield match.group()

		pos = match.end()

	if pos < len(sentence):
		yield sentence[pos:]


def token_count(sentence):
	return sum(1 for _ in tokenize(sentence))


def ngrams(tokeniter, size):
	ngram = tuple([None])
	try:
		while len(ngram) < size:
			ngram = (*ngram, next(tokeniter))
	except StopIteration:
		return
	while True:
		token = next(tokeniter, None)
		if token is None:
			break
		ngram = (*ngram[1:], token)
		yield ngram


def count_ngrams(tokeniter, size):
	maps = [Counter() for _ in range(size)]
	for ngram in ngrams(tokeniter, size):
		for ngram_size in range(1, size + 1):
			maps[ngram_size - 1][ngram[:ngram_size]] += 1
	return maps


def metric_bleu(sent_sl, sent_tl):
	pprint([
		sent_sl,
		list(count_ngrams(tokenize(sent_sl), 3))
	])

if len(sys.argv) == 2:
	cols = (0, 1)	
elif len(sys.argv) == 4:
	cols = (int(sys.argv[2]) - 1, int(sys.argv[3]) - 1)
else:
	print(f"usage: {sys.argv[0]} [ COL1 COL2 ]", file=sys.stderr)
	sys.exit(1)

for line in sys.stdin:
	fields = line.rstrip("\n").split("\t")
	metric_bleu(fields[cols[0]], fields[cols[1]])
