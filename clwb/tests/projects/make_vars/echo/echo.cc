#include <iostream>
#include <fstream>
#include <filesystem>

#include "echo.h"

int echo(const char** args, int argc) {
   std::ofstream outputFile("output.txt");
   if (!outputFile) {
       return 1;
   }

   for (int i = 0; i < argc; ++i) {
       outputFile << args[i] << std::endl;
   }

    auto fullPath = std::filesystem::current_path() / "output.txt";
    std::cout << "ECHO_OUTPUT_FILE: " << fullPath.string() << std::endl;

    return 0;
}
