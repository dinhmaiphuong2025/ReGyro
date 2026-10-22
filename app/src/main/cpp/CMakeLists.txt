cmake_minimum_required(VERSION 3.22.1)

project("regyro")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(FetchContent)
FetchContent_Declare(
    dobby
    GIT_REPOSITORY https://github.com/jmpews/Dobby.git
    GIT_TAG 5dfc8546954ce3b3198132ab13fddb89ee92cdd7
    GIT_SHALLOW TRUE
)
set(DOBBY_BUILD_TEST OFF CACHE BOOL "" FORCE)
set(DOBBY_BUILD_EXAMPLE OFF CACHE BOOL "" FORCE)
set(DOBBY_GENERATE_SHARED OFF CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(dobby)

add_library(regyro SHARED regyro.cpp)

find_library(log-lib log)

target_include_directories(regyro PRIVATE
    ${dobby_SOURCE_DIR}/include
)

target_compile_options(regyro PRIVATE
    -Os
    -fno-rtti
    -fno-exceptions
    -fvisibility=hidden
    -fvisibility-inlines-hidden
)

target_link_libraries(regyro PRIVATE
    dobby_static
    ${log-lib}
)
