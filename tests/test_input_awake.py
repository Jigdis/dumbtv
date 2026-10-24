"""Structural regression for the native input activity's display-lifetime contract.

The host Android jar cannot execute Activity/Window; real idle-time behavior
must additionally be checked on the television.
"""
from pathlib import Path
import unittest


class InputAwakeTest(unittest.TestCase):
    def test_input_window_requests_foreground_screen_on(self):
        source = (Path(__file__).resolve().parents[1] /
                  'launcher/src/tv/dumbtv/home/Input.java').read_text(encoding='utf-8')
        create = source.split('protected void onCreate(Bundle state) {', 1)[1].split(
            'protected void onStart()', 1)[0]
        self.assertIn('getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);',
                      create)
