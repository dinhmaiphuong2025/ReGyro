cmake_minimum_required(VERSION 3.22.1)

project("regyro")

add_library(regyro SHARED regyro.cpp)

find_library(log-lib log)

target_compile_options(regyro PRIVATE -Os -fno-rtti -fno-exceptions)
target_link_libraries(regyro ${log-lib})
