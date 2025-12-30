cmake_minimum_required(VERSION 3.22.1)

project("gamelabs")

# Cria a biblioteca específica para PS1
add_library(
        ps1-bridge
        SHARED
        ps1-bridge.cpp)

find_library(log-lib log)

target_link_libraries(
        ps1-bridge
        ${log-lib}
        GLESv2
        EGL
        android)