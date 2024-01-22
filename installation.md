## Installation overview

ABBA consists of:
* a Fiji plugin and 
* an extension for QuPath 

The QuPath extension, and in general, the use of QuPath is optional in ABBA but strongly encouraged.

:warning: NEW (Jan 2024) ! If you are using Windows, [a standalone installer is available](https://github.com/BIOP/ijp-imagetoatlas/releases/).


<!-- TOC -->
  * [Installation overview](#installation-overview)
* [Installing QuPath and its ABBA extension](#installing-qupath-and-its-abba-extension)
* [Fiji's ABBA plugin installation methods](#fijis-abba-plugin-installation-methods)
  * [Option 1 - Installing ABBA plugin with an installer (Windows only)](#option-1---installing-abba-plugin-with-an-installer--windows-only-)
  * [Option 2 - Installing ABBA plugin in Fiji](#option-2---installing-abba-plugin-in-fiji)
    * [1. Download and install Fiji](#1-download-and-install-fiji)
    * [2. Activate the PTBIOP update site](#2-activate-the-ptbiop-update-site)
    * [3. If you use OMERO, activate the OMERO 5.5-5.6 update site](#3-if-you-use-omero-activate-the-omero-55-56-update-site)
    * [4. Install elastix/transformix](#4-install-elastixtransformix)
        * [Windows](#windows)
        * [Mac](#mac)
        * [Linux](#linux)
        * [Indicate `elastix` and `transformix` executable location in Fiji:](#indicate-elastix-and-transformix-executable-location-in-fiji-)
    * [5. Installing DeepSlice locally (optional)](#5-installing-deepslice-locally--optional-)
  * [Option 3 - Installing ABBA plugin in python](#option-3---installing-abba-plugin-in-python)
* [Alternative installation - bash scripts](#alternative-installation---bash-scripts)
  * [Windows](#windows-1)
  * [Mac OSX](#mac-osx)
  * [Linux](#linux-1)
<!-- TOC -->

# Installing QuPath and its ABBA extension

1. Install [QuPath](https://qupath.github.io/)
2. Download [QuPath's ABBA extension zip file](https://github.com/BIOP/qupath-extension-abba/releases/latest) (named `qupath-extension-warpy-x.y.z.zip`)
3. Unzip it
4. Drag and drop the jar files it contains into QuPath's main graphical user interface

---
Optional: if you want to work on data coming from an OMERO database, install the [QuPath OMERO RAW extension](https://github.com/BIOP/qupath-extension-biop-omero). Please check its [readme](https://github.com/BIOP/qupath-extension-biop-omero/blob/omero-raw/README.md) for installation instructions.

---

5. Restart QuPath: in `Extensions>Managed extensions` you should see the following extensions installed:
  * ABBA
  * Image Combiner Warpy
  * Warpy
  * OMERO BIOP

# Fiji's ABBA plugin installation methods

ABBA is a Fiji plugin that can be installed easily, however, a bare ABBA plugin will lack key features. In particular ABBA is supposed to interact with these components:

* [DeepSlice](https://www.deepslice.com.au/): a deep-learning registration method that automatically register coronal sections to mouse and rat brain atlases
* [elastix/transformix](https://github.com/SuperElastix/elastix): the software that automates 2D in-plane registration
* [BrainGlobe](https://brainglobe.info/documentation/bg-atlasapi/index.html): a python library that standardized a set of atlases, and the way to access their data

There are 3 main ways to install ABBA, that correspond to different parts of the documentation:
* Using an installer (available for Windows only) ([Option 1.](#option-1---installing-abba-plugin-with-an-installer--windows-only-))
* Using [Fiji](https://fiji.sc/) ([Option 2.](#option-2---installing-abba-plugin-in-fiji))
* Using python with the pip dependency [abba_python](https://pypi.org/project/abba-python/) ([Option 3.](#option-3---installing-abba-plugin-in-python))


Here's a summary of the supported functionality depending on how ABBA is installed and on the OS:

|                                                                          | Headless | GUI | Mouse and Rat atlases | Brainglobe Atlases | DeepSlice (Local) |
|--------------------------------------------------------------------------|----------|-----|-----------------------|--------------------|-------------------|
| Opt 1. ABBA installer (Win only)                                         | [x]      | [x] | [x]                   | [x]                | [x]               |
| Opt 2. Fiji + PTBIOP update site (Win, Mac, Linux)                       | [x]      | [x] | [x]                   |                    |                   |
| Opt 2. Fiji + PTBIOP update site + DeepSlice conda env (Win, Mac, Linux) | [x]      | [x] | [x]                   |                    | [x]               |
| Opt 3. abba_python (Win, Linux)                                          | [x]      | [x] | [x]                   | [x]                |                   |
| Opt 3. abba_python + DeepSlice conda env (Win, Linux)                    | [x]      | [x] | [x]                   | [x]                | [x]               |
| Opt 3. abba_python (Mac)                                                 | [x]      |     | [x]                   | [x]                |                   |
| Opt 3. abba_python + DeepSlice conda env (Mac)                           | [x]      |     | [x]                   | [x]                | [x]               |

## Option 1 - Installing ABBA plugin with an installer (Windows only)

This possibility is the easiest one. It is available only if you are working with windows:

[https://github.com/BIOP/ijp-imagetoatlas/releases/latest](https://github.com/BIOP/ijp-imagetoatlas/releases/latest)

Be aware that you need an internet connection if you run the installer because at some point in the process some dependencies have to be downloaded from PyPI. Also, the first time you run ABBA, you will have to download the atlas you'd like to use from internet.

## Option 2 - Installing ABBA plugin in Fiji

### 1. Download and install Fiji
If you do not have Fiji already, download it and install it at [fiji.sc](https://fiji.sc/).

### 2. Activate the PTBIOP update site
* Click `Help > Update... > Manage update sites`
* Tick the checkbox `PTBIOP`
* Click `Apply and close`
* Click `Apply changes`
* Close and restart Fiji

### 3. If you use OMERO, activate the OMERO 5.5-5.6 update site
* Click `Help > Update... > Manage update sites`
* Tick the checkbox `OMERO 5.5-5.6`
* Click `Apply and close`
* Click `Apply changes`
* Close and restart Fiji

### 4. Install elastix/transformix

For automated 2D in-plane registration, ABBA uses the [elastix](https://github.com/SuperElastix/elastix) software, which is independent of Fiji. To use it, elastix should be installed, and its executable location (elastix and transformix) should be specified in Fiji.

* Download the [release 5.0.1 of elastix for your OS](https://github.com/SuperElastix/elastix/releases/tag/5.0.1). This documentation has been tested for elastix 5.0.1. The version [5.1.0 currently fails](https://github.com/BIOP/ijp-imagetoatlas/issues/171)!

* Unzip it somewhere convenient ( `C` drive on windows; `Applications` for Mac )

##### Windows

For windows users, you also need to install [Visual C++ redistributable](https://learn.microsoft.com/en-us/cpp/windows/latest-supported-vc-redist?view=msvc-170), (choose `vc_redist.x64.exe` for a 64-bit system).

##### Mac

Fiji will be calling the elastix executables, which are recognized as ‘unknown developers’ by Mac OS. Thus you need to [make security exceptions for both elastix and transformix](https://support.apple.com/en-hk/guide/mac-help/mh40616/mac) to avoid clicking indefinitely on the OS warning messages.

##### Linux
Nothing particular should be required for linux system.

##### Indicate `elastix` and `transformix` executable location in Fiji:

* In Fiji, execute `Plugins › BIOP › Set and Check Wrappers` then indicate the proper location of executable files, for instance:

![Setting elastix and transformix path in Fiji](./assets/img/fiji_elastix_transformix_path.png)

This message should show up in the ImageJ console (and maybe errors for Cellpose, but that's not important):
* `[INFO] Transformix	->	set :-)`
* `Elastix	->	set :-)`

Once elastix is installed, you can run [the following script](https://gist.github.com/NicoKiaru/b91f9f3f0069b765a49b5d4629a8b1c7) in Fiji to test elastix functionality. Save the linked file with a `.groovy` extension, open it in Fiji, and run it.

### 5. Installing DeepSlice locally (optional)

It is always possible to use the web interface of DeepSlice, without any further installation. However, it is convenient to have it installed locally since it will require less user manipulation and enable the registration procedure to be fully automated.

To install DeepSlice locally, please follow the instructions specified in the [BIOP wrappers repository](https://github.com/BIOP/ijl-utilities-wrappers#deepslice). In brief, the installation consists of:
* installing miniforge
* creating a conda environment for deepslice
* adding conda to the PATH environement variable (windows)
* specifying the conda environment location in Fiji


## Option 3 - Installing ABBA plugin in python

ABBA is available as a PyPI dependency. If you want to use this dependency, please check the installation instruction and startup command in the [readme of abba_python](https://github.com/BIOP/abba_python). 

# Alternative installation - bash scripts

The procedure described in this section installs:
- QuPath
- QuPath ABBA extension
- Fiji
- Fiji ABBA plugin
- elastix / transformix

However, it does not install DeepSlice, nor will you be able to use BrainGlobe atlases.
The procedure works for all OSes with minor variations explained below.

## Windows

* Install [Git for Windows](https://gitforwindows.org/) with standard options (just hit next on the installer)
* Download the [install scripts](https://github.com/BIOP/biop-bash-scripts/archive/refs/heads/main.zip) that comes from [this repository](https://github.com/BIOP/biop-bash-scripts)
* Unzip them
* Double click on the script `full_install_abba.sh`
* It is recommended to choose `C:/` as the install path
* Wait until the script ends

## Mac OSX

You will need to know your admin password.

:warning: If, for some reason, you want to keep your previous version of QuPath intact, save it to a different name before starting the script.

* Download the [install scripts](https://github.com/BIOP/biop-bash-scripts/archive/refs/heads/main.zip)
* Unzip them
* Right-click on `full_install_abba.command` (do NOT double click!)
* Click Open
* Accept the execution: you will be asked for your admin password (you won’t see any character as you type the password, and that’s normal!)
* Enter `/Applications/` as the install path (compulsory)
* Wait until the script ends

If you get this issue:

```
shell-init: error retrieving current directory: getcwd: cannot access parent directories: Operation not permitted
/bin/bash: ./full_install_abba.sh: Operation not permitted,
```

Then follow the operation explained in [https://osxdaily.com/2018/10/09/fix-operation-not-permitted-terminal-error-macos/](https://osxdaily.com/2018/10/09/fix-operation-not-permitted-terminal-error-macos/) and restart the script.

## Linux

* Download the [install scripts](https://github.com/BIOP/biop-bash-scripts/archive/refs/heads/main.zip)
* Unzip them
* Run `full_install_abba.sh`

-----
[**Back to documentation main page**](index.md)

-----

