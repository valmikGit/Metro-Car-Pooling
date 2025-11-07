import csv
import random
import string

# ==============================
# CONFIGURABLE PARAMETERS
# ==============================
k = 100   # Number of locations per letter (A1...Ak)
m = 40    # Number of nearby points (Me1...Mem)
output_file = "location_nearby.csv"  # Output CSV file
random_seed = 42                     # For reproducibility
# ==============================

random.seed(random_seed)

# Generate location set: A1...Ak, B1...Bk, ..., Z1...Zk
letters = list(string.ascii_uppercase)  # ['A', 'B', ..., 'Z']
locations = [f"{L}{i}" for L in letters for i in range(1, k + 1)]

# Generate nearby set: Me1...Mem
nearby_points = [f"Me{i}" for i in range(1, m + 1)]

N = len(locations)
print(f"Total locations: {N}")

# Choose 50% of locations to have a nearby point
num_with_nearby = N // 2
locations_with_nearby = set(random.sample(locations, num_with_nearby))

# Prepare CSV rows
rows = []
for loc in locations:
    if loc in locations_with_nearby:
        nearby = random.choice(nearby_points)
    else:
        nearby = ""  # 50% remain blank
    rows.append((loc, nearby))

# Write to CSV
with open(output_file, mode="w", newline="") as f:
    writer = csv.writer(f)
    writer.writerow(["location", "nearby"])
    writer.writerows(rows)

print(f"✅ CSV '{output_file}' generated successfully.")
print(f"→ {num_with_nearby} locations mapped to nearby points, {N - num_with_nearby} left blank.")
