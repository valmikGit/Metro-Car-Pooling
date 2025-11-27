"""
Generate complete undirected graph distances CSV (compressed),
AND generate nodes.json listing all node names.

CSV columns: node1,node2,distance
JSON format:
{
    "nodes": ["a1", "a2", ..., "z10"]
}
"""

import csv
import string
import random
import gzip
import json
from tqdm import tqdm

# -----------------------
k = 10                     # nodes per letter -> total = 26 * k
max_distance = 20          # distance sampled uniformly 1..max_distance
random_seed = 42
csv_output_file = "complete_undirected_distances.csv.gz"
json_output_file = "nodes.json"
use_gzip = True
# -----------------------

random.seed(random_seed)

# Generate node names
letters = list(string.ascii_lowercase)
nodes = [f"{L}{i}" for L in letters for i in range(1, k + 1)]
N = len(nodes)

def sample_distance():
    return random.randint(1, max_distance)

# ======================
#   WRITE JSON FILE
# ======================

with open(json_output_file, "w") as jf:
    json.dump({"nodes": nodes}, jf, indent=2)

print(f"✅ JSON node list written to '{json_output_file}'")
print(f"Nodes: {N}")

# ======================
#   WRITE CSV FILE
# ======================

# Choose writer (gzip or normal)
if use_gzip:
    f = gzip.open(csv_output_file, "wt", newline="")
else:
    f = open(csv_output_file, "w", newline="")

with f:
    writer = csv.writer(f)
    writer.writerow(["node1", "node2", "distance"])

    total_pairs = N * (N - 1) // 2

    pbar = tqdm(total=total_pairs + N, desc="Writing CSV pairs")
    for i in range(N):
        ni = nodes[i]
        # Include self-loop (i == j)
        for j in range(i, N):
            nj = nodes[j]
            dist = 0 if i == j else sample_distance()
            writer.writerow([ni, nj, dist])
            pbar.update(1)
    pbar.close()

print(f"✅ Complete graph CSV written to '{csv_output_file}'")
print(f"Total edges (rows): {total_pairs}")
