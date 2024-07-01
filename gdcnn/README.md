# GDCnn

This project is a fork of [MESCnn](https://github.com/Nicolik/MESCnn), which is an end-to-end pipeline for glomerular Oxford classification of 
whole slide images. The paper for MESCnn can be found at
[https://www.sciencedirect.com/science/article/pii/S0169260723004807](https://www.sciencedirect.com/science/article/pii/S0169260723004807),
and its citation in BibTeX is the following:

```
@article{ALTINI2023107814,
title = {Performance and Limitations of a Supervised Deep Learning Approach for the Histopathological Oxford Classification of Glomeruli with IgA Nephropathy},
journal = {Computer Methods and Programs in Biomedicine},
pages = {107814},
year = {2023},
issn = {0169-2607},
doi = {https://doi.org/10.1016/j.cmpb.2023.107814},
url = {https://www.sciencedirect.com/science/article/pii/S0169260723004807},
author = {Nicola Altini and Michele Rossini and Sándor Turkevi-Nagy and Francesco Pesce and Paola Pontrelli and Berardino Prencipe and Francesco Berloco and Surya Seshan and Jean-Baptiste Gibier and Anibal Pedraza Dorado and Gloria Bueno and Licia Peruzzi and Mattia Rossi and Albino Eccher and Feifei Li and Adamantios Koumpis and Oya Beyan and Jonathan Barratt and Huy Quoc Vo and Chandra Mohan and Hien Van Nguyen and Pietro Antonio Cicalese and Angela Ernst and Loreto Gesualdo and Vitoantonio Bevilacqua and Jan Ulrich Becker},
}
```

*GDCnn* (*G*lomerular *D*isease *C*lassification by *n*eural *n*etwork) aims
to adapt MESCnn for a different classification than the Oxford one, implementing
a 2-step classification of glomeruli: first, into sclerosed and non-sclerosed classes,
and second, into 12 other pathologies among the non-sclerosed glomeruli.

## Installation

To install this tool, which is a local Python package, you need to have Python 3.8 or higher installed on your system.
Then, you can install the package by running the following command in the terminal, outside this folder:

```bash
pip install ./gdcnn/
```

## Usage

This tool is thought to be used from a QuPath extension, which will provide a
user-friendly interface to run the pipeline on whole slide images.
