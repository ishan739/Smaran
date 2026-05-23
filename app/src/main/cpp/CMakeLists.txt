cmake_minimum_required(VERSION 3.22.1)
project("smaran")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

# whisper.cpp repo cloned here:
# app/src/main/cpp/whisper.cpp-src
set(WHISPER_CPP_DIR ${CMAKE_SOURCE_DIR}/whisper.cpp-src)

# Disable unnecessary whisper.cpp build targets
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)

# Disable unnecessary ggml backends for Android CPU build
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_BLAS OFF CACHE BOOL "" FORCE)
set(GGML_CUDA OFF CACHE BOOL "" FORCE)
set(GGML_METAL OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN OFF CACHE BOOL "" FORCE)

# Build upstream whisper.cpp properly
add_subdirectory(${WHISPER_CPP_DIR} whisper_build)

add_library(
        whisper_jni
        SHARED
        whisper_jni.cpp
)

target_include_directories(
        whisper_jni
        PRIVATE
        ${WHISPER_CPP_DIR}/include
        ${WHISPER_CPP_DIR}/src
        ${WHISPER_CPP_DIR}/ggml/include
        ${WHISPER_CPP_DIR}/ggml/src
)

target_compile_options(
        whisper_jni
        PRIVATE
        -O3
        -DNDEBUG
)

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(
        whisper_jni
        whisper
        ${log-lib}
        ${android-lib}
)