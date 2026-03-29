cmake_minimum_required(VERSION 3.22.1)
project("androidstudiomobile")

add_library(
    native-bridge
    SHARED
    native_bridge.cpp
    shell_executor.cpp
    process_manager.cpp
    build_engine.cpp
)

find_library(log-lib log)

target_link_libraries(
    native-bridge
    ${log-lib}
    android
)
