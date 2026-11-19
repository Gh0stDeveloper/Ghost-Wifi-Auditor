#include "pcap_core.h"
#include <algorithm>
#include <array>
#include <fstream>
#include <limits>
#include <sstream>
#include <vector>

namespace ghost {
namespace {
constexpr std::uint64_t MAX_FILE = 128ULL * 1024 * 1024;
constexpr std::uint32_t MAX_BLOCK = 16U * 1024 * 1024;
constexpr std::uint64_t MAX_PACKETS = 4000000;
enum class Endian { LITTLE, BIG };
using Bytes = std::vector<std::uint8_t>;

std::uint16_t u16(const std::uint8_t* p, Endian e) {
    return e == Endian::LITTLE ? std::uint16_t(p[0] | (p[1] << 8))
        : std::uint16_t((p[0] << 8) | p[1]);
}
std::uint32_t u32(const std::uint8_t* p, Endian e) {
    if (e == Endian::LITTLE)
        return std::uint32_t(p[0]) | (std::uint32_t(p[1]) << 8) |
            (std::uint32_t(p[2]) << 16) | (std::uint32_t(p[3]) << 24);
    return (std::uint32_t(p[0]) << 24) | (std::uint32_t(p[1]) << 16) |
        (std::uint32_t(p[2]) << 8) | std::uint32_t(p[3]);
}
bool readAt(std::ifstream& file, std::uint64_t offset, std::uint8_t* out, std::size_t length) {
    file.clear();
    file.seekg(static_cast<std::streamoff>(offset));
    if (!file) return false;
    file.read(reinterpret_cast<char*>(out), static_cast<std::streamsize>(length));
    return file.good() || (file.eof() && static_cast<std::size_t>(file.gcount()) == length);
}
bool check(std::uint64_t from, std::uint64_t count, std::uint64_t limit) {
    return from <= limit && count <= limit - from;
}
bool fail(CaptureSummary& s, const char* reason) {
    s.ok = false;
    s.error = reason;
    return false;
}
void record(CaptureSummary& s, std::uint32_t link, const std::uint8_t* p, std::size_t size) {
    ++s.packets;
    s.linkTypes.insert(link);
    if (link == 1) { ++s.ethernet; return; }
    std::size_t offset = 0;
    if (link == 127) {
        if (size < 8 || p[0] != 0) { ++s.other; return; }
        offset = std::size_t(p[2]) | (std::size_t(p[3]) << 8);
        if (offset < 8 || offset + 2 > size) { ++s.other; return; }
    } else if (link != 105) {
        ++s.other;
        return;
    }
    if (offset + 2 > size) { ++s.other; return; }
    switch ((p[offset] >> 2) & 0x03) {
        case 0: ++s.management; break;
        case 1: ++s.control; break;
        case 2: ++s.data; break;
        default: ++s.other; break;
    }
}
bool packet(std::ifstream& file, CaptureSummary& s, std::uint64_t at,
    std::uint32_t size, std::uint32_t link, std::uint64_t total) {
    if (!check(at, size, total) || size > MAX_BLOCK) return fail(s, "Invalid packet bounds");
    std::array<std::uint8_t, 256> prefix{};
    const auto count = std::min<std::size_t>(prefix.size(), size);
    if (count && !readAt(file, at, prefix.data(), count))
        return fail(s, "Could not read packet");
    if (s.packets >= MAX_PACKETS) return fail(s, "Packet limit exceeded");
    record(s, link, prefix.data(), count);
    return true;
}
bool classic(std::ifstream& file, CaptureSummary& s, std::uint64_t total) {
    std::array<std::uint8_t, 24> header{};
    if (total < header.size() || !readAt(file, 0, header.data(), header.size()))
        return fail(s, "Truncated PCAP header");
    const bool little = header[0] == 0xd4 || header[0] == 0x4d;
    const auto e = little ? Endian::LITTLE : Endian::BIG;
    if (u16(header.data() + 4, e) != 2) return fail(s, "Unsupported PCAP major version");
    const auto snap = u32(header.data() + 16, e);
    const auto link = u32(header.data() + 20, e) & 0xffffu;
    if (snap == 0 || snap > MAX_BLOCK) return fail(s, "Invalid PCAP snaplen");
    s.format = "PCAP";
    s.linkTypes.insert(link);
    std::uint64_t pos = 24;
    while (pos < total) {
        std::array<std::uint8_t, 16> rec{};
        if (!check(pos, rec.size(), total) || !readAt(file,pos,rec.data(),rec.size()))
            return fail(s, "Truncated PCAP packet header");
        const auto incl = u32(rec.data()+8, e);
        const auto orig = u32(rec.data()+12, e);
        if (incl > snap || incl > orig) return fail(s, "Invalid PCAP packet length");
        pos += rec.size();
        if (!packet(file,s,pos,incl,link,total)) return false;
        pos += incl;
    }
    return true;
}
bool ng(std::ifstream& file, CaptureSummary& s, std::uint64_t total) {
    Endian endian = Endian::LITTLE;
    bool hasSection = false;
    std::vector<std::uint32_t> interfaces;
    std::uint64_t pos = 0;
    s.format = "PCAPNG";
    while (pos < total) {
        std::array<std::uint8_t, 32> h{};
        if (!check(pos,12,total) || !readAt(file,pos,h.data(),12))
            return fail(s, "Truncated PCAPNG block header");
        const bool section = h[0]==0x0a && h[1]==0x0d && h[2]==0x0d && h[3]==0x0a;
        if (section) {
            if (h[8]==0x4d && h[9]==0x3c && h[10]==0x2b && h[11]==0x1a) endian=Endian::LITTLE;
            else if (h[8]==0x1a && h[9]==0x2b && h[10]==0x3c && h[11]==0x4d) endian=Endian::BIG;
            else return fail(s, "Invalid PCAPNG byte-order magic");
            interfaces.clear();
            hasSection = true;
        } else if (!hasSection) return fail(s,"PCAPNG section header missing");
        const auto type = u32(h.data(),endian);
        const auto length = u32(h.data()+4,endian);
        if (length < 12 || length > MAX_BLOCK || (length & 3) || !check(pos,length,total))
            return fail(s, "Invalid PCAPNG block length");
        std::array<std::uint8_t,4> tail{};
        if (!readAt(file,pos+length-4,tail.data(),4) || u32(tail.data(),endian)!=length)
            return fail(s,"Mismatched PCAPNG block trailer");
        if (section && length < 28) return fail(s, "Truncated PCAPNG section");
        if (type==1) { // Interface Description Block
            if (length < 20 || !readAt(file,pos,h.data(),16))
                return fail(s,"Invalid PCAPNG interface block");
            if (interfaces.size() >= 512) return fail(s,"Too many PCAPNG interfaces");
            const auto link = u16(h.data()+8,endian);
            interfaces.push_back(link);
            s.linkTypes.insert(link);
        } else if (type==6 || type==2) { // Enhanced Packet Block / obsolete Packet Block
            const auto minLength = type==6 ? 32u : 32u;
            if (length < minLength || !readAt(file,pos,h.data(),28))
                return fail(s,"Invalid PCAPNG packet block");
            const auto iface = type==6 ? u32(h.data()+8,endian) : u16(h.data()+8,endian);
            const auto incl = u32(h.data()+20,endian);
            const auto orig = u32(h.data()+24,endian);
            if (iface>=interfaces.size() || incl>orig || incl>MAX_BLOCK ||
                !check(type==6?28:28,incl,length-4))
                return fail(s,"Invalid PCAPNG packet size or interface");
            if (!packet(file,s,pos+28,incl,interfaces[iface],total)) return false;
        } else if (type==3) { // Simple Packet Block
            if (length < 16 || interfaces.empty() || !readAt(file,pos,h.data(),12))
                return fail(s,"Invalid PCAPNG simple packet block");
            const auto orig = u32(h.data()+8,endian);
            const auto padded = length-16;
            if (orig > padded || padded-orig > 3) {
                // Snaplen may truncate original length; count but no reliable unpadded length.
                if (!packet(file,s,pos+12,padded,interfaces[0],total)) return false;
            } else if (!packet(file,s,pos+12,orig,interfaces[0],total)) return false;
        }
        pos += length;
    }
    return hasSection;
}
std::string escape(const std::string& input) {
    std::string out;
    for (const auto c:input) {
        if (c=='"' || c=='\\') { out+='\\';out+=c; }
        else if (static_cast<unsigned char>(c)<32) out+=' ';
        else out+=c;
    }
    return out;
}
} // namespace

std::string CaptureSummary::json() const {
    std::ostringstream out;
    out << "{\"ok\":" << (ok?"true":"false") << ",\"error\":\"" << escape(error)
        << "\",\"format\":\"" << escape(format)
        << "\",\"bytes\":" << fileBytes << ",\"packets\":" << packets
        << ",\"management\":" << management << ",\"control\":" << control
        << ",\"data\":" << data << ",\"ethernet\":" << ethernet
        << ",\"other\":" << other << ",\"linkTypes\":[";
    bool first=true;
    for (auto v:linkTypes) { if (!first) out<<","; first=false; out<<v; }
    out<<"]}";
    return out.str();
}
CaptureSummary inspectCapture(const std::string& path) {
    CaptureSummary s;
    std::ifstream file(path,std::ios::binary | std::ios::ate);
    if (!file.is_open()) { s.error="Cannot open private capture";return s; }
    const auto end = file.tellg();
    if (end < 0) { s.error="Cannot determine capture size";return s; }
    s.fileBytes=static_cast<std::uint64_t>(end);
    if (s.fileBytes>MAX_FILE) { s.error="Capture exceeds 128 MiB";return s; }
    std::array<std::uint8_t,4> magic{};
    if (s.fileBytes<4 || !readAt(file,0,magic.data(),4)) {
        s.error="Capture header missing";return s;
    }
    const bool isNg = magic==std::array<std::uint8_t,4>{0x0a,0x0d,0x0d,0x0a};
    const bool isClassic =
        magic==std::array<std::uint8_t,4>{0xd4,0xc3,0xb2,0xa1} ||
        magic==std::array<std::uint8_t,4>{0xa1,0xb2,0xc3,0xd4} ||
        magic==std::array<std::uint8_t,4>{0x4d,0x3c,0xb2,0xa1} ||
        magic==std::array<std::uint8_t,4>{0xa1,0xb2,0x3c,0x4d};
    if (!isNg && !isClassic) { s.error="Not a PCAP/PCAPNG capture";return s; }
    s.ok=isNg ? ng(file,s,s.fileBytes) : classic(file,s,s.fileBytes);
    return s;
}
} // namespace ghost
