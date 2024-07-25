"""
From: https://github.com/Nicolik/MESCnn
Modified by: Israel Mateos-Aparicio-Ruiz
Modifications:
    - Changed logs_dir to 'gncnn/classification/logs'
"""
import os
from gncnn.definitions import ROOT_DIR


def get_logs_path(root_dir=None):
    root_dir = ROOT_DIR if root_dir is None else root_dir
    logs_dir = os.path.join(root_dir, 'gncnn', 'classification', 'logs')
    os.makedirs(logs_dir, exist_ok=True)
    return logs_dir
