import json
import csv
import os

def get_license_string(component):
    licenses = component.get("licenses", [])
    if not licenses:
        return "UNKNOWN"

    license_ids = []
    for entry in licenses:
        if "license" in entry and "id" in entry["license"]:
            license_ids.append(entry["license"]["id"])
        elif "expression" in entry:
            license_ids.append(entry["expression"])
        elif "license" in entry and "name" in entry["license"]:
            license_ids.append(entry["license"]["name"])
    return " OR ".join(license_ids) if license_ids else "UNKNOWN"

sbom_path = "app/public/sbom/sbom.json"
output_dir = os.path.dirname(sbom_path)

with open(sbom_path, "r") as f:
    sbom = json.load(f)

csv_path = os.path.join(output_dir, "licenses.csv")
with open(csv_path, "w", newline="") as csvfile:
    writer = csv.writer(csvfile)
    writer.writerow(["Name", "Version", "License(s)", "PURL"])
    for comp in sbom.get("components", []):
        name = comp.get("name", "UNKNOWN")
        version = comp.get("version", "UNKNOWN")
        license_str = get_license_string(comp)
        purl = comp.get("purl", "N/A")
        writer.writerow([name, version, license_str, purl])

txt_path = os.path.join(output_dir, "licenses.txt")
with open(txt_path, "w") as f:
    for comp in sbom.get("components", []):
        name = comp.get("name", "UNKNOWN")
        version = comp.get("version", "UNKNOWN")
        license_str = get_license_string(comp)
        purl = comp.get("purl")
        f.write(f"{name}@{version} → {license_str}\n")
        if purl:
            f.write(f"  ↳ {purl}\n")
