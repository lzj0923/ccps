import importlib.util
from pathlib import Path
import sys
import types
import unittest

# Windows can exercise the pure policy without touching host services.
if sys.platform == 'win32': sys.modules['fcntl'] = types.SimpleNamespace()
spec=importlib.util.spec_from_file_location('watchdog',Path(__file__).with_name('ccps_backend_watchdog.py'))
watchdog=importlib.util.module_from_spec(spec);spec.loader.exec_module(watchdog)

class RecoveryPolicyTest(unittest.TestCase):
    def test_requires_three_failures(self):
        state={}
        for now in [1000,1030]:
            state,restart=watchdog.decision(state,now,True,0,False);self.assertFalse(restart)
        _,restart=watchdog.decision(state,1060,True,0,False);self.assertTrue(restart)
    def test_success_clears_failure_streak(self):
        state,restart=watchdog.decision({'failures':2},1000,True,0,True)
        self.assertEqual(state['failures'],0);self.assertFalse(restart)
    def test_stopped_or_starting_service_is_not_restarted(self):
        for running,started in [(False,0),(True,900)]:
            state,restart=watchdog.decision({'failures':5},1000,running,started,False)
            self.assertFalse(restart);self.assertEqual(state['failures'],0)
    def test_cooldown_and_hourly_budget(self):
        for state in [{'failures':9,'last_restart':900},{'failures':9,'restarts':[400,600,800]}]:
            self.assertFalse(watchdog.decision(state,1000,True,0,False)[1])
        self.assertTrue(watchdog.decision({'failures':9,'restarts':[100,200,300]},4000,True,0,False)[1])

if __name__=='__main__':unittest.main()
