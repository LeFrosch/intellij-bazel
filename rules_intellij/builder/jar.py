from rules_intellij.builder.utils import *
from rules_intellij.builder.jar_pb2 import Arguments

from zipfile import ZipFile, ZipInfo

import os
import shutil

# set to Jan 1 1980, the earliest date supported by zipfile
ZIP_DATE = (1980, 1, 1, 0, 0, 0)


def copy_file(src: str, dst: str, zip: ZipFile):
  """
  Copies the src file to the dst inside the zip file. The date is set to Jan 1
  1980 to ensure all zip files are the same.
  """

  with open(src, 'rb') as f:
    zip_info = ZipInfo(dst, ZIP_DATE)
    zip.writestr(zip_info, f.read())


def main():
  args = read_arguments(Arguments())
  plugin = args.plugin

  # copy the input zip file to the output location
  shutil.copy(plugin.impl_jar, args.output)
  os.chmod(args.output, 0o666)

  with ZipFile(args.output, 'a') as f:

    # copy all module xml files into the root of the jar
    for module_xml in plugin.module_xmls:
      copy_file(module_xml, os.path.basename(module_xml), f)

    # copy the plugin xml file into the META-INF directory
    copy_file(plugin.plugin_xml, 'META-INF/plugin.xml', f)

if __name__ == '__main__':
  main()
