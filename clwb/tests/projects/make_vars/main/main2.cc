#include "echo/echo.h"

int main() {
  const char* array[4] = {EXECPATH, ROOTPATH, RLOCATIONPATH, LOCATION};
  return echo(array, 4);
}
