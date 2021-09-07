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
Note that the `pipeline.sh` script will generate its output in your current work directory. So first `cd` to some folder you want the data to end up in.

```bash
path/to/pipeline.sh <path/to/translate.sh> [--any-translate-arguments] -- <files-to-process>
```

So for example, to test or run on valhalla locally, do something like
```bash
./pipeline.sh models/es-en/translate.sh --cpu-threads 4 -- /data/europat/paired/es-en/ES-EN-*.tab
```

Or on CSD3, it would be more like:
```bash
cd /rds/project/rds-48gU72OtDNY/europat
sbatch ~/src/europat-scripts/pipeline.sh models/es-en/translate.sh -- paired/es-en/ES-EN-2020-*.tab
```


