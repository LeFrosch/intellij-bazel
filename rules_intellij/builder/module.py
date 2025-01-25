from rules_intellij.builder.utils import *
from rules_intellij.builder.module_pb2 import Arguments

from xml.dom.minidom import Document

def empty_module_xml() -> Document:
  """
  Creates an XML document with an empty root for an 'idea-plugin'
  """

  dom = Document()

  root = dom.createElement('idea-plugin')
  dom.appendChild(root)

  return dom


def main():
  args = read_arguments(Arguments())
  module = args.module

  # parse the existing xml file or create an empty one
  if module.plugin_xml:
    dom = parse_plugin_xml(module.plugin_xml)
  else:
    dom = empty_module_xml()

  # set the package attribute of the root 'idea-plugin' node
  dom.documentElement.setAttribute('package', module.package)

  # add all direct dependencies to the dependencies node
  dependencies = get_or_create_node(dom, 'dependencies')

  for dep in module.deps:
    node = dom.createElement('module')
    node.setAttribute('name', dep.name)

    # at the moment, all module dependencies are optional
    # node.setAttribute('loading', 'optional' if dep.optional else 'required')

    dependencies.appendChild(node)

  # write the module xml file
  with open(args.output, 'wb') as f:
    f.write(dom.toprettyxml(encoding='utf-8'))


if __name__ == '__main__':
  main()
