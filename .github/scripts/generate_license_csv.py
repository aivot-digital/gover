import json
import csv

def get_license_ids(component):
    licenses = component.get("licenses", [])
    return [l.get("license", {}).get("id") for l in licenses if l.get("license")]

with open("app/public/sbom/sbom.json", "r") as f:
    sbom = json.load(f)

with open("app/public/sbom/licenses.csv", "w", newline="") as csvfile:
    writer = csv.writer(csvfile)
    writer.writerow(["Component", "Version", "License(s)"])
    for comp in sbom.get("components", []):
        name = comp.get("name", "UNKNOWN")
        version = comp.get("version", "UNKNOWN")
        licenses = ", ".join(get_license_ids(comp)) or "UNKNOWN"
        writer.writerow([name, version, licenses])
