import os
import json
import sys

# ✅ Approved licenses (safe to use)
ALLOWLIST = {
    "MIT", "MIT-0", "Apache-2.0", "BSD-2-Clause", "BSD-3-Clause",
    "Unlicense", "ISC", "UPL-1.0", "CC0-1.0"
}

# ⚠️ Licenses that require caution and review
CAUTION = {
    "LGPL-3.0-only", "LGPL-3.0-or-later", "MPL-2.0"
}

# Allow specific packages to use non-allowlisted licenses
PACKAGE_LICENSE_ALLOWLIST = {
    # Format: "package": ["1.0.0", "2.3.4"], or ["*"] for all versions
    "gover-frontend": ["*"],
    "gover-backend": ["*"],
    "gover-mail-templates": ["*"],
}

# Allow specific packages (optionally with versions) to have no license
IGNORE_NO_LICENSE = {
    # Format: "package": ["1.0.0", "2.3.4"], or ["*"] for all versions
}

# Manual overrides for missing or incorrect license information in the SBOM
KNOWN_LICENSES = {
    "config-chain@1.1.13": "MIT",
}

def is_ignored_missing_license(name, version):
    if name in IGNORE_NO_LICENSE:
        versions = IGNORE_NO_LICENSE[name]
        return "*" in versions or version in versions
    return False

def is_mixed_license(license_str, allowlist, caution):
    cleaned = license_str.replace('(', '').replace(')', '')
    licenses = [l.strip() for l in cleaned.split('OR')]
    allowed = [l for l in licenses if l in allowlist]
    bad = [l for l in licenses if l not in allowlist and l not in caution]
    return allowed and bad

def is_fully_allowed(license_str, allowlist):
    cleaned = license_str.replace('(', '').replace(')', '')
    licenses = [l.strip() for l in cleaned.split('OR')]
    return all(l in allowlist for l in licenses)

def is_allowed_nonstandard_license(name, version):
    if name in PACKAGE_LICENSE_ALLOWLIST:
        versions = PACKAGE_LICENSE_ALLOWLIST[name]
        return "*" in versions or version in versions
    return False

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

def get_known_license(name, version):
    key = f"{name}@{version}"
    return [KNOWN_LICENSES[key]] if key in KNOWN_LICENSES else None

def main():
    try:
        with open("app/public/sbom/sbom.json", "r") as f:
            sbom = json.load(f)
    except Exception as e:
        # Always write a minimal report for downstream steps
        with open("license_report.md", "w") as f:
            f.write("<!-- license-check -->\n")
            f.write("## 🧾 License Compliance Report\n\n")
            f.write(f"❌ Could not read SBOM: {e}\n")
        print(f"❌ Could not read SBOM: {e}")
        sys.exit(1)

    bad = []
    caution = []
    unknown = []

    for comp in sbom.get("components", []):
        name = comp.get("name", "UNKNOWN")
        version = comp.get("version", "")
        purl = comp.get("purl", "")
        licenses = get_known_license(name, version)
        if not licenses:
            licenses = get_license_ids(comp)

        for lic in licenses:
            if lic == "UNKNOWN":
                if is_ignored_missing_license(name, version):
                    continue
                unknown.append((name, version, "UNKNOWN", purl))
            elif is_allowed_nonstandard_license(name, version):
                continue
            elif is_fully_allowed(lic, ALLOWLIST):
                continue
            elif is_mixed_license(lic, ALLOWLIST, CAUTION):
                caution.append((name, version, f"MIXED: {lic}", purl))
            elif lic in ALLOWLIST:
                continue
            elif lic in CAUTION:
                caution.append((name, version, lic, purl))
            else:
                bad.append((name, version, lic, purl))

    with open("license_report.md", "w") as f:
        f.write("<!-- license-check -->\n")
        f.write("## 🧾 License Compliance Report\n\n")
        f.write("This report analyzes the licenses of all dependencies used in this project, based on the Software Bill of Materials (SBOM).\n")
        f.write("It includes both direct and transitive (indirect) dependencies, as all of them are relevant for legal compliance and redistribution. The goal is to ensure that only approved open source licenses are used.\n\n")
        f.write("For more information about allowed licenses and compliance rules, please refer to our [Contribution Guidelines](https://github.com/aivot-digital/.github/blob/main/docs/CONTRIBUTING.md#dependencies-).\n\n")
        f.write("_This comment is automatically updated with every push to ensure up-to-date compliance information._\n\n")

        if caution:
            f.write("### ⚠️ Packages with caution or mixed licenses:\n\n")
            for name, version, lic, purl in caution:
                f.write(f"- `{name}@{version}` → `{lic}` {f' [`{purl}`]' if purl else ''}\n")

        if bad:
            f.write("\n### ❌ Prohibited or unknown licenses:\n\n")
            for name, version, lic, purl in bad:
                f.write(f"- `{name}@{version}` → `{lic}` {f' [`{purl}`]' if purl else ''}\n")

        if unknown:
            f.write("\n### ❓ Components with unknown or missing license info:\n\n")
            for name, version, lic, purl in unknown:
                f.write(f"- `{name}@{version}` → `{lic}` {f' [`{purl}`]' if purl else ''}\n")

        if not caution and not bad and not unknown:
            f.write("\n### ✅ All licenses approved.\n")

        f.write("\n---\n")
        f.write("> **Note:** This report only includes dependencies explicitly declared by the application (e.g. in `package.json`, `pom.xml`, etc.). Dependencies introduced by Docker base images, GitHub Actions, or external CI/CD tools are not included and must be reviewed manually.\n")

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
