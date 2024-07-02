"""
From: https://github.com/Nicolik/MESCnn
Modified by: Israel Mateos-Aparicio-Ruiz
Modifications:
    - Changed local_dir to './gdcnn'
"""
from huggingface_hub import hf_hub_download


def download_classifier(net_name, target, train_version):
    return hf_hub_download(
        repo_id="MESCnn/MESCnn",
        filename=f"classification/logs/cnn/holdout/{net_name}_{target}_{train_version}.pth",
        token="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",
        local_dir='./gdcnn',
        local_dir_use_symlinks=False,
        force_download=True,
    )
