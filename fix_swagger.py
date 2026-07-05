import os
import glob

for filepath in glob.glob('/home/ndbau/projects/yas/**/SwaggerConfig.java', recursive=True):
    with open(filepath, 'r') as f:
        content = f.read()
    new_content = content.replace('url = "${server.servlet.context-path}"', 'url = "/api${server.servlet.context-path}"')
    if new_content != content:
        with open(filepath, 'w') as f:
            f.write(new_content)
        print(f"Updated {filepath}")
