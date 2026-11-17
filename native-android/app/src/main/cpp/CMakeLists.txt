cmake_minimum_required(VERSION 3.22.1)
project(lotd_engine_android C)
set(CMAKE_C_STANDARD 11)
set(SDL_SHARED ON CACHE BOOL "Build shared SDL2" FORCE)
set(SDL_STATIC OFF CACHE BOOL "Do not build static SDL2" FORCE)
set(SDL_TEST OFF CACHE BOOL "Do not build SDL2 tests" FORCE)
set(LOTD_ROOT "E:/LOTDAndroid/lotd-webport")
set(SDL_ROOT "E:/LOTDAndroid/SDL2-2.32.10")
if(NOT EXISTS "${LOTD_ROOT}/gen/modules.c")
    message(FATAL_ERROR "LOTD generated files missing")
endif()
set(SDL_HIDAPI OFF CACHE BOOL "Disable HIDAPI C++ code for Stage 5 offline Android test" FORCE)
add_subdirectory("${SDL_ROOT}" "${CMAKE_BINARY_DIR}/sdl-build" EXCLUDE_FROM_ALL)
file(GLOB_RECURSE LOTD_REC CONFIGURE_DEPENDS "${LOTD_ROOT}/gen/*/rec_*.c")
file(GLOB LOTD_HOST CONFIGURE_DEPENDS "${LOTD_ROOT}/src/*.c")
list(REMOVE_ITEM LOTD_HOST "${LOTD_ROOT}/src/main.c" "${LOTD_ROOT}/src/wsock.c")
add_library(main SHARED
    android_entry.c
    wsock_android_offline.c
    "${LOTD_ROOT}/src/main.c"
    "${LOTD_ROOT}/gen/modules.c"
    "${LOTD_ROOT}/gen/import_hle.c"
    ${LOTD_HOST}
    ${LOTD_REC}
)
set_source_files_properties("${LOTD_ROOT}/src/main.c" PROPERTIES COMPILE_DEFINITIONS "main=lotd_host_main")
target_compile_definitions(main PRIVATE SDL_MAIN_HANDLED)
target_include_directories(main PRIVATE "${LOTD_ROOT}/src" "${SDL_ROOT}/include")
target_compile_options(main PRIVATE -O1 -fno-strict-aliasing -w -fno-omit-frame-pointer)
target_link_libraries(main PRIVATE SDL2::SDL2 GLESv3 EGL android log dl m z)
# Catch undefined symbols now instead of crashing later:
target_link_options(main PRIVATE -Wl,--no-undefined)
