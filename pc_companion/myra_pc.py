#!/usr/bin/env python3
"""
MYRA AI - Secure Desktop Companion Client
Connects with MYRA Android over the local network.
Transfers text, files, and sends approved commands.
"""

import sys
import os
import requests
import json

def main():
    print("=" * 55)
    print("     MYRA AI - PC COMPANION WORKSTATION CLIENT     ")
    print("=" * 55)
    
    phone_ip = input("Enter MYRA Android IP address (from app screen, e.g. 192.168.1.5): ").strip()
    port = input("Enter port [default 8088]: ").strip() or "8088"
    base_url = f"http://{phone_ip}:{port}"

    print(f"\nChecking status on {base_url}...")
    try:
        r = requests.get(f"{base_url}/status", timeout=5)
        print("Connected to MYRA Android!", r.json())
    except Exception as e:
        print(f"Error connecting: {e}")
        sys.exit(1)

    pin = input("\nEnter the 6-digit Pairing PIN shown on MYRA Android: ").strip()
    pair_res = requests.post(f"{base_url}/pair", json={
        "pin": pin,
        "pc_name": "My-PC"
    })
    
    if pair_res.status_code != 200 or not pair_res.json().get("success"):
        print("Pairing failed! Check PIN and try again.")
        sys.exit(1)

    print("Successfully paired with MYRA AI Android!")
    
    while True:
        print("\nOptions:")
        print("1. Send text message / clipboard to Phone")
        print("2. Send file to Phone")
        print("3. Check Phone status")
        print("4. Exit")
        choice = input("Select [1-4]: ").strip()

        if choice == "1":
            text = input("Enter text to send: ")
            res = requests.post(f"{base_url}/message", json={"text": text, "sender": "PC"})
            print("Response:", res.json())
        elif choice == "2":
            file_path = input("Enter path to file: ").strip()
            if os.path.exists(file_path):
                file_name = os.path.basename(file_path)
                with open(file_path, "rb") as f:
                    headers = {"X-File-Name": file_name}
                    res = requests.post(f"{base_url}/file", data=f, headers=headers)
                    print("Transfer result:", res.json())
            else:
                print("File not found!")
        elif choice == "3":
            res = requests.get(f"{base_url}/status")
            print("Status:", res.json())
        elif choice == "4":
            print("Goodbye!")
            break

if __name__ == "__main__":
    main()
