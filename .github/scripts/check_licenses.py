import json
import sys

WHITELIST = {"MIT", "Apache-2.0", "BSD-2-Clause", "BSD-3-Clause", "Unlicense"}
CAUTION = {"LGPL-3.0-only", "LGPL-3.0-or-later", "MPL-2.0"}

def get_license_ids(component):
    licenses = component.get("licenses", [])
    return [l.get("license", {}).get("id") for l in licenses if l.get("license")]

def main():
    with open("app/public/sbom.json", "r") as f:
        sbom = json.load(f)

    bad = []
    caution = []

    for comp in sbom.get("components", []):
        for lic in get_license_ids(comp):
            if lic in WHITELIST:
                continue
            elif lic in CAUTION:
                caution.append((comp["name"], lic))
            else:
                bad.append((comp["name"], lic))

    if caution:
        print("⚠️ Caution licenses detected:")
        for name, lic in caution:
            print(f"  - {name}: {lic}")

    if bad:
        print("❌ Prohibited or unknown licenses detected:")
        for name, lic in bad:
            print(f"  - {name}: {lic}")
        sys.exit(1)  # CI-Fail
    else:
        print("✅ All licenses approved.")

if __name__ == "__main__":
    main()
