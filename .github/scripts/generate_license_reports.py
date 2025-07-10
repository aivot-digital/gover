import json
import csv
import os

# Helper to extract license IDs safely
def get_license_ids(component):
    licenses = component.get("licenses", [])
    ids = []
    for entry in licenses:
        license = entry.get("license", {})
        license_id = license.get("id")
        if isinstance(license_id, str):
            ids.append(license_id)
    return ids or ["UNKNOWN"]

# Path to SBOM input
sbom_path = "app/public/sbom/sbom.cyclonedx.json"
output_dir = os.path.dirname(sbom_path)

# Load SBOM JSON
with open(sbom_path, "r") as f:
    sbom = json.load(f)

# CSV output
csv_path = os.path.join(output_dir, "licenses.csv")
with open(csv_path, "w", newline="") as csvfile:
    writer = csv.writer(csvfile)
    writer.writerow(["Name", "Version", "License(s)", "PURL"])
    for comp in sbom.get("components", []):
        name = comp.get("name", "UNKNOWN")
        version = comp.get("version", "UNKNOWN")
        licenses = ", ".join(get_license_ids(comp))
        purl = comp.get("purl", "N/A")
        writer.writerow([name, version, licenses, purl])

# TXT output
txt_path = os.path.join(output_dir, "licenses.txt")
with open(txt_path, "w") as f:
    for comp in sbom.get("components", []):
        name = comp.get("name", "UNKNOWN")
        version = comp.get("version", "UNKNOWN")
        licenses = ", ".join(get_license_ids(comp))
        purl = comp.get("purl")
        f.write(f"{name}@{version} → {licenses}\n")
        if purl:
            f.write(f"  ↳ {purl}\n")

