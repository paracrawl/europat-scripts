#!/usr/bin/env python3
import sys
import re

MIN_LENTH = 0.6

MAX_LENGTH = 1.6

MIN_TOKENS = 3

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


def metric_elrc(sent_sl, sent_tl):
	length_ratio = len(sent_sl) * 1.0 / len(sent_tl) if len(sent_tl) > 0 else 0.0
	tokens_sl = token_count(sent_sl)
	tokens_tl = token_count(sent_tl)
	return (length_ratio, tokens_sl, tokens_tl)


def pair_info(sent_sl, sent_tl):
	tags = []

	if re.sub("[^0-9]", "", sent_sl) != re.sub("[^0-9]", "", sent_tl):
		tags.append('different numbers in TUVs')

	if re.sub(r'\W+', '', sent_sl) == re.sub(r'\W+', '', sent_tl):
		tags.append('equal TUVs')

	return '|'.join(tags)


def main(argv):
	if len(argv) == 1:
		cols = (0, 1)	
	elif len(argv) == 3:
		cols = (int(argv[1]) - 1, int(argv[2]) - 1)
	else:
		print(f"usage: {argv[0]} [ COL1 COL2 ]", file=sys.stderr)
		sys.exit(1)

	for line in sys.stdin:
		fields = line.rstrip("\n").split("\t")

		length_ratio, tokens_sl, tokens_tl = metric_elrc(fields[cols[0]], fields[cols[1]])

		print("\t".join([
			f'{length_ratio:.3f}',
			f'{tokens_sl:d}',
			f'{tokens_tl:d}',
			f'very short segments, shorter than {MIN_TOKENS}' if tokens_sl < MIN_TOKENS else '',
			f'very short segments, shorter than {MIN_TOKENS}' if tokens_tl < MIN_TOKENS else '',
			pair_info(fields[cols[0]], fields[cols[1]])
		]))

if __name__ == '__main__':
	main(sys.argv)