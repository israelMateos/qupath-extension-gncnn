from huggingface_hub import hf_hub_download


def download_detector(model_name, config_dir, local_dir):
    return hf_hub_download(
        repo_id="MESCnn/MESCnn",
        filename=f"detection/logs/{model_name}/{config_dir}/output/model_final.pth",
        token="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",
        local_dir=local_dir,
        local_dir_use_symlinks=False,
        force_download=True,
    )
