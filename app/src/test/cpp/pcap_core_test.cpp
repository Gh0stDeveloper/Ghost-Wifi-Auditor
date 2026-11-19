#include "../../main/cpp/pcap_core.h"
#include <cassert>
#include <cstdint>
#include <filesystem>
#include <fstream>
#include <iostream>
#include <vector>

using Bytes = std::vector<std::uint8_t>;
void u16(Bytes &b, std::uint16_t value) {
    b.push_back(value & 0xff); b.push_back((value >> 8) & 0xff);
}
void u32(Bytes &b, std::uint32_t value) {
    u16(b, value & 0xffff); u16(b, value >> 16);
}
void block(Bytes &out, std::uint32_t id, const Bytes& payload) {
    u32(out, id);
    u32(out, static_cast<std::uint32_t>(payload.size()+12));
    out.insert(out.end(),payload.begin(),payload.end());
    u32(out, static_cast<std::uint32_t>(payload.size()+12));
}
std::filesystem::path write(const Bytes &b, const char* suffix) {
    const auto p = std::filesystem::temp_directory_path() /
        (std::string("ghost-pcap-m3-") + suffix);
    std::ofstream out(p,std::ios::binary|std::ios::trunc);
    out.write(reinterpret_cast<const char*>(b.data()),b.size());
    out.close();
    return p;
}
int main() {
    Bytes classic={0xd4,0xc3,0xb2,0xa1};
    u16(classic,2); u16(classic,4);
    u32(classic,0);u32(classic,0);u32(classic,65535);u32(classic,105);
    u32(classic,1);u32(classic,0);u32(classic,4);u32(classic,4);
    classic.insert(classic.end(),{0x80,0x00,0x00,0x00});
    const auto a=write(classic,"classic.pcap");
    auto s=ghost::inspectCapture(a.string());
    assert(s.ok && s.format=="PCAP" && s.packets==1 && s.management==1);
    assert(s.linkTypes.count(105)==1);

    Bytes ng;
    Bytes shb={0x4d,0x3c,0x2b,0x1a,0x01,0x00,0x00,0x00,
        0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff};
    block(ng,0x0a0d0d0a,shb);
    Bytes idb;u16(idb,105);u16(idb,0);u32(idb,65535);
    block(ng,1,idb);
    Bytes epb;
    u32(epb,0);u32(epb,0);u32(epb,0);u32(epb,4);u32(epb,4);
    epb.insert(epb.end(),{0x08,0x00,0x00,0x00});
    block(ng,6,epb);
    const auto b=write(ng,"ng.pcapng");
    s=ghost::inspectCapture(b.string());
    assert(s.ok && s.format=="PCAPNG" && s.packets==1 && s.data==1);
    ng.back()^=0xff;
    const auto invalid=write(ng,"bad.pcapng");
    s=ghost::inspectCapture(invalid.string());
    assert(!s.ok && !s.error.empty());

    classic.pop_back();
    const auto truncated=write(classic,"truncated.pcap");
    s=ghost::inspectCapture(truncated.string());
    assert(!s.ok);

    const auto unknown=write({0,1,2,3},"unknown.bin");
    s=ghost::inspectCapture(unknown.string());
    assert(!s.ok && s.packets==0);
    for (auto& p: {a,b,invalid,truncated,unknown}) std::filesystem::remove(p);
    std::cout << "Native PCAP/PCAPNG parser tests passed" << std::endl;
}
