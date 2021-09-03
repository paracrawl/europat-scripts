#!/usr/bin/python3

import argparse
import datetime
import os
import sys

from collections import OrderedDict
from pathlib import Path

EARLIEST_YEAR = 1800
DEFAULT_START_YEAR = 1980

EN = 'en'

# patent parts in order
FILE_TYPES = OrderedDict()
FILE_TYPES.update({'Title' : 'title'})
FILE_TYPES.update({'Abstract' : 'abstract'})
FILE_TYPES.update({'Description' : 'desc'})
FILE_TYPES.update({'Claim' : 'claim'})

def generate_pairs(args):
    for year in range(args.start, args.end+1):
        if args.verbose:
            print('Processing {}'.format(year))
        generate_pairs_by_year(args, year)
        if args.verbose:
            print('{} done'.format(year))

def generate_pairs_by_year(args, year):
    session = '{}-{}'.format(args.country, year)
    yeardir = '{}/{}'.format(args.datadir, session)
    infofile = '{}/{}-info.txt'.format(yeardir, session)
    if not os.path.isdir(yeardir) or not os.path.isfile(infofile):
        return
    # match the downloaded text entries for each file type
    for t, f in FILE_TYPES.items():
        if args.verbose:
            print('  finding {} files'.format(t))
        input_file_pattern = '{}-*-{}-{}.tab'.format(args.country, year, f)
        output_file_suffix = 'EN-{}-{}.tab'.format(year, t)
        generate_pairs_by_type(args, yeardir, input_file_pattern, output_file_suffix)

def generate_pairs_by_type(args, yeardir, input_file_pattern, output_file_suffix):
    matches = []
    english_filename = None
    for filename in map(lambda x: x.name, Path(yeardir).glob(input_file_pattern)):
        lang = filename.split('-')[1]
        input_filename = '{}/{}'.format(yeardir, filename)
        if lang == EN:
            english_filename = input_filename
            continue
        output_filename = '{}/{}-{}'.format(args.outdir, lang, output_file_suffix)
        if os.path.isfile(output_filename):
            message = 'File exists - skipping: {}'.format(output_filename)
            print(message, file=sys.stderr)
            continue
        matches.append((lang, input_filename, output_filename))
    if english_filename and matches:
        # load the English texts only once
        english = load_texts(english_filename)
        generate_pairs_by_language(args, matches, english)

def generate_pairs_by_language(args, matches, english):
    for lang, input_filename, output_filename in matches:
        if args.verbose:
            print('    pairing {} and EN into {}'.format(lang, output_filename))
        with open(output_filename, 'w') as f:
            for docid, content in read_texts(input_filename):
                if docid in english:
                    en_content = english[docid]
                    f.write('{}\t{}\t{}\t{}\n'.format(docid, content, docid, en_content))

def read_texts(filename):
    if os.path.isfile(filename):
        with open(filename) as file:
            for line in file:
                parts = line.strip().split('\t')
                docid = parts[0]
                content = parts[-1]
                yield (docid.replace('-', '.'), content)

def load_texts(filename):
    docids = {}
    for docid, content in read_texts(filename):
        docids[docid] = content
    return docids

def check_country(value):
    if len(value) == 2 and value.isalpha():
        return value.upper()
    raise argparse.ArgumentTypeError('must be a 2-letter country code')

def check_year(value):
    ivalue = int(value)
    if ivalue < EARLIEST_YEAR or ivalue > datetime.datetime.now().year:
        raise argparse.ArgumentTypeError('%s is out of range' % value)
    return ivalue

def check_dir_path(value):
    message = 'not a directory'
    if os.path.isdir(value):
        return value
    else:
        raise argparse.ArgumentTypeError(message)

def main():
    startyear = DEFAULT_START_YEAR
    endyear = datetime.datetime.now().year-1
    datadir = os.environ.get('PATENT_DATA_DIR', '/data/patents/pdfpatents/')
    message = 'Generate patent pair data for the given country code. A single year, or both a start and end year (inclusive) can be given. If no years are specified, the range {}-{} will be used.'.format(startyear, endyear)

    parser = argparse.ArgumentParser(description=message)
    parser.add_argument('outdir', metavar='output-dir', help='Output directory', type=check_dir_path)
    parser.add_argument('country', metavar='country-code', help='Country code', type=check_country)
    parser.add_argument('start', metavar='start-year', nargs='?', help='First year of patents to process', type=check_year)
    parser.add_argument('end', metavar='end-year', nargs='?', help='Last year of patents to process', type=check_year)
    parser.add_argument('--datadir', help='Directory with data files to process', default=datadir)
    parser.add_argument('-v', '--verbose', help='Verbose output', action='store_true')
    args = parser.parse_args()

    # dynamic defaults for missing positional args
    args.end = args.end or args.start or endyear
    args.start = args.start or startyear
    generate_pairs(args)


if __name__ == '__main__':
    # execute only if run as a script
    main()
