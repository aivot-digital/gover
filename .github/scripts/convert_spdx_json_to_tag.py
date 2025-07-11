import os
import sys
from spdx_tools.spdx.jsonschema.parser import parse_file
from spdx_tools.spdx.tagvalue.writer import write_document_to_file

# Eingabe- und Ausgabepfade
input_file = sys.argv[1]
output_file = sys.argv[2]

# JSON einlesen
doc = parse_file(input_file)

# In Tag-Format schreiben
write_document_to_file(doc, output_file)
