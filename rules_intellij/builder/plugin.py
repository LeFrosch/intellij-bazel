from rules_intellij.builder.utils import *
from rules_intellij.builder.plugin_pb2 import Arguments


def main():
  args = read_arguments(Arguments())
  plugin = args.plugin
  dom = parse_plugin_xml(plugin.plugin_xml)

  # set the package attribute of the root 'idea-plugin' node
  dom.documentElement.setAttribute('package', plugin.package)

  # add plugin id node
  id = create_node(dom, 'id')
  id.appendChild(dom.createTextNode(plugin.id))

  # add plugin name node
  id = create_node(dom, 'name')
  id.appendChild(dom.createTextNode(plugin.name))

  # add vendor node
  vendor = create_node(dom, 'vendor')
  vendor.appendChild(dom.createTextNode(plugin.vendor))

  # add version node
  version = create_node(dom, 'version')
  version.appendChild(dom.createTextNode(plugin.version))

  # add all direct dependencies to the content node
  content = create_node(dom, 'content')

  for dep in plugin.deps:
    node = dom.createElement('module')
    node.setAttribute('name', dep.name)
    node.setAttribute('loading', 'optional' if dep.optional else 'required')

    content.appendChild(node)

  with open(args.output, 'wb') as f:
    f.write(dom.toprettyxml(encoding='utf-8'))


if __name__ == '__main__':
  main()
