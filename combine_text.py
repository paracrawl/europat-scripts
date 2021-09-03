#!/usr/bin/python3

import argparse
import csv
import datetime
import os
import pandas as pd

from collections import defaultdict, OrderedDict
from pathlib import Path

EARLIEST_YEAR = 1800
DEFAULT_START_YEAR = 1980

# patent parts in order
FILE_TYPES = OrderedDict()
FILE_TYPES.update({'Title' : 'title'})
FILE_TYPES.update({'Abstract' : 'abstract'})
FILE_TYPES.update({'Description' : 'desc'})
FILE_TYPES.update({'Claims' : 'claim'})

def combine_text(args):
    for year in range(args.start, args.end+1):
        if args.verbose:
            print('Processing {}'.format(year))
        combine_text_by_year(args, year)
        if args.verbose:
            print('{} done'.format(year))

def combine_text_by_year(args, year):
    session = '{}-{}'.format(args.country, year)
    yeardir = '{}/{}'.format(args.datadir, session)
    infofile = '{}/{}-info.txt'.format(yeardir, session)
    if not os.path.isdir(yeardir) or not os.path.isfile(infofile):
        return
    # find the downloaded text entries
    langs = defaultdict(dict)
    file_pattern = '{}-*-{}-*.tab'.format(args.country, year)
    for p in Path(yeardir).glob(file_pattern):
        _, lang, _, filetype = p.stem.split('-')
        langs[lang][filetype] = str(p)
    # combine the downloaded text entries for each language
    for lang in langs:
        if args.verbose:
            print('Language: {}'.format(lang))
        filenames = []
        for t, f in FILE_TYPES.items():
            filename = langs[lang].get(f)
            if filename is not None:
                filenames.append(filename)
            if args.verbose:
                print('  {}:\t{}'.format(t, filename))
        df = combine_text_by_language(filenames)
        # save the combined data
        if df is not None:
            output_filename = '{}/{}-{}-{}-text.tab'.format(yeardir, args.country, lang, year)
            with open(output_filename, 'w') as f:
                df.to_csv(f, sep='\t', quoting=csv.QUOTE_NONE, header=False, index=False)
            if args.verbose:
                print('  Saved {}'.format(output_filename))

def combine_text_by_language(input_filenames):
    CONTENT_FIELD = 'content'
    CONTENT_FIELD_X = CONTENT_FIELD + '_x'
    CONTENT_FIELD_Y = CONTENT_FIELD + '_y'
    df = None
    for input_filename in input_filenames:
        df2 = pd.read_csv(input_filename, sep='\t', quoting=csv.QUOTE_NONE, names=['ID', 'date', CONTENT_FIELD])
        if df is None:
            df = df2
        else:
            df = df.merge(df2, how='outer', sort=True, on=['ID', 'date'], validate='1:1')
            df[CONTENT_FIELD] = df[[CONTENT_FIELD_X, CONTENT_FIELD_Y]].apply(join_text, axis=1)
            df = df.drop(columns=[CONTENT_FIELD_X, CONTENT_FIELD_Y])
    return df

def join_text(texts):
    # skip missing values
    return '<br>'.join((t for t in texts if pd.notna(t)))

def check_country(value):
    if len(value) == 2 and value.isalpha():
        return value.upper()
    raise argparse.ArgumentTypeError('must be a 2-letter country code')

def check_year(value):
    ivalue = int(value)
    if ivalue < EARLIEST_YEAR or ivalue > datetime.datetime.now().year:
        raise argparse.ArgumentTypeError('%s is out of range' % value)
    return ivalue

def main():
    startyear = DEFAULT_START_YEAR
    endyear = datetime.datetime.now().year-1
    datadir = os.environ.get('PATENT_DATA_DIR', '/data/patents/pdfpatents/')
    message = 'Combine text parts for all patents with the given country code. A single year, or both a start and end year (inclusive) can be given. If no years are specified, the range {}-{} will be used.'.format(startyear, endyear)

    parser = argparse.ArgumentParser(description=message)
    parser.add_argument('country', metavar='country-code', help='Country code', type=check_country)
    parser.add_argument('start', metavar='start-year', nargs='?', help='First year of patents to process', type=check_year)
    parser.add_argument('end', metavar='end-year', nargs='?', help='Last year of patents to process', type=check_year)
    parser.add_argument('--datadir', help='Directory with data files to process', default=datadir)
    parser.add_argument('-v', '--verbose', help='Verbose output', action='store_true')
    args = parser.parse_args()

    # dynamic defaults for missing positional args
    args.end = args.end or args.start or endyear
    args.start = args.start or startyear
    combine_text(args)


if __name__ == '__main__':
    # execute only if run as a script
    main()
