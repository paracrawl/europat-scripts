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
FILE_TYPES.update({'Title' : ['title', 'Title']})
FILE_TYPES.update({'Abstract' : ['abstract', 'Abstract']})
FILE_TYPES.update({'Description' : ['desc', 'Description']})
FILE_TYPES.update({'Claims' : ['claim', 'Claim']})

ID_FIELD = 'ID'
DATE_FIELD = 'date'
CONTENT_FIELD = 'content'
CONTENT_FIELD_X = CONTENT_FIELD + '_x'
CONTENT_FIELD_Y = CONTENT_FIELD + '_y'

def combine_text(args):
    for year in range(args.start, args.end+1):
        if args.verbose:
            print('Processing {}'.format(year))
        combine_text_by_year(args, year)
        if args.verbose:
            print('{} done'.format(year))

def combine_text_by_year(args, year):
    # find the downloaded text entries
    langs = get_files_by_language(args, year)
    # combine the downloaded text entries for each language
    for lang in langs:
        if args.verbose:
            print('Language: {}'.format(lang))
        filenames = []
        for label in FILE_TYPES:
            filename = langs[lang].get(label)
            if filename is not None:
                filenames.append(filename)
            if args.verbose:
                print('  {}:\t{}'.format(label, filename))
        df = combine_text_by_language(filenames)
        # save the combined data
        if df is not None:
            output_dir = get_output_dir(args, year)
            os.makedirs(output_dir, exist_ok=True)
            output_filename = get_output_filename(args, lang, year)
            write_data(df, '{}/{}'.format(output_dir, output_filename))
            if args.verbose:
                print('  Saved {}'.format(output_filename))

def combine_text_by_language(input_filenames):
    df = None
    for input_filename in input_filenames:
        df2 = fix_format_errors(read_data(input_filename))
        if df is None:
            df = df2
        else:
            df = df.merge(df2, how='outer', sort=True, on=[ID_FIELD, DATE_FIELD], validate='1:1')
            df[CONTENT_FIELD] = df[[CONTENT_FIELD_X, CONTENT_FIELD_Y]].apply(join_text, axis=1)
            df = df.drop(columns=[CONTENT_FIELD_X, CONTENT_FIELD_Y])
    return df

def read_file(filename):
    if os.path.isfile(filename):
        with open(filename) as file:
            for line in file:
                yield line.strip().split('\t', 2)

def fix_format_errors(df):
    # fix some common file format errors
    TMP_FIELD = 'tmp'
    df[TMP_FIELD] = df[DATE_FIELD].str[8:]
    df[DATE_FIELD] = df[DATE_FIELD].str[:8]
    df[CONTENT_FIELD] = df[[TMP_FIELD, CONTENT_FIELD]].apply(join_text, axis=1)
    df = df.drop(columns=[TMP_FIELD])
    df[CONTENT_FIELD] = df[CONTENT_FIELD].str.replace('\t', '@TAB@')
    df = df.drop_duplicates([ID_FIELD, DATE_FIELD])
    return df

def join_text(texts):
    # skip missing and empty values
    return '<br>'.join((t for t in texts if t and pd.notna(t)))

def get_files_by_language(args, year):
    langs = defaultdict(dict)
    input_dir = get_input_dir(args, year)
    if os.path.isdir(input_dir):
        file_pattern = get_file_pattern(args, year)
        for p in Path(input_dir).glob(file_pattern):
            lang, _, suffix = p.stem.split('-')[-3:]
            filetype = get_file_type(suffix)
            if filetype is not None:
                langs[lang][filetype] = str(p)
                # also combine text with case differences in language names
                if lang.upper() == args.country:
                    langs[''][filetype] = str(p)
    return langs

def get_input_dir(args, year):
    if args.epo:
        input_dir = 'epo'
    elif args.us:
        input_dir = 'uspto'
    else:
        input_dir = '{}-{}'.format(args.country, year)
    return '{}/{}'.format(args.datadir, input_dir)

def get_file_pattern(args, year):
    if args.epo:
        file_pattern = '*/{}-{}-*.tab'.format(args.country, year)
    elif args.us:
        file_pattern = '*/USPTO-{}-{}-*.tab'.format(args.country, year)
    else:
        file_pattern = '{}-*-{}-*.tab'.format(args.country, year)
    return file_pattern

def get_file_type(suffix):
    for label, types in FILE_TYPES.items():
        if suffix in types:
            return label
    return None

def get_output_dir(args, year):
    if args.outdir:
        return args.outdir
    output_dir = get_input_dir(args, year)
    if args.epo or args.us:
        output_dir = '{}/text'.format(output_dir)
    return output_dir

def get_output_filename(args, lang, year):
    lang_year = f'{lang}-{year}' if lang else year
    if args.epo:
        output_filename = 'EP-{}-text.tab'.format(lang_year)
    elif args.us:
        output_filename = 'USPTO-{}-text.tab'.format(lang_year)
    else:
        output_filename = '{}-{}-text.tab'.format(args.country, lang_year)
    return output_filename

def read_data(filename):
    return pd.DataFrame(read_file(filename), columns=[ID_FIELD, DATE_FIELD, CONTENT_FIELD])

def write_data(df, filename):
    df = df[[ID_FIELD, DATE_FIELD, CONTENT_FIELD]]
    with open(filename, 'w') as f:
        df.to_csv(f, sep='\t', quoting=csv.QUOTE_NONE, header=False, index=False)

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
    endyear = datetime.datetime.now().year
    datadir = os.environ.get('PATENT_DATA_DIR', '/data/patents/pdfpatents/')
    message = 'Combine text parts for all patents with the given country code. A single year, or both a start and end year (inclusive) can be given. If no years are specified, the range {}-{} will be used.'.format(startyear, endyear)

    parser = argparse.ArgumentParser(description=message)
    parser.add_argument('country', metavar='country-code', help='Country code', type=check_country)
    parser.add_argument('start', metavar='start-year', nargs='?', help='First year of patents to process', type=check_year)
    parser.add_argument('end', metavar='end-year', nargs='?', help='Last year of patents to process', type=check_year)
    parser.add_argument('--epo', help='EPO data', action='store_true')
    parser.add_argument('--us', '--uspto', help='USPTO data', action='store_true')
    parser.add_argument('--outdir', metavar='output-dir', help='Output directory', type=check_dir_path)
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
