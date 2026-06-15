# Set the minimum required version of CMake
cmake_minimum_required(VERSION 3.22.1)

# Set standards for C and C++
set(CMAKE_C_STANDARD 99)
set(CMAKE_CXX_STANDARD 11)

if(APP_LIB_NAME)

    project("${APP_LIB_NAME}")

    # Include raylib and raymob as a subdirectories
    add_subdirectory(${CMAKE_SOURCE_DIR}/deps/raylib)
    add_subdirectory(${CMAKE_SOURCE_DIR}/deps/raymob)

    # Fetch all source files for your project (recursively), excluding 'deps' source files
    file(GLOB_RECURSE SOURCES "${CMAKE_SOURCE_DIR}/src/*.c" "${CMAKE_SOURCE_DIR}/src/*.cpp")
    list(FILTER SOURCES EXCLUDE REGEX "${CMAKE_SOURCE_DIR}/deps/.*")

    # Ensure ANDROID_NDK is defined for include path
    if(NOT ANDROID_NDK AND CMAKE_ANDROID_NDK)
        set(ANDROID_NDK ${CMAKE_ANDROID_NDK})
    endif()

    # Add headers directory for android_native_app_glue.c
    include_directories(${ANDROID_NDK}/sources/android/native_app_glue/)

    # Add android_native_app_glue.c to the source files
    list(APPEND SOURCES ${ANDROID_NDK}/sources/android/native_app_glue/android_native_app_glue.c)

    # Create a shared library with game source files
    add_library(${APP_LIB_NAME} SHARED ${SOURCES})

    # Define compiler macros for the library
    target_compile_definitions(${APP_LIB_NAME} PRIVATE PLATFORM_ANDROID)

    # Apply flags depending on the build type
    if(CMAKE_BUILD_TYPE MATCHES "Debug")
        target_compile_definitions(${APP_LIB_NAME} PRIVATE _DEBUG DEBUG)
    endif()

    # Include raylib and raymob header files
    target_include_directories(${APP_LIB_NAME} PRIVATE "${CMAKE_SOURCE_DIR}/deps/raylib")
    target_include_directories(${APP_LIB_NAME} PRIVATE "${CMAKE_SOURCE_DIR}/deps/raymob")

    # Link required libraries to the native application
    target_link_libraries(${APP_LIB_NAME} raylib raymoblib)

else()

    project(repspectre_pc C)
    set(BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
    add_subdirectory(deps/raylib)
    add_executable(app src/main.c)
    target_compile_definitions(app PRIVATE PLATFORM_DESKTOP)
    target_include_directories(app PRIVATE deps/raylib/src deps/raymob)

    target_link_libraries(app raylib m pthread dl)
    if(UNIX AND NOT APPLE)
        target_link_libraries(app GL X11)
    endif()
endif()
