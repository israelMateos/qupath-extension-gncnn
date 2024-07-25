"""
From: https://github.com/Nicolik/MESCnn
Modified by: Israel Mateos-Aparicio-Ruiz
Modifications:
    - Removed unused functions
    - Adapted tile2xywh to own format
"""
import os

import cv2
import numpy as np


def get_dataset_dicts_validation(basepath):
    dataset_dicts = []
    filenames = os.listdir(basepath)

    for image in filenames:
        slide_id = image.split(".")[-1]
        record = {}

        filename = os.path.join(basepath, image)
        height, width = cv2.imread(filename).shape[:2]

        record["file_name"] = filename
        record["image_id"] = slide_id
        record["height"] = height
        record["width"] = width

        dataset_dicts.append(record)

    return dataset_dicts


def tile2xywh(filename):
    x_tile = int(filename.split('x=')[1].split(',')[0])
    y_tile = int(filename.split('y=')[1].split(',')[0])
    w_tile = int(filename.split('w=')[1].split(',')[0])
    h_tile = int(filename.split('h=')[1].split(']')[0])
    return x_tile, y_tile, w_tile, h_tile


def mask2polygon(mask):
    contours, heirarchy = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    polygon = max(contours, key=cv2.contourArea)
    polygon = np.squeeze(polygon, axis=1)
    return polygon


def get_area_10x(polygon):
    polygon_small = np.array([[point[0], point[1]] for point in polygon])
    area_um = cv2.contourArea(polygon_small)
    return area_um
