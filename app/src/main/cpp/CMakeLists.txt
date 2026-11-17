cmake_minimum_required(VERSION 3.22)
project(traubinon_mgba LANGUAGES C CXX)
set(MGBA_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../../../mgba-src")
set(SDL2_SRC "" CACHE PATH "Path to SDL2 source")
set(LIBMGBA_ONLY ON)
add_subdirectory(${MGBA_ROOT} mgba)
if(NOT SDL2_SRC)
  message(FATAL_ERROR "SDL2 source path missing")
endif()
set(SDL_SHARED OFF CACHE BOOL "" FORCE)
set(SDL_STATIC ON CACHE BOOL "" FORCE)
set(SDL_TEST OFF CACHE BOOL "" FORCE)
add_subdirectory(${SDL2_SRC} sdl2)
add_library(main SHARED
  ${MGBA_ROOT}/src/platform/android/android-main.c
  ${CMAKE_CURRENT_SOURCE_DIR}/android/android-ui.c
  ${MGBA_ROOT}/src/platform/sdl/sdl-audio.c
  ${MGBA_ROOT}/src/platform/sdl/sdl-events.c
  ${MGBA_ROOT}/src/platform/sdl/sw-sdl2.c)
get_target_property(MGBA_DEFS mgba COMPILE_DEFINITIONS)
if(MGBA_DEFS)
  target_compile_definitions(main PRIVATE ${MGBA_DEFS})
endif()
target_include_directories(main PRIVATE
  ${MGBA_ROOT}/include ${MGBA_ROOT}/src
  ${MGBA_ROOT}/src/platform/sdl ${CMAKE_CURRENT_SOURCE_DIR}/android
  ${MGBA_ROOT}/src/platform/android ${CMAKE_CURRENT_BINARY_DIR}/mgba/include)
target_link_libraries(main PRIVATE SDL2::SDL2-static mgba log android EGL GLESv1_CM GLESv2 OpenSLES dl)
