import sys

if len(sys.argv) != 4:
    print("Usage: merge_spdx_tag.py <frontend_tag> <backend_tag> <output_tag>")
    sys.exit(1)

frontend_file = sys.argv[1]
backend_file = sys.argv[2]
output_file = sys.argv[3]

with open(frontend_file, 'r') as f:
    frontend_lines = f.readlines()

with open(backend_file, 'r') as f:
    backend_lines = f.readlines()

# Filter out extra document headers (first section of the SPDX doc)
def split_document(lines):
    doc_start = 0
    for i, line in enumerate(lines):
        if line.strip() == "##### Package Information":
            return lines[:i], lines[i:]
    return lines[:], []

frontend_head, frontend_body = split_document(frontend_lines)
backend_head, backend_body = split_document(backend_lines)

# Merge document header from frontend, body from both
with open(output_file, 'w') as f:
    f.writelines(frontend_head)
    f.write("\n##### Combined Package Information #####\n")
    f.writelines(frontend_body)
    f.write("\n")
    f.writelines(backend_body)
