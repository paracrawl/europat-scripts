#!/usr/bin/python3

import argparse
import csv
import datetime
import os
import pandas as pd

EARLIEST_YEAR = 1800
DEFAULT_START_YEAR = 1980

EN_LANGUAGE = 'en'
ID_FIELD = 'id'
FAMILY_FIELD = 'family'

def match_families(args):
    print(f'Reading {EN_LANGUAGE} patents from {args.en}')
    en_patents = read_data(args.en, EN_LANGUAGE)
    if args.verbose:
        print(f'  Read {len(en_patents)} {EN_LANGUAGE} patents')
    for year in range(args.start, args.end+1):
        session = f'{args.country}-{year}'
        yeardir = f'{args.datadir}/{session}'
        infofile = f'{yeardir}/{session}-info.txt'
        language = args.country.lower()
        patents = read_data(infofile, language)
        if len(patents) == 0:
            continue
        if args.verbose:
            print(f'Read {len(patents)} patents for {session}')
        matches = patents.join(en_patents, how='inner', rsuffix=EN_LANGUAGE)
        if len(matches) == 0:
            print(f'  No matches for {session}')
            continue
        if args.verbose:
            print(f'  Found {len(matches)} matches for {session}')
        output_filename = f'{args.country}-EN-{year}-family.tab'
        write_data(matches, f'{args.outdir}/{output_filename}', language)
        if args.verbose:
            print(f'  Saved {output_filename} in {args.outdir}')

def write_data(df, filename, language):
    # make sure the fields are in order
    df = df[[f'{language}_{ID_FIELD}', f'{language}_{FAMILY_FIELD}', f'{EN_LANGUAGE}_{ID_FIELD}', f'{EN_LANGUAGE}_{FAMILY_FIELD}']]
    with open(filename, 'w') as f:
        df.to_csv(f, sep='\t', quoting=csv.QUOTE_NONE, header=False, index=False)

def read_data(filename, language):
    df = pd.DataFrame(read_file(filename), columns=[f'{language}_{ID_FIELD}', FAMILY_FIELD])
    df[f'{language}_{FAMILY_FIELD}'] = df[FAMILY_FIELD]
    return df.set_index(FAMILY_FIELD)

def read_file(filename):
    if os.path.isfile(filename):
        with open(filename) as file:
            for line in file:
                docid, family = line.strip().split('\t', 2)[:2]
                yield docid.replace('.', '-'), family

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
    datadir = os.environ.get('PATENT_DATA_DIR', '/data/patents/pdfpatents')
    outdir = os.environ.get('PATENT_FAMILY_DIR', f'{datadir}/families')
    message = f'Match patent families for patents with the given country code. A single year, or both a start and end year (inclusive) can be given. If no years are specified, the range {startyear}-{endyear} will be used.'

    parser = argparse.ArgumentParser(description=message)
    parser.add_argument('en', help='Data file with EN family data')
    parser.add_argument('country', metavar='country-code', help='Country code', type=check_country)
    parser.add_argument('start', metavar='start-year', nargs='?', help='First year of patents to process', type=check_year)
    parser.add_argument('end', metavar='end-year', nargs='?', help='Last year of patents to process', type=check_year)
    parser.add_argument('--datadir', help='Directory with data files to process', default=datadir)
    parser.add_argument('--outdir', metavar='output-dir', help='Output directory', default=outdir, type=check_dir_path)
    parser.add_argument('-v', '--verbose', help='Verbose output', action='store_true')
    args = parser.parse_args()

    # dynamic defaults for missing positional args
    args.end = args.end or args.start or endyear
    args.start = args.start or startyear
    match_families(args)


if __name__ == '__main__':
    # execute only if run as a script
    main()
