import json
import sys

# ✅ Approved licenses (safe to use)
WHITELIST = {
    "MIT",
    "Apache-2.0",
    "BSD-2-Clause",
    "BSD-3-Clause",
    "Unlicense"
}

# ⚠️ Licenses that require caution and review
CAUTION = {
    "LGPL-3.0-only",
    "LGPL-3.0-or-later",
    "MPL-2.0"
}

def get_license_ids(component):
    """
    Extract SPDX license IDs from a component's license block.
    """
    licenses = component.get("licenses", [])
    return [l.get("license", {}).get("id") for l in licenses if l.get("license")]

def main():
    # Try to load the SBOM JSON file
    try:
        with open("app/public/sbom.json", "r") as f:
            sbom = json.load(f)
    except Exception as e:
        print(f"❌ Could not read SBOM: {e}")
        sys.exit(1)

    bad = []      # ❌ Forbidden or unknown licenses
    caution = []  # ⚠️ Licenses requiring manual review

    # Iterate over all components in the SBOM
    for comp in sbom.get("components", []):
        name = comp.get("name", "UNKNOWN")
        for lic in get_license_ids(comp):
            if not lic:
                continue
            if lic in WHITELIST:
                continue
            elif lic in CAUTION:
                caution.append((name, lic))
            else:
                bad.append((name, lic))

    # Write results to a markdown file for GitHub PR comments
    with open("license_report.md", "w") as f:
        f.write("<!-- license-check -->\n")
        f.write("## 🧾 License Compliance Report\n\n")
        f.write("This report analyzes the licenses of all dependencies used in this project, based on the Software Bill of Materials (SBOM).\n")
        f.write("It includes both direct and transitive (indirect) dependencies, as all of them are relevant for legal compliance and redistribution.\n")
        f.write("The goal is to ensure that only approved open source licenses are used, in accordance with our contribution guidelines.\n\n")
        f.write("For more information about allowed licenses and compliance rules, please refer to our [Contribution Guidelines](https://github.com/aivot-digital/.github/blob/main/docs/CONTRIBUTING.md#dependencies-).\n\n")
        f.write("_This comment is automatically updated with every push to ensure up-to-date compliance information._\n\n")

        if caution:
            f.write("\n### ⚠️ *Packages with caution licenses:*\n\n")
            for name, lic in caution:
                f.write(f"- `{name}` → `{lic}`\n")

        if bad:
            f.write("\n### ❌ *Prohibited or unknown licenses:*\n\n")
            for name, lic in bad:
                f.write(f"- `{name}` → `{lic}`\n")

        if not caution and not bad:
            f.write("\n### ✅ All licenses approved.\n")

    # Fail the CI step only if forbidden licenses are detected
    if bad:
        sys.exit(1)
    else:
        sys.exit(0)

if __name__ == "__main__":
    main()
