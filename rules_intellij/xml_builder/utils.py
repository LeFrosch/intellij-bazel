import sys

from google.protobuf import text_format
from xml.dom.minidom import Document, Node, parse as parse_xml


def read_arguments(proto):
  """
  Gets the arguments that are  provided as text proto on the command line.
  """

  return text_format.Parse(sys.argv[1], proto)


def _remove_text_nodes(node: Node):
  """
  Recursively removes all text nodes from a DOM node to remove unnecessary
  whitespace.
  """

  for child in list(node.childNodes):
    if child.nodeType == Node.TEXT_NODE:
      node.removeChild(child)
    if child.nodeType == Node.ELEMENT_NODE:
      _remove_text_nodes(child)


def parse_plugin_xml(path: str) -> Document:
  """
  Parses the given plugin XML file and returns a normalized DOM representation.
  """

  dom = parse_xml(path)
  _remove_text_nodes(dom)
  dom.normalize()

  if dom.documentElement.tagName != 'idea-plugin':
    raise RuntimeError('plugin xml must start with <idea-plugin> tag')

  return dom


def get_or_create_node(dom: Document, name: str) -> Node:
  """
  Gets or creates a child node from the document root node.
  """

  child = dom.documentElement.getElementsByTagName(name)
  if len(child) > 0:
    return child[0]

  child = dom.createElement(name)
  dom.documentElement.appendChild(child)

  return child
