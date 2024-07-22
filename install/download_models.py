"""Script to download all models from Hugging Face Hub."""
#!/usr/bin/env python
import os

from huggingface_hub import snapshot_download

ROOT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def download_repository(output_dir):
    return snapshot_download(
        repo_id="israelMateos/GNCnn",
        token="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",
        local_dir=output_dir,
        local_dir_use_symlinks=False,
        force_download=True,
    )


def main():
    download_repository(os.path.join(ROOT_DIR, "models"))


if __name__ == "__main__":
    main()
