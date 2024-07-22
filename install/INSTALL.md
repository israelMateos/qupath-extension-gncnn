# GNCnn Installation

> [!WARNING]
> This extension **is developed for QuPath 0.5.0 or higher**, and has not been tested with other versions.
>
> If having a NVIDIA GPU, the extension only supports CUDA 11.1. CPU is also supported.

GNCnn was tested on Ubuntu 20.04 and 22.04, Windows 10 and macOS Big Sur 11.4. It requires Python 3.8 or 3.9 (not higher).

**0.** Install Python 3.8 or 3.9 (not higher) on your system.

<!-- **0.** Install on your system the following dependencies:

- Python 3.8 or 3.9 (not higher)
- Git LFS (for downloading the model weights) -->

**1.** Download the `.jar` file for the extension from the [Releases](https://github.com/israelMateos/qupath-extension-gncnn/releases/latest) page.

**2.** This step depends on the platform you are using.

- **Linux**: edit `install/linux.cfg`.
- **Windows**: edit `install/windows.cfg`.
- **macOS**: edit `install/mac.cfg`.

In the configuration file, you should set the following variables:

- `qupath_path`: 
  - For Linux, the path to the QuPath installation directory. It should contain the `bin` directory, in which the `QuPath` executable is located.
  - For Windows and macOS, the path to the QuPath executable. In Windows, it should include the console version of QuPath, _e.g._ `QuPath-0.5.1 (console).exe`. In macOS, it should include the executable buried inside the `.app` directory, _e.g._ `QuPath-0.5.1-x64.app/Contents/MacOS/QuPath-0.5.1-x64`.
- `extension_path`: the path to the `.jar` file downloaded in step 1. It should include the file name.
  
**3.** From the `install` directory, run the following command:

- **Linux**:

```bash
bash install.sh
```

- **Windows**:

```bash
.\install.bat
```

- **macOS**:

```bash
sh install.sh
```

This script will install the Python tool on which the extension depends, and will download the model weights. It will also create a new directory for the GNCnn extension in the QuPath extensions directory. The next time you open QuPath, the extension will be available in the menu.

**4.** Once the extension is installed, you can remove this repository's directory (`qupath-extension-gncnn`) from your system.