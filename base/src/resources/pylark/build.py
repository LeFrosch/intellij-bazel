from .default import *
from .native import *

### builtin rules ###

def alias(
  name,
  actual,
  compatible_with=[],
  deprecation=None,
  features=[],
  restricted_to=[],
  tags=[],
  target_compatible_with=[],
  testonly=False,
  visibility=[]): ...

def config_setting(
  name,
  values={},
  constraint_values=[],
  define_values={},
  deprecation=None,
  distribs=[],
  features=[],
  flag_values={},
  licenses=["none"],
  tags=[],
  testonly=False,
  visibility=[]): ...