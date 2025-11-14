#!/usr/bin/env python3
import gzip, csv, json, redis, os

# --- Redis connection ---
REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = int(os.getenv("REDIS_PORT", 6379))
r = redis.StrictRedis(host=REDIS_HOST, port=REDIS_PORT, decode_responses=True)

# --- Input files ---
DIST_FILE = "/data/complete_undirected_distances.csv.gz"
NEARBY_FILE = "/data/location_nearby.csv"

print("Connecting to Redis at", REDIS_HOST, REDIS_PORT)

# --- 1. Load location-location map ---
print("Loading distance map...")
dist_map = {}
with gzip.open(DIST_FILE, "rt", newline="") as f:
    reader = csv.DictReader(f)
    for row in reader:
        a, b = row["node1"], row["node2"]
        d = float(row["distance"])
        dist_map.setdefault(a, {})[b] = d
        dist_map.setdefault(b, {})[a] = d  # symmetric for undirected graph

r.set("location-location-map", json.dumps(dist_map))
print(f"✅ Stored 'location-location-map' with {len(dist_map)} nodes.")

# --- 2. Load nearby station map ---
print("Loading nearby station map...")
nearby_map = {}
with open(NEARBY_FILE, newline="") as f:
    reader = csv.DictReader(f)
    for row in reader:
        city = row["city_point"]
        station = row["near_metro_dropoff"]
        if station.strip():
            nearby_map[city] = station.strip()

r.set("nearby-stations", json.dumps(nearby_map))
print(f"✅ Stored 'nearby-stations' with {len(nearby_map)} entries.")

print("🎯 Redis cache initialization complete.")
