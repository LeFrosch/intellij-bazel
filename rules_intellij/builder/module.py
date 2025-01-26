from rules_intellij.builder.utils import *
from rules_intellij.builder.module_pb2 import Arguments

def main():
  args = read_arguments(Arguments())
  module = args.module
  dom = parse_plugin_xml(module.plugin_xml)

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
