"""Bounded host-side recovery for CCPS only. Does not start an intentionally stopped service."""
import datetime
import fcntl
import json
import os
from pathlib import Path
import subprocess
import time
import urllib.request

STATE = Path('/var/lib/ccps-backend-watchdog/state.json')

def decision(state, now, running, started, healthy):
    state = dict(state)
    state['restarts'] = [x for x in state.get('restarts', []) if 0 <= now-x < 3600]
    if not running or now-started < 180:
        state['failures'] = 0
        return state, False
    state['failures'] = 0 if healthy else state.get('failures', 0)+1
    allowed = (state['failures'] >= 3 and now-state.get('last_restart', 0) >= 300
               and len(state['restarts']) < 3)
    return state, allowed

def main():
    STATE.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
    with (STATE.parent/'lock').open('a') as lock:
        try: fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError: return
        inspected = json.loads(subprocess.check_output(['docker','inspect','ccps-backend'], timeout=10))[0]
        labels = inspected['Config'].get('Labels') or {}
        if inspected['Name'] != '/ccps-backend' or labels.get('com.docker.compose.project') != 'ccps' or labels.get('com.docker.compose.service') != 'backend':
            raise RuntimeError('Refusing unexpected recovery target')
        now = time.time()
        started = datetime.datetime.strptime(inspected['State']['StartedAt'][:19], '%Y-%m-%dT%H:%M:%S').replace(tzinfo=datetime.timezone.utc).timestamp()
        healthy = False
        if inspected['State']['Running'] and now-started >= 180:
            try:
                client = urllib.request.build_opener(urllib.request.ProxyHandler({}))
                with client.open('http://127.0.0.1:8082/api/health', timeout=4) as response:
                    healthy = response.status == 200 and json.load(response).get('status') == 'UP'
            except Exception: pass
        old = json.loads(STATE.read_text()) if STATE.exists() else {}
        state, restart = decision(old,now,inspected['State']['Running'],started,healthy)
        if restart:
            # Record the attempt before acting: failed restarts must also be rate-limited.
            state['last_restart'] = now
            state['restarts'].append(now)
            state['failures'] = 0
        temporary = STATE.with_suffix('.new')
        with temporary.open('w') as file:
            os.chmod(str(temporary),0o600); json.dump(state,file)
        os.replace(str(temporary),str(STATE))
        if restart:
            print('CCPS health failed three consecutive checks; restarting exact backend container',flush=True)
            subprocess.run(['docker','restart','--time','20','ccps-backend'],check=True,timeout=60,stdout=subprocess.PIPE)

if __name__ == '__main__': main()
