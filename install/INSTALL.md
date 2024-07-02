# GDCnn Installation

> [!WARNING]
> This extension **is developed for QuPath 0.5.0**, and has not been tested with other versions.
>
> This extension requires a CUDA-compatible GPU, and has been tested only with CUDA 11.1.

GDCnn was tested on Ubuntu 20.04 and 22.04. It requires Python 3.8 or 3.9 (not higher).

**0.** Install Python 3.8 or 3.9 (not higher) on your system.

<!-- **0.** Install on your system the following dependencies:

- Python 3.8 or 3.9 (not higher)
- Git LFS (for downloading the model weights) -->

**1.** Download the `.jar` file for the extension from the [Releases](https://github.com/israelMateos/qupath-extension-gdcnn/releases/latest) page.

**2.** In `install/install.cfg`, define the following variables:

- `qupath_path`: the path to the QuPath installation directory. It should contain the `bin` directory, in which the `QuPath` executable is located.
- `extension_path`: the path to the `.jar` file downloaded in step 1. It should include the file name.
  
**3.** From the `install` directory, run the following command:

```bash
bash install.sh
```

This script will copy the extension `.jar` file to the QuPath extensions directory, and will create a new directory for the GDCnn extension in the QuPath extensions directory. The next time you open QuPath, the extension will be available in the menu.