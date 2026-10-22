cmake_minimum_required(VERSION 3.22.1)

project("regyro")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(regyro SHARED regyro.cpp And64InlineHook.cpp)

find_library(log-lib log)

target_compile_options(regyro PRIVATE
    -Os
    -fno-rtti
    -fno-exceptions
    -fvisibility=hidden
    -fvisibility-inlines-hidden
)

target_link_libraries(regyro PRIVATE
    ${log-lib}
)
