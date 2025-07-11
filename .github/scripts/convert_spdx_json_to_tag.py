import os
import sys
from spdx.parsers.jsonparser import parse as parse_json
from spdx.writers.tagvalue import write_document

# Input and output paths
input_path = sys.argv[1] if len(sys.argv) > 1 else "sbom.spdx.json"
output_path = sys.argv[2] if len(sys.argv) > 2 else "sbom.spdx.tag"

# Parse SPDX JSON document
with open(input_path, "r", encoding="utf-8") as f:
    document, _ = parse_json(f)

# Write Tag/Value format
with open(output_path, "w", encoding="utf-8") as f:
    write_document(document, f)
