import sys
from spdx_tools.spdx.jsonschema.json_document_parser import parse_json_file
from spdx_tools.spdx.tagvalue.tagvalue_writer import write_document_to_file

# Usage: python convert_spdx_json_to_tag.py input.json output.spdx

input_file = sys.argv[1]
output_file = sys.argv[2]

# Parse SPDX JSON file
document = parse_json_file(input_file)

# Write to tag-value format
with open(output_file, "w") as out:
    write_document_to_file(document, out)
