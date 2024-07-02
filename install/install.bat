:: Batch Script

:: Get QuPath installation path from install.cfg qupath_path variable
call install.cfg
echo QuPath installation path: %qupath_path%
echo Extension path: %extension_path%

:: Install the required Python packages
echo Installing the required Python packages...
:: Install torch and torchvision pre-built with CUDA 11.1
:: If not installed previously, gdcnn cannot be installed (detectron2 dependency)
pip install torch==1.8.0+cu111 torchvision==0.9.0+cu111 --no-cache-dir -f https://download.pytorch.org/whl/torch_stable.html
:: Install pre-built mmcv-full to avoid errors when compiling from source
pip install "mmcv-full>=1.4.6" --no-cache-dir -f https://download.openmmlab.com/mmcv/dist/cu111/torch1.8.0/index.html

pip install .\gdcnn\ --no-cache-dir
echo Python packages installed.

:: Get model target paths
echo Copying the models to the target paths...
for /f "delims=" %%i in ('python -c "import gdcnn; print(gdcnn.__path__[0])"') do set gdcnn_path=%%i
set detection_model_dir="%gdcnn_path%\detection\logs\cascade_mask_rcnn_R_50_FPN_1x\external-validation\output\"
set classification_model_dir="%gdcnn_path%\classification\logs\"

:: Copy the detection model to the target path
if not exist %detection_model_dir% mkdir %detection_model_dir%
copy ..\models\detection\model_final.pth %detection_model_dir%
echo Detection model copied.

:: Copy the classification models to the target path
if not exist %classification_model_dir% mkdir %classification_model_dir%
xcopy /E /I ..\models\classification\* %classification_model_dir%
echo Classification models copied.

:: Remove models directory
rmdir /S /Q ..\models

:: Install the QuPath extension
echo Installing QuPath extension
set qupath="%qupath_path%\bin\QuPath"
%qupath% script .\install.groovy --args %qupath_path% --save
echo QuPath extension installed.