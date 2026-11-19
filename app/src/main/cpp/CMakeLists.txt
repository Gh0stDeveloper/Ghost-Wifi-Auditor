cmake_minimum_required(VERSION 3.22.1)
project(ghostpcap LANGUAGES CXX)
add_library(ghostpcap SHARED pcap_inspector.cpp)
target_compile_features(ghostpcap PRIVATE cxx_std_17)
