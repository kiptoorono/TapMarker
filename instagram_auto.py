import subprocess
import time
import re
import sys

ADB_PATH = r"C:\Users\Rono\Downloads\platform-tools-latest-windows\platform-tools\adb.exe"

def adb_command(command):
    full_cmd = f"{ADB_PATH} {command}"
    result = subprocess.run(full_cmd, shell=True, capture_output=True, text=True)
    return result.stdout.strip()

def get_coordinates_from_phone():
    adb_command("logcat -c")
    print("📱 Waiting for coordinates from the phone...")
    print("   Press the ▶ (Play) button on your phone's floating panel.")
    start_time = time.time()
    while time.time() - start_time < 30:
        output = adb_command("logcat -d -s TAP_MARKER:I")
        if "COORDS:" in output:
            match = re.search(r'COORDS: (.*)', output)
            if match:
                coords_str = match.group(1).strip()
                print(f"✅ Received coordinates: {coords_str}")
                return coords_str
        time.sleep(0.5)
    print("❌ Timeout: No coordinates received.")
    return None

def parse_and_execute(coords_str):
    if not coords_str:
        return False
    parts = coords_str.split('|')
    for part in parts:
        if part.startswith("SCROLL:"):
            data = part.replace("SCROLL:", "")
            x1, y1, x2, y2 = data.split(',')
            print(f"📜 Scrolling from ({x1},{y1}) to ({x2},{y2})...")
            adb_command(f"shell input swipe {x1} {y1} {x2} {y2} 300")
            time.sleep(1)
        elif part.startswith("TAP:"):
            data = part.replace("TAP:", "")
            taps = data.split(';')
            for tap in taps:
                _, x, y = tap.split(',')
                print(f"👆 Tapping at ({x}, {y})...")
                adb_command(f"shell input tap {x} {y}")
                time.sleep(0.8)
    time.sleep(0.5)
    link = adb_command("shell cmd clipboard get-text")
    if link:
        print(f"✅ Copied link: {link}")
        return link
    else:
        print("❌ No link found in clipboard.")
        return None

if __name__ == "__main__":
    coords = get_coordinates_from_phone()
    if coords:
        result = parse_and_execute(coords)
        if result:
            print(f"LINK_OUTPUT: {result}")
        else:
            print("LINK_OUTPUT: ERROR")
    else:
        print("LINK_OUTPUT: ERROR")
