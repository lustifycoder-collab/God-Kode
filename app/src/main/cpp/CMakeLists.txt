# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

project("godkode")

add_subdirectory(third-party/tiny-AES-c)

add_library(${CMAKE_PROJECT_NAME} SHARED
        godkode.cpp
        gemini.cpp
        terminal/executor.cpp
)

target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        log
        tiny-aes
)

# nodewrap: seccomp wrapper that blocks the statx() syscall so that Node.js
# (v20+) can load modules inside the proot jail (which does not implement
# statx). Built as an executable; the Android packaging convention requires
# the file to be named lib<name>.so so it is extracted into the jniLibs ABI
# dir and can be chmod +x at runtime (see init.sh).
add_executable(nodewrap terminal/nodewrap.c)
set_target_properties(nodewrap PROPERTIES
        OUTPUT_NAME "libnodewrap.so"
)
# Statically link so the binary has no runtime lib dependencies inside proot.
target_link_options(nodewrap PRIVATE "-static")
