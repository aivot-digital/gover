import json
import sys

# ✅ Approved licenses (safe to use)
ALLOWLIST = {
    "MIT",
    "MIT-0",
    "Apache-2.0",
    "BSD-2-Clause",
    "BSD-3-Clause",
    "Unlicense",
    "ISC",
    "UPL-1.0",
    "CC0-1.0"
}

# ⚠️ Licenses that require caution and review
CAUTION = {
    "LGPL-3.0-only",
    "LGPL-3.0-or-later",
    "MPL-2.0"
}

def get_license_ids(component):
    licenses = component.get("licenses", [])
    if not licenses:
        return ["UNKNOWN"]

    ids = []
    for entry in licenses:
        if "license" in entry and "id" in entry["license"]:
            ids.append(entry["license"]["id"])
        elif "expression" in entry:
            ids.append(entry["expression"])
        elif "license" in entry and "name" in entry["license"]:
            ids.append(entry["license"]["name"])
    return ids if ids else ["UNKNOWN"]

def main():
    try:
        with open("app/public/sbom/sbom.cyclonedx.json", "r") as f:
            sbom = json.load(f)
    except Exception as e:
        print(f"❌ Could not read SBOM: {e}")
        sys.exit(1)

    bad = []
    caution = []
    unknown = []

    for comp in sbom.get("components", []):
        name = comp.get("name", "UNKNOWN")
        version = comp.get("version", "")
        licenses = get_license_ids(comp)

        for lic in licenses:
            if lic == "UNKNOWN":
                unknown.append(f"{name}@{version}")
            elif lic in ALLOWLIST:
                continue
            elif lic in CAUTION:
                caution.append((name, version, lic))
            else:
                bad.append((name, version, lic))

    with open("license_report.md", "w") as f:
        f.write("<!-- license-check -->\n")
        f.write("## 🧾 License Compliance Report\n\n")
        f.write("This report analyzes the licenses of all dependencies used in this project, based on the Software Bill of Materials (SBOM).\n")
        f.write("It includes both direct and transitive (indirect) dependencies, as all of them are relevant for legal compliance and redistribution. The goal is to ensure that only approved open source licenses are used.\n\n")
        f.write("For more information about allowed licenses and compliance rules, please refer to our [Contribution Guidelines](https://github.com/aivot-digital/.github/blob/main/docs/CONTRIBUTING.md#dependencies-).\n\n")
        f.write("_This comment is automatically updated with every push to ensure up-to-date compliance information._\n\n")

        if caution:
            f.write("\n### ⚠️ *Packages with caution licenses:*\n\n")
            for name, version, lic in caution:
                f.write(f"- `{name}@{version}` → `{lic}`\n")

        if bad:
            f.write("\n### ❌ *Prohibited or unknown licenses:*\n\n")
            for name, version, lic in bad:
                f.write(f"- `{name}@{version}` → `{lic}`\n")

        if unknown:
            f.write("\n### ❓ *Components with unknown or missing license info:*\n\n")
            for entry in unknown:
                f.write(f"- `{entry}` → `UNKNOWN`\n")

        if not caution and not bad and not unknown:
            f.write("\n### ✅ All licenses approved.\n")

    if bad:
        print("❌ Compliance check failed due to forbidden licenses.")
        sys.exit(1)
    elif unknown:
        print("⚠️ Compliance check completed with unknown licenses.")
        sys.exit(0)
    else:
        print("✅ License compliance check passed.")
        sys.exit(0)

if __name__ == "__main__":
    main()
