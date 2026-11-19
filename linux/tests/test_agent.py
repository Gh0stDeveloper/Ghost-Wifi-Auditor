"""Tests for the Linux agent, run without root, Wi-Fi or tcpdump."""
import importlib.util
import pathlib
import sys
import tempfile
import unittest
from unittest import mock

AGENT = pathlib.Path(__file__).parents[1] / "ghost_wifi_agent.py"
spec = importlib.util.spec_from_file_location("ghost_wifi_agent", AGENT)
agent = importlib.util.module_from_spec(spec)
spec.loader.exec_module(agent)


class AgentTests(unittest.TestCase):
    def test_parse_monitor_interfaces(self):
        sample = """phy#0
    Interface wlan0
        ifindex 4
        type managed
    Interface mon0
        ifindex 7
        type monitor
"""
        found = agent.parse_iw(sample)
        self.assertEqual(found, [
            {"name": "wlan0", "type": "managed"},
            {"name": "mon0", "type": "monitor"},
        ])

    def test_reject_malicious_interface(self):
        self.assertFalse(agent.IFACE.fullmatch("mon0;whoami"))
        self.assertFalse(agent.IFACE.fullmatch("--help"))
        self.assertFalse(agent.IFACE.fullmatch("mon0/spoof"))
        self.assertTrue(agent.IFACE.fullmatch("mon0"))

    @mock.patch.object(agent, "iw_interfaces", return_value=[{"name": "wlan0", "type": "managed"}])
    def test_cannot_capture_without_monitor(self, _):
        result = agent.create_capture("wlan0", 5)
        self.assertFalse(result["ok"])
        self.assertIn("monitor", result["error"])

    def test_limit_duration(self):
        self.assertFalse(agent.create_capture("mon0", 1000)["ok"])
        self.assertFalse(agent.create_capture("mon0", -1)["ok"])

    def test_cleanup_path_validation(self):
        self.assertFalse(agent.cleanup("../../etc/passwd")["ok"])
        with tempfile.TemporaryDirectory() as temp:
            with mock.patch.object(agent, "CAPTURE_DIR", pathlib.Path(temp)):
                ident = "11111111-2222-3333-4444-555555555555"
                path = pathlib.Path(temp) / (ident + ".pcap")
                path.write_bytes(b"placeholder")
                self.assertTrue(agent.cleanup(ident)["ok"])
                self.assertFalse(path.exists())


if __name__ == "__main__":
    unittest.main()
