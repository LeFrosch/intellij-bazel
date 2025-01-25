from rules_intellij.xml_builder.utils import *
from rules_intellij.xml_builder.plugin_pb2 import Arguments


def main():
  args = read_arguments(Arguments())
  plugin = args.plugin

  # parse the existing plugin xml file
  dom = parse_plugin_xml(plugin.plugin_xml)

  # set the package attribute of the root 'idea-plugin' node
  dom.documentElement.setAttribute('package', plugin.package)

  # ensure that the content node was not added manually
  nodes = dom.documentElement.getElementsByTagName('content')
  if len(nodes) > 0:
    raise RuntimeError('plugin xml must not contain a <content> node')

  # add all direct dependencies to the content node
  content = dom.createElement('content')
  dom.documentElement.appendChild(content)

  for dep in plugin.deps:
    node = dom.createElement('module')
    node.setAttribute('name', dep.name)
    node.setAttribute('loading', 'optional' if dep.optional else 'required')

    content.appendChild(node)

  with open(args.output, 'wb') as f:
    f.write(dom.toprettyxml(encoding='utf-8'))


if __name__ == '__main__':
  main()
