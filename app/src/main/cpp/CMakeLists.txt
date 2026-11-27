cmake_minimum_required(VERSION 3.22.1)

project("library_native")

add_library(
        library_native
        SHARED
        library_native.c
)

find_library(
        log-lib
        log
)

target_link_libraries(
        library_native
        ${log-lib}
)