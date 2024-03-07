import torch


def get_proper_device():
    if torch.cuda.is_available():
        return torch.device("cuda")
    elif torch.has_mps:
        return torch.device("mps")
    else:
        torch.device("cpu")
