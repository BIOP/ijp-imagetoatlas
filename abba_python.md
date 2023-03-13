# [ABBA-Python](https://github.com/NicoKiaru/ABBA-Python)

ABBA, which is Java based, can be used in conjunction with Python. This allows to use Python functionalities (BrainGlobe, DeepSlice) during the registration, but also to run python code for analysis after the registration. The 'glue' between Java and Python is made with [`JPype`](https://github.com/jpype-project/jpype) and [`PyImageJ`](https://github.com/imagej/pyimagej). There are different ways to use ABBA-Python as explained below.

## Windows installer

 The first option is simply to use the [ABBA-Python installer](https://github.com/NicoKiaru/ABBA-Python/releases/) (windows only currently). With this option, you have access to DeepSlice and BrainGlobe. Java and Python work together on the background, and you won't notice it. When Java requires a python module (like DeepSlice), it executes some python code which returns its result to Java.

This is the preferred way to use ABBA with a graphical interface. Note that the installer ships a conda environment which can potentially be used for python scripting (but it does not include jupyter, so you can't use notebooks directly, you have to pip install jupyter manually to the env (not tested)).

## With jupyter notebooks

You can create a conda environment for ABBA by using the specified [environment.yml](https://github.com/NicoKiaru/ABBA-Python/blob/dev/environment.yml) file. Using mamba (minimamba) is the prefered way to build the environment since it takes too long with conda (a day ? I was not patient enough to let it finish).

This environment has some constrains because DeepSlice do not seem to work with recent libraries.

With this environment activated, you can clone [ABBA-Python](https://github.com/NicoKiaru/ABBA-Python), then test, run, modify or create new notebooks. 'Self sufficient' [example notebooks](https://github.com/NicoKiaru/ABBA-Python/tree/dev/notebooks) are provided in the ABBA-Python repository.

## With an IDE (PyCharm)

Similarly to the jupyter notebooks examples, you need to create a conda environment from [environment.yml](https://github.com/NicoKiaru/ABBA-Python/blob/dev/environment.yml).

Then, using your favorite IDE (I personnally use [PyCharm](https://www.jetbrains.com/pycharm/download/)), you can specify that you work within this conda environment and start writing some Python code. I personally find this way of coding Python code much easier since you can have access to all the code of the environement (`Ctrl+B` is a life-saver).

To know the function available from Python with an Abba object, you can browse [Abba.py](https://github.com/NicoKiaru/ABBA-Python/blob/dev/src/abba_python/Abba.py). There's not a lot of documentation unfortunately.

There's also an intrinsic difficulty if you want to access all the available API: the Python is discovered fine, but JPype can also import Java classes on the fly, and the Java methods available from these Java classes can't be automatically discovered in the Python IDE. In practice, when I need to access both what's available in Python and in Java, I have PyCharm opened on the ABBA-Python repository, and IntelliJ opened on the original [Java ABBA repository](https://github.com/BIOP/ijp-imagetoatlas). Yep, that's not very straightforward. 

There are probably some possibilities to make PyCharm discover automatically Java classes and method, I've heard of them, but I do not know them, and are they easy to use ?
 
