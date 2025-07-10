import json

build_version = os.getenv("BUILD_VERSION", "dev")

with open("app/public/sbom/sbom.spdx.frontend.json") as f1, open("app/public/sbom/sbom.spdx.backend.json") as f2:
    spdx1 = json.load(f1)
    spdx2 = json.load(f2)

merged = {
    "SPDXID": "SPDXRef-DOCUMENT",
    "spdxVersion": "SPDX-2.3",
    "name": "gover",
    "documentNamespace": f"https://github.com/aivot-digital/gover/spdx/{build_version}",
    "creationInfo": spdx1["creationInfo"],
    "packages": spdx1.get("packages", []) + spdx2.get("packages", []),
    "files": spdx1.get("files", []) + spdx2.get("files", []),
    "relationships": spdx1.get("relationships", []) + spdx2.get("relationships", []),
}

with open("app/public/sbom/sbom.spdx.json", "w") as f:
    json.dump(merged, f, indent=2)
