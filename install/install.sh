#!/usr/bin/env bash

# Get QuPath installation path from install.cfg qupath_path variable
source ./linux.cfg
echo "QuPath installation path: ${qupath_path}"
echo "Extension path: ${extension_path}"

# Detect NVIDIA GPU and save boolean result
echo "Detecting NVIDIA GPU..."
nvidia-smi > /dev/null
if [ $? -eq 0 ]; then
    echo "NVIDIA GPU detected."
    suffix='cu111'
else
    echo "NVIDIA GPU not detected."
    suffix='cpu'
fi

# Install the required Python packages
echo "Installing the required Python packages..."
# Install numpy and cached-property first to avoid errors in Python 3.9
pip install "numpy>=1.24.4,<2" "cached-property>=1.5.2" --no-cache-dir
# Install torch and torchvision pre-built with CUDA 11.1 if NVIDIA GPU is detected
# Otherwise, install the CPU version
# If not installed previously, gdcnn cannot be installed (detectron2 dependency)
pip install torch==1.8.0+${suffix} \
    torchvision==0.9.0+${suffix} \
    --no-cache-dir \
    -f https://download.pytorch.org/whl/torch_stable.html
# Install pre-built mmcv-full to avoid errors when compiling from source
if [ $suffix = 'cu111' ]; then
    pip install "mmcv-full==1.7.2" --no-cache-dir -f https://download.openmmlab.com/mmcv/dist/cu111/torch1.8.0/index.html
# If no NVIDIA GPU is detected, install mmcv (lite version) instead
else
    pip install "mmcv==1.7.2" --no-cache-dir
fi

# pip install ../gdcnn/[linux-${suffix}] --no-cache-dir
echo "Python packages installed."

# Download the models
echo "Downloading the models..."
python3 ./download_models.py
echo "Models downloaded."

# Get model target paths
echo "Copying the models to the target paths..."
gdcnn_path=$(python -c "import gdcnn; print(gdcnn.__path__[0])")
detection_model_dir="${gdcnn_path}/detection/logs/cascade_mask_rcnn_R_50_FPN_1x/external-validation/output/"
classification_model_dir="${gdcnn_path}/classification/logs/"

# Copy the detection model to the target path
mkdir -p ${detection_model_dir}
cp ../models/models/detection/model_final.pth ${detection_model_dir}
echo "Detection model copied."

# Copy the classification models to the target path
mkdir -p ${classification_model_dir}
cp -r ../models/models/classification/* ${classification_model_dir}
echo "Classification models copied."

# Remove models directory
rm -rf ../models

# Install the QuPath extension
echo "Installing QuPath extension"
qupath="${qupath_path}/bin/QuPath"
$qupath script ./install.groovy --args $extension_path --save
echo "QuPath extension installed."