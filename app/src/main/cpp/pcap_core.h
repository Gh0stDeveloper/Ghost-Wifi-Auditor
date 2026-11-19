#pragma once
#include <cstdint>
#include <set>
#include <string>

namespace ghost {
struct CaptureSummary {
    bool ok = false;
    std::string error;
    std::string format;
    std::uint64_t fileBytes = 0, packets = 0;
    std::uint64_t management = 0, control = 0, data = 0, ethernet = 0, other = 0;
    std::set<std::uint32_t> linkTypes;
    std::string json() const;
};
CaptureSummary inspectCapture(const std::string &filePath);
}
