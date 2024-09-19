from .default import *
from . import native

def rule(
  implementation,
  *,
  test=None,
  attrs={},
  outputs=None,
  executable=unbound,
  output_to_genfiles=False,
  fragments=[],
  host_fragments=[],
  _skylark_testable=False,
  toolchains=[],
  incompatible_use_toolchain_transition=False,
  doc=None,
  provides=[],
  exec_compatible_with=[],
  analysis_test=False,
  build_setting=None,
  cfg=None,
  exec_groups=None,
  initializer=None,
  parent=None,
  extendable=None,
  subrules=[],
): ...
