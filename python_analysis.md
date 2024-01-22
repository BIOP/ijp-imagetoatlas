# Using ABBA with Python

-----
[**Back to documentation main page**](index.md)

-----

## Using ABBA with Python

ABBA, which is Java based, can be used in conjunction with Python. This allows to use Python functionalities (BrainGlobe, DeepSlice) during the registration, but also to run python code for analysis after the registration. The 'glue' between Java and Python is made with [`JPype`](https://github.com/jpype-project/jpype) and [`PyImageJ`](https://github.com/imagej/pyimagej).


To enable that, you need to install ABBA via pip. Please check [the installation guide](installation.md).

There are [example jupyter notebooks](https://github.com/BIOP/abba_python/tree/main/example_notebooks) in the abba python repository.

## Other repositories for data analysis

You can use the scripts developed by [@enassar](https://github.com/enassar) and [@nickdelgrosso](https://github.com/nickdelgrosso) in this repository: https://github.com/nickdelgrosso/ABBA-QuPath-utility-scripts in order to automate cell detection and export.

:warning: the repo has not been updated to the more recent ABBA version, so please use https://github.com/NicoKiaru/ABBA-QuPath-utility-scripts instead.

An alternative repository which allows to do additional processing is available in https://github.com/bmi-lsym/ABBA-QuPath-post_processing

Note that these set of scripts may not have been yet updated to the most recent version of ABBA.


-----
[**Back to documentation main page**](index.md)

-----