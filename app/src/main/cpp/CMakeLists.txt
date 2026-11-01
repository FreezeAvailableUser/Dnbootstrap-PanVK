# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name. The project name can be accessed via ${ PROJECT_NAME},
# Since this is the top level CMakeLists.txt, the project name is also accessible
# with ${CMAKE_PROJECT_NAME} (both CMake variables are in-sync within the top level
# build script scope).
project("dnbootstrap")

####################################################
# GLFW
####################################################
set(GLFW_BUILD_EXAMPLES OFF CACHE STRING "" FORCE)
set(GLFW_BUILD_TESTS    OFF CACHE STRING "" FORCE)
set(GLFW_INSTALL        OFF CACHE STRING "" FORCE)
set(BUILD_SHARED_LIBS   ON  CACHE STRING "" FORCE)
set(GLFW_BUILD_X11      OFF CACHE STRING "" FORCE)
set(GLFW_BUILD_WAYLAND  OFF CACHE STRING "" FORCE)
set(GLFW_BUILD_ANDROID  ON  CACHE STRING "" FORCE)

add_subdirectory(glfw34)
include_directories(glfw34/include src/)

####################################################
# nethost
####################################################

add_library(nethost SHARED IMPORTED)
set_target_properties(nethost PROPERTIES
        IMPORTED_LOCATION "${CMAKE_CURRENT_SOURCE_DIR}/nethost/libnethost.so"
)
target_include_directories(nethost INTERFACE
        "${CMAKE_CURRENT_SOURCE_DIR}/nethost/include"
)

set(CMAKE_SHARED_LINKER_FLAGS
        "${CMAKE_SHARED_LINKER_FLAGS} -u ANativeActivity_onCreate")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
#
# In this top level CMakeLists.txt, ${CMAKE_PROJECT_NAME} is used to define
# the target library name; in the sub-module's CMakeLists.txt, ${PROJECT_NAME}
# is preferred for the same purpose.
#
# In order to load a library into your app from Java/Kotlin, you must call
# System.loadLibrary() and pass the name of the library defined here;
# for GameActivity/NativeActivity derived applications, the same library name must be
# used in the AndroidManifest.xml file.
add_library(${CMAKE_PROJECT_NAME} SHARED
        # List C/C++ source files with relative paths to this CMakeLists.txt.
        dnbootstrap.c)

# Specifies libraries CMake should link to your target library. You
# can link libraries from various origins, such as libraries defined in this
# build script, prebuilt third-party libraries, or Android system libraries.
# ---- FristOneRR / Mali-G57 tuning: applied to OUR targets only (not GLFW) ----
# -mcpu=cortex-a75+nocrypto covers Helio G88 (A75+A55) and G100/D6300 (A76+A55);
# it implies armv8.2-a (+fp16 +dotprod) so a separate -march=armv8-a would be redundant/contradictory.
set(FRR_OPT -O3 -fvisibility=hidden -fno-semantic-interposition
            -mcpu=cortex-a75+nocrypto -fno-plt -ffunction-sections -fdata-sections)
set(FRR_LTO -flto=thin)

add_library(vk_frr SHARED frr_vulkan_shim.c)          # -> libvk_frr.so
target_compile_options(vk_frr PRIVATE ${FRR_OPT} ${FRR_LTO})
target_link_options(vk_frr PRIVATE ${FRR_LTO} -Wl,--gc-sections -Wl,-z,max-page-size=16384)
target_link_libraries(vk_frr dl log)

target_compile_options(${CMAKE_PROJECT_NAME} PRIVATE ${FRR_OPT} ${FRR_LTO})
target_link_options(${CMAKE_PROJECT_NAME} PRIVATE ${FRR_LTO} -Wl,--gc-sections -Wl,-z,max-page-size=16384)

target_link_libraries(${CMAKE_PROJECT_NAME}
        # List libraries link to the target library
        dl
        android
        log
        glfw
        nethost)