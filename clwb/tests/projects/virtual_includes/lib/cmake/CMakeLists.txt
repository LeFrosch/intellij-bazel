cmake_minimum_required(VERSION 3.5.0)

add_library(cmake STATIC cmake_lib.cc)

install(TARGETS cmake ARCHIVE DESTINATION lib)
install(FILES cmake_lib.h DESTINATION include)
