import os
import json
import sys

def load_policy():
    with open(".github/scripts/data/license_policy.json", "r", encoding="utf-8") as f:
        raw = json.load(f)

        def extract_versions(obj):
            return {k: v["versions"] for k, v in obj.items()}

        def extract_known_licenses(obj):
            return {k: v["license"] for k, v in obj.items()}

        return {
            "approved": set(raw.get("approved", [])),
            "restricted": set(raw.get("restricted", [])),
            "prohibited": set(raw.get("prohibited", [])),
            "packageLicenseOverrides": extract_versions(raw.get("packageLicenseOverrides", {})),
            "ignoredNoLicense": extract_versions(raw.get("ignoredNoLicense", {})),
            "knownLicenses": extract_known_licenses(raw.get("knownLicenses", {})),
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

def get_known_license(name, version, known_licenses):
    key = f"{name}@{version}"
    return [known_licenses[key]] if key in known_licenses else None

def is_ignored_missing_license(name, version, ignored):
    if name in ignored:
        versions = ignored[name]
        return "*" in versions or version in versions
    return False

def is_allowed_nonstandard_license(name, version, overrides):
    if name in overrides:
        versions = overrides[name]
        return "*" in versions or version in versions
    return False

def is_fully_approved(license_str, approved):
    cleaned = license_str.replace('(', '').replace(')', '')
    licenses = [l.strip() for l in cleaned.split('OR')]
    return all(l in approved for l in licenses)

def is_mixed_license(license_str, approved, restricted):
    cleaned = license_str.replace('(', '').replace(')', '')
    licenses = [l.strip() for l in cleaned.split('OR')]
    allowed = [l for l in licenses if l in approved]
    bad = [l for l in licenses if l not in approved and l not in restricted]
    return allowed and bad

def main():
    try:
        with open("app/public/sbom/sbom.json", "r", encoding="utf-8") as f:
            sbom = json.load(f)
    except Exception as e:
        with open("license_report.md", "w") as f:
            f.write("<!-- license-check -->\n")
            f.write("## 🧾 License Compliance Report\n\n")
            f.write(f"❌ Could not read SBOM: {e}\n")
        print(f"❌ Could not read SBOM: {e}")
        sys.exit(1)

    try:
        policy = load_policy()
    except Exception as e:
        print(f"❌ Failed to load license policy: {e}")
        sys.exit(1)

    approved = policy["approved"]
    restricted = policy["restricted"]
    prohibited = policy["prohibited"]
    known_licenses = policy["knownLicenses"]
    ignored = policy["ignoredNoLicense"]
    overrides = policy["packageLicenseOverrides"]

    caution = []
    denied = []
    unknown = []

    for comp in sbom.get("components", []):
        name = comp.get("name", "UNKNOWN")
        version = comp.get("version", "")
        purl = comp.get("purl", "")
        licenses = get_known_license(name, version, known_licenses)
        if not licenses:
            licenses = get_license_ids(comp)

        for lic in licenses:
            if lic == "UNKNOWN":
                if is_ignored_missing_license(name, version, ignored):
                    continue
                unknown.append((name, version, lic, purl))
            elif is_allowed_nonstandard_license(name, version, overrides):
                continue
            elif is_fully_approved(lic, approved):
                continue
            elif is_mixed_license(lic, approved, restricted):
                caution.append((name, version, f"MIXED: {lic}", purl))
            elif lic in approved:
                continue
            elif lic in restricted:
                caution.append((name, version, lic, purl))
            elif lic in prohibited:
                denied.append((name, version, lic, purl))
            else:
                unknown.append((name, version, lic, purl))

    with open("license_report.md", "w", encoding="utf-8") as f:
        f.write("<!-- license-check -->\n")
        f.write("## 🧾 License Compliance Report\n\n")
        f.write("This report analyzes the licenses of all dependencies used in this project, based on the Software Bill of Materials (SBOM).\n")
        f.write("It includes both direct and transitive dependencies. The goal is to ensure that only approved open source licenses are used.\n\n")
        f.write("For more information, see the [Contribution Guidelines](https://github.com/aivot-digital/.github/blob/main/docs/CONTRIBUTING.md#dependencies-). This comment is automatically updated with every push.\n\n")

        if caution:
            f.write("### ⚠️ Packages with caution or mixed licenses:\n\n")
            f.write("The following dependencies use licenses that are either partially approved or ambiguous. Each license must be reviewed individually by a project administrator to determine its acceptability.\n")
            for name, version, lic, purl in caution:
                f.write(f"- `{name}@{version}` → `{lic}` {f'[`{purl}`]' if purl else ''}\n")

        if denied:
            f.write("\n### ❌ Prohibited licenses:\n\n")
            f.write("The following dependencies use licenses that are not allowed under any circumstances. These components **must not be used** in this project. Please remove and replace them as soon as possible.\n\n")
            for name, version, lic, purl in denied:
                f.write(f"- `{name}@{version}` → `{lic}` {f'[`{purl}`]' if purl else ''}\n")

        if unknown:
            f.write("\n### ❓ Unknown or unverified licenses:\n\n")
            f.write("The following dependencies have unknown or unverified licenses. Please contact a project administrators to review and validate these components.\n\n")
            for name, version, lic, purl in unknown:
                f.write(f"- `{name}@{version}` → `{lic}` {f'[`{purl}`]' if purl else ''}\n")

        if not caution and not denied and not unknown:
            f.write("\n### ✅ All licenses approved.\n")

        f.write("\n\n> **Note:** This report only includes dependencies explicitly declared by the application (e.g. in `package.json`, `pom.xml`, etc.). Dependencies introduced by Docker base images, GitHub Actions, or external CI/CD tools are not included and must be reviewed manually.\n")

    if denied:
        print("❌ Compliance check failed due to prohibited licenses.")
        sys.exit(1)
    elif unknown:
        print("⚠️ Compliance check completed with unknown licenses.")
        sys.exit(0)
    elif caution:
        print("⚠️ Compliance check completed with caution/mixed licenses.")
        sys.exit(0)
    else:
        print("✅ License compliance check passed.")
        sys.exit(0)

if __name__ == "__main__":
    main()
