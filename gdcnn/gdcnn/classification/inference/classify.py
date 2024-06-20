import logging
import os

import numpy as np
import torch
import pandas as pd

from mmcls.apis import init_model
from mmcls.datasets.pipelines import Compose
from mmcv.parallel import collate, scatter

from gdcnn.classification.gutils.utils import get_proper_device
from gdcnn.classification.inference.paths import get_logs_path
from gdcnn.definitions import ROOT_DIR


def get_most_predicted_topk_classes(scores, topk):
    """Each glomerulus have topk classes predicted. This function returns the topk classes that are most predicted for the whole WSI."""
    scores = scores.sum(axis=0)
    topk_labels = np.argsort(scores)[::-1][:topk]
    return topk_labels


def main():
    import argparse
    parser = argparse.ArgumentParser(description='Classifiers Inference for Glomeruli Task')
    parser.add_argument('-r', '--root-path', type=str, help='Root path', default=ROOT_DIR)
    parser.add_argument('-e', '--export-dir', type=str, help='Directory to export report', required=True)
    parser.add_argument('--netB', type=str, help='Network architecture for Sclerotic vs. Non-Sclerotic', required=True)
    parser.add_argument('--netM', type=str, help='Network architecture for 12 classes (Non-sclerotic)', required=False)
    parser.add_argument('--multi', action='store_true', help='Use 12-class classification after Sclerotic vs. Non-Sclerotic classification', default=False)
    parser.add_argument('--topk', type=int, help='Top k classes to consider for 12-class classification', default=1)
    args = parser.parse_args()

    if args.multi and args.netM is None:
        parser.error("--multi requires --netM")

    net_name_dict = {
        "B": args.netB,
        "M": args.netM if args.multi else None,
    }

    root_path = args.root_path
    export_dir = args.export_dir

    mesc_log_dir = get_logs_path(root_path)
    crop_dir = os.path.join(export_dir, "Temp", "ann-export-output")

    if not os.path.exists(crop_dir):
        logging.warning(f"Directory {crop_dir} does not exist")
        return

    # report_dir = os.path.join(export_dir, "Report")
    report_dir = os.path.join(export_dir, "Report", f"B-{args.netB}_M-{args.netM}")
    os.makedirs(report_dir, exist_ok=True)

    wsi_ids = os.listdir(crop_dir)
    if len(wsi_ids) == 0:
        logging.warning("No WSI IDs found in the export directory")
        return

    wsi_dict = {
        'WSI-ID': [],
        'most-predicted-class': [],
        'ratio-most-predicted-class': [],
    }

    output_file_summary_csv = os.path.join(report_dir, "summary.csv")

    for wsi_id in wsi_ids:
        output_file_csv = os.path.join(report_dir, f"{wsi_id}.csv")
        prediction_dir = os.path.join(crop_dir, wsi_id)

        mesc_dict = {
            'filename': [],
            'predicted-class': [],

            'NoSclerotic-prob': [],
            'Sclerotic-prob': [],

            'ABMGN-prob': [],
            'ANCA-prob': [],
            'C3-GN-prob': [],
            'CryoglobulinemicGN-prob': [],
            'DDD-prob': [],
            'Fibrillary-prob': [],
            'IAGN-prob': [],
            'IgAGN-prob': [],
            'MPGN-prob': [],
            'Membranous-prob': [],
            'PGNMID-prob': [],
            'SLEGN-IV-prob': [],
        }

        # Model 1: Sclerotic vs. Non-Sclerotic
        net_name = net_name_dict["B"]
        net_path = os.path.join(mesc_log_dir, 'binary', net_name, f'{net_name}_B_ckpt.pth')
        config_path = os.path.join(mesc_log_dir, 'binary', net_name, f'{net_name}_B_config.py')

        device = get_proper_device()
        bin_model = init_model(config_path, net_path, device=device)

        # Model 2: 12 classes
        if args.multi:
            net_name = net_name_dict["M"]
            net_path = os.path.join(mesc_log_dir, '12classes', net_name, f'{net_name}_M_ckpt.pth')
            config_path = os.path.join(mesc_log_dir, '12classes', net_name, f'{net_name}_M_config.py')
            
            mult_model = init_model(config_path, net_path, device=device)
        
        images_list = os.listdir(prediction_dir)
        images_list = [os.path.join(prediction_dir, f) for f in images_list if f.endswith(".png")]
        for image_path in images_list:
            # Build the data pipeline
            data = dict(img_info=dict(filename=image_path), img_prefix=None)
            pipeline = bin_model.cfg.data.test.pipeline
            comp_pipeline = Compose(pipeline)
            data = comp_pipeline(data)
            data = collate([data], samples_per_gpu=1)
            if next(bin_model.parameters()).is_cuda:
                # Scatter to specified GPU
                data = scatter(data, [device])[0]

            # Forward the sclerotic vs. non-sclerotic model
            with torch.no_grad():
                scores = bin_model(return_loss=False, **data)
            # Collect the predicted class and the scores
            pred_label = np.argsort(scores, axis=1)[0][::-1]
            pred_class = bin_model.CLASSES[pred_label[0]][3:]
            scores = scores[0]
            mesc_dict['NoSclerotic-prob'].append(scores[0])
            mesc_dict['Sclerotic-prob'].append(scores[1])

            if not args.multi or pred_class == "Sclerotic":
                # Append NaNs for the 12 classes
                mesc_dict['ABMGN-prob'].append(np.nan)
                mesc_dict['ANCA-prob'].append(np.nan)
                mesc_dict['C3-GN-prob'].append(np.nan)
                mesc_dict['CryoglobulinemicGN-prob'].append(np.nan)
                mesc_dict['DDD-prob'].append(np.nan)
                mesc_dict['Fibrillary-prob'].append(np.nan)
                mesc_dict['IAGN-prob'].append(np.nan)
                mesc_dict['IgAGN-prob'].append(np.nan)
                mesc_dict['MPGN-prob'].append(np.nan)
                mesc_dict['Membranous-prob'].append(np.nan)
                mesc_dict['PGNMID-prob'].append(np.nan)
                mesc_dict['SLEGN-IV-prob'].append(np.nan)
            else:
                # Forward the 12 classes model
                with torch.no_grad():
                    scores = mult_model(return_loss=False, **data)
                # Collect the predicted class and the scores
                pred_label = np.argsort(scores, axis=1)[0][::-1]
                # pred_class = mult_model.CLASSES[pred_label[0]][3:]
                topk_labels = pred_label[:args.topk]
                pred_class = [mult_model.CLASSES[l][3:] for l in topk_labels]
                scores = scores[0]
                mesc_dict['ABMGN-prob'].append(scores[0])
                mesc_dict['ANCA-prob'].append(scores[1])
                mesc_dict['C3-GN-prob'].append(scores[2])
                mesc_dict['CryoglobulinemicGN-prob'].append(scores[3])
                mesc_dict['DDD-prob'].append(scores[4])
                mesc_dict['Fibrillary-prob'].append(scores[5])
                mesc_dict['IAGN-prob'].append(scores[6])
                mesc_dict['IgAGN-prob'].append(scores[7])
                mesc_dict['MPGN-prob'].append(scores[8])
                mesc_dict['Membranous-prob'].append(scores[9])
                mesc_dict['PGNMID-prob'].append(scores[10])
                mesc_dict['SLEGN-IV-prob'].append(scores[11])

                pred_class = " | ".join(pred_class)
            
            mesc_dict['filename'].append(image_path)
            mesc_dict['predicted-class'].append(pred_class)

        mesc_df = pd.DataFrame(data=mesc_dict)
        mesc_df.to_csv(output_file_csv, sep=';', index=False)

        # Save each WSI's results
        if args.topk == 1:
            most_predicted_class = mesc_df['predicted-class'].mode().values[0]
            count_most_predicted_class = mesc_df[mesc_df['predicted-class'] == most_predicted_class].shape[0]
        else:
            scores = mesc_df[['ABMGN-prob', 'ANCA-prob', 'C3-GN-prob', 'CryoglobulinemicGN-prob', 'DDD-prob', 'Fibrillary-prob', 'IAGN-prob', 'IgAGN-prob', 'MPGN-prob', 'Membranous-prob', 'PGNMID-prob', 'SLEGN-IV-prob']].values
            no_sclerotic_prob = mesc_df['NoSclerotic-prob'].values.reshape(-1, 1)
            scores = scores * no_sclerotic_prob
            sclerotic_prob = mesc_df['Sclerotic-prob'].values.reshape(-1, 1)
            scores = np.concatenate((sclerotic_prob, scores), axis=1)
            # Replace NaNs with 0
            scores = np.nan_to_num(scores)
            topk_labels = get_most_predicted_topk_classes(scores, args.topk)

            classes = []
            for label in topk_labels:
                if label == 0:
                    classes.append("Sclerotic")
                else:
                    classes.append(mult_model.CLASSES[label - 1][3:])
            most_predicted_class = " | ".join(classes)
            # Get the count of the most predicted class (the first one in the topk)
            top1_most_predicted_class = classes[0]
            top1_predicted_classes = mesc_df['predicted-class'].apply(lambda x: x.split(" | ")[0])
            count_most_predicted_class = mesc_df[top1_predicted_classes == top1_most_predicted_class].shape[0]

        # most_predicted_class = mesc_df['predicted-class'].mode().values[0]
        # count_most_predicted_class = mesc_df[mesc_df['predicted-class'] == most_predicted_class].shape[0]
        total_crops = mesc_df.shape[0]

        wsi_dict['WSI-ID'].append(wsi_id)
        wsi_dict['most-predicted-class'].append(most_predicted_class)
        wsi_dict['ratio-most-predicted-class'].append(f'{count_most_predicted_class} | {total_crops}')


    wsi_df = pd.DataFrame(data=wsi_dict)
    wsi_df.to_csv(output_file_summary_csv, sep=';', index=False)


if __name__ == '__main__':
    main()
