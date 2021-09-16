# Europat processing pipeline
For when you already have document pairs in tabulated format (as provided by Omniscien), this pipeline translates and aligns sentence pairs from those.

## Set-up
This is mostly set up to be set up on CSD3 or Cirrus. Might also work on Valhalla though, just didn't test that :)

Make sure you checked out all the submodules as well:
```bash
git submodule update --init --recursive
```

Then, just run the setup script:
```bash
./setup.sh install-all
```

(or use `setup.sh install -f <specific-software-package>` to re-compile anything you updated.)

This will check and if necessary compile all the programs into `./bin`.

The `pipeline.sh` script loads `init.sh` which makes sure the necessary modules are available and `./bin` is added to `$PATH`.

Models can be put anywhere. Some are part of this repository (not yet sure whether that was a good idea, they're stored in lfs but still...) but any model that has a `translate.sh` style script will do.

# Running
Make sure all files are in a file, e.g. input.txt:

```sh
ls -1d /lustre/home/dc007/efarrow/europat/ocr_text_new/PL-????-ocr.tab >> input.txt
```

Then for each language, queue translation. Don't forget the input file as the only argument. Array indices are line numbers in the input.txt file.

```sh
sbatch --array=153-170 -J translate-hr ./translate.slurm input.txt
```

Fast-align classification is fast, so no need for an array job. Just schedule it and pass a bunch of files as arguments to the command.

```sh
sbatch -J fasttext-hr ./pipeline-fasttext.sh /lustre/home/dc007/efarrow/europat/ocr_text_new/PL-????-ocr.tab
```

Alignment (also does combining with English text and stuff). Array parameter is the years. Don't forget the language as the only argument to align.slurm.

```sh
sbatch -J align-hr --array=1995-1998,2007-2019 ./align.slurm hr
```

Finally turning the whole thing into a tmx file. Arguments are the language first, and then all the years we have files for.

```sh
sbatch -j tmx-hr ./pipeline-tmx.slurm hr {1995..1998} {2007..2019}
```