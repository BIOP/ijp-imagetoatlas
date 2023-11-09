# Which data source can be used by ABBA ?

-----
[**Back to documentation main page**](index.md)

-----

<!-- TOC -->
* [Which data source can be used by ABBA ?](#which-data-source-can-be-used-by-abba-)
  * [1. Any Bio-Formats supported file format,](#1-any-bio-formats-supported-file-format)
  * [2. ideally multi-resolution (Bio-Formats supported),](#2-ideally-multi-resolution--bio-formats-supported--)
  * [3. and calibrated.](#3-and-calibrated)
* [Tested file formats](#tested-file-formats)
* [More technicalities](#more-technicalities)
<!-- TOC -->

-----

ABBA can read data [from many sources](dataset_prerequisite.md#more-technicalities), but these two sources are the most commonly used:
- [Bio-Formats](https://bio-formats.readthedocs.io/en/latest/supported-formats.html) compatible files
- Images from an [OMERO](https://www.openmicroscopy.org/omero/) database

This section deals with the file formats requirements. 

:warning: While ABBA can [open files directly](import_qupath_project.html#direct-opening-of-a-file), it is highly recommended to package all files from a single animal into a QuPath project, as explained in the [create QuPath dataset section](create_qupath_dataset.md).

In short ABBA can use any [Bio-Formats supported file format](dataset_prerequisite.md#1-any-bio-formats-supported-file-format), [ideally multi-resolution](dataset_prerequisite.md#2-ideally-multi-resolution--bio-formats-supported--), and [calibrated](dataset_prerequisite.md#3-and-calibrated). It can also stream images for an OMERO database.

## 1. Any Bio-Formats supported file format,
All Bio-Formats readable file formats are supported. You can check in the [Bio-Formats documentation](https://bio-formats.readthedocs.io/en/latest/supported-formats.html) if your files are supported. 

NB: Files which can be opened **only** via [OpenSlide](https://openslide.org/) in QuPath are not supported.

Besides Bio-Formats compatibility, there are two 'classical requirements' for an optimal use of ABBA:

## 2. ideally multi-resolution (Bio-Formats supported),

Brain slices are usually very big 2d images and ABBA has been designed to work with such images. To do so, it relies on [tiled pyramidal file formats](https://qupath.readthedocs.io/en/0.4/docs/intro/formats.html): a file contains not only its data at the highest resolution, but also  pre-computed downsampled versions of itself.
To know if Bio-Formats supports a potential multi-resolution file format, check whether [the `Pyramid` column](https://bio-formats.readthedocs.io/en/latest/supported-formats.html) is checked.

If files are pyramidal, ABBA can load only a sub-part (**tiles**) of an image at a specific resolution level (**pyramid level**) needed, thus speeding-up very significantly the display and processing of these images. Having pyramidal images also helps improving the registration quality: registrations are using large scale features (size above  a few cells), which are incorrectly sampled if no downsampled image pre-exists.

For non-pyramidal files, the full data needs to be loaded, displayed and registered. This can be fine for small images (<2000x2000 pixels), but will be annoying when images are bigger.

If your images are not pyramidal, we advise to convert your files to pyrimadal OME-TIFF formats. This can be done for instance by using one of the following options:
- [Kheops](https://github.com/BIOP/ijp-kheops) Fiji plugin ,
- [NGFF converter by Glencoe](https://www.glencoesoftware.com/products/ngff-converter/) (!n5 not supported, choose OME-TIFF).

Alternatively, if you have access to an OMERO database, you can upload your images to OMERO, and the pyramidal levels will by computed by the server. 

RGB images as well as fluorescent 8-bits and 16-bits images have been successfully tested.


## 3. and calibrated.

All files need to be properly calibrated (microns, millimeters, etc, but not pixels!). ABBA takes advantage of the  calibration to set appropriate registration parameters. To know if your images are properly calibrated, you can for instance open them in QuPath, using a Bio-Formats image server, and check that the pixel size is set and correct.

If your images are uncalibrated, ABBA will assume that each pixel has a size of 1mm, which will lead to gigantic images. To correct for this issue, you can re-save your images with the proper metadata and make sure that the metadata is recognized by bio-formats. This can be done with [Kheops](https://github.com/BIOP/ijp-kheops), which offers an option to override the pixel size while an OME-TIFF version is re-exported.

If you want to avoid the conversion step and keep using uncalibrated files, you can assemble your files in a QuPath  project, then [set each image pixel size](https://qupath.readthedocs.io/en/0.4/docs/starting/first_steps.html#setting-the-pixel-size). ABBA will then read the QuPath metadata, and ignore the original bio-formats metadata.

---

:warning: For this QuPath method to work, you need to set the calibration BEFORE the project is imported in ABBA

---


# Tested file formats

File formats  which are known to work:
- Olympus / Evident `.vsi`
- Zeiss `.czi` (you have to tick `Split RGB channels` for 16-bits RGB images)
- Pyramidal OME-TIFF `.ome.tif`

According to bio-formats, these formats should be optimal as well:
- Aperio `.svs` `.afi`
- Imaris `.ims`
- Dicom `.dcm` `.dicom`
- Hamamatsu `.ndpi` `ndpis`
- JPEG2000 `.jp2`
- Keller Lab Block `.klb`
- Vectra QPTIFF `.qptiff`
- Ventana BIF `.bif`

Note that Leica `.lif` files are not well supported because their multi-resolution data are not handled by Bio-Formats.

# More technicalities

Hidden somewhere in the state of an ABBA instance, one or several BigDataViewer dataset are used. BigDataViewer dataset can use different backends, which are implementing the java `ImageLoader` interface. See this [youtube video](https://youtu.be/LHI7vXiUUms?t=280) for more details.

Depending on the image loader, the data can be read from various sources:
- Loaders from [BigDataViewer core](https://github.com/bigdataviewer/bigdataviewer-core):
  - [xml/hdf5](https://github.com/bigdataviewer/bigdataviewer-core/tree/master/src/main/java/bdv/img/hdf5)
  - [catmaid](https://github.com/bigdataviewer/bigdataviewer-core/tree/master/src/main/java/bdv/img/catmaid)
  - [imaris](https://github.com/bigdataviewer/bigdataviewer-core/tree/master/src/main/java/bdv/img/imaris)
  - [openconnectome](https://github.com/bigdataviewer/bigdataviewer-core/tree/master/src/main/java/bdv/img/openconnectome)
  - [n5](https://github.com/bigdataviewer/bigdataviewer-core/tree/master/src/main/java/bdv/img/n5)
  - [BigDataServer](https://github.com/bigdataviewer/bigdataviewer-core/tree/master/src/main/java/bdv/img/remote)
- [bigdataviewer-image-loaders](https://github.com/BIOP/bigdataviewer-image-loaders) is a library developped and maintained by the BIOP. It is a meta image-loader that delegates its loading depending on the image URI, a mechanism similar to the QuPath Image Server. It allows to load/stream data from:
  - [Bio-Formats](https://github.com/BIOP/bigdataviewer-image-loaders/tree/master/src/main/java/ch/epfl/biop/bdv/img/bioformats)
  - [OMERO](https://github.com/BIOP/bigdataviewer-image-loaders/tree/master/src/main/java/ch/epfl/biop/bdv/img/omero)
  - [QuPath projects](https://github.com/BIOP/bigdataviewer-image-loaders/tree/master/src/main/java/ch/epfl/biop/bdv/img/qupath). It is in itself a meta-loader in the meta image loader (ouch) which calls other loaders depending on the QuPath Image Server.

If you add the Fiji MoBiE update site, you should be able to use OME/ZARR dataset:
  - [OMEZARR loader](https://github.com/mobie/mobie-io/tree/main/src/main/java/org/embl/mobie/io/ome/zarr/loaders)

Now, using ABBA, the easiest way is again to make a QuPath project with Bio-Formats supported files or with OMERO images through the use of the [QuPath OMERO RAW extension](https://github.com/BIOP/qupath-extension-biop-omero).

While other sources from other image loaders should work almost 'out of the box', there is no very direct way to do that because it's not in the GUI of ABBA. If you need other sources coming from different loaders, please write an issue on GitHub or open a post on the forum. 

One option is to open an xml bdv dataset with BigDataViewer-Playground and [drag and drop sources into ABBA's BigDataViewer Playground window](import_qupath_project.md#sources-from-bigdataviewer-playground).

-----
[**Back to documentation main page**](index.md)

-----
