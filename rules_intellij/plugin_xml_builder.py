import sys
import os

from google.protobuf import text_format
from rules_intellij.manifest_pb2 import Manifest, File

from xml.dom.minidom import Document, parse as parse_xml

def read_manifest(path: str) -> list[File]:
  with open(path, 'r') as f:
    manifest = text_format.Parse(f.read(), Manifest())

  return manifest.files

def append_file(tree: Document, path: str):
  try:
    file_tree = parse_xml(path)
  except:
    raise RuntimeError('could not parse: %s' % path)

  for node in file_tree.documentElement.childNodes:
    tree.documentElement.appendChild(tree.importNode(node, True))

def plugins_hash(plugins: list[str]) -> int:
  return hash(tuple(sorted(plugins)))

def empty_plugin_xml() -> Document:
  dom = Document()

  root = dom.createElement('idea-plugin')
  dom.appendChild(root)

  return dom

if __name__ == '__main__':
  trees = dict()
  required_plugins = set()

  for file in read_manifest(sys.argv[1]):
    dom = trees.setdefault(plugins_hash(file.plugins), empty_plugin_xml())
    append_file(dom, file.path)

  with open(sys.argv[2], 'wb') as f:
    for tree in trees.values():
      f.write(dom.toxml(encoding='utf-8'))