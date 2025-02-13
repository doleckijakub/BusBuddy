#!/usr/bin/env python3

import os
import sys
import time

from selenium import webdriver
from selenium.webdriver.chrome.service import Service

options = webdriver.ChromeOptions()
service = Service('/usr/bin/chromedriver')
driver = webdriver.Chrome(service=service, options=options)

import re
from bs4 import BeautifulSoup

from opencage.geocoder import OpenCageGeocode

geocoder = OpenCageGeocode(os.getenv("OPENCAGE_API_KEY"))

import sqlite3

db = sqlite3.connect("app/src/main/res/raw/appdata.db")

db.execute("""
    CREATE TABLE IF NOT EXISTS bus_stops (
        id INT PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        lat FLOAT NOT NULL,
        lng FLOAT NOT NULL,
        address TEXT NOT NULL
    )
    """)

db.commit()

db.execute("""
    CREATE TABLE IF NOT EXISTS bus_routes (
        id INT PRIMARY KEY,
        bus_name VARCHAR(15) NOT NULL,
        direction VARCHAR(255) NOT NULL
    )
    """)

db.commit()

db.execute("""
    CREATE TABLE IF NOT EXISTS route_stops (
        route_id INT NOT NULL,
        stop_id INT NOT NULL,
        PRIMARY KEY (route_id, stop_id)
    )
    """)

db.commit()

def get_bus_stop(id, it = 0):
    if it == 0:
        url = f"https://sip.ztm.lublin.eu/RTT.aspx?id={i}"
        driver.get(url)

    time.sleep(1)

    html = driver.page_source
    soup = BeautifulSoup(html, 'html.parser')
    stop_name_tag = soup.select_one('.realtable-stop > tbody:nth-child(1) > tr:nth-child(1) > th:nth-child(2)')

    if stop_name_tag:
        stop_name = re.sub('<[^>]*>', '', str(stop_name_tag)).strip()
    else:
        if it >= 5:
            return
        else:
            return get_bus_stop(id, it + 1)

    if stop_name == "":
        return

    geocoder_result = geocoder.geocode(f"{stop_name}, Lublin")[0]

    result = {
       "id": i,
       "name": stop_name,
       "lat": float(geocoder_result['geometry']['lat']),
       "lng": float(geocoder_result['geometry']['lng']),
       "address": geocoder_result['formatted'],
    }

    return result

# for i in range(1, 2000):
#     db_res = db.execute("SELECT * FROM bus_stops WHERE id = ?", (i, ))
#     result = db_res.fetchone()
#     if result is not None:
#         print(f"Bus stop #{i} already in the database", file = sys.stderr)
#         continue
#
#     bus_stop = get_bus_stop(i)
#     if bus_stop:
#         print(f"#{i}: {bus_stop}", file = sys.stderr)
#         cursor = db.cursor()
#         cursor.execute("""
#             INSERT INTO bus_stops (id, name, lat, lng, address)
#             VALUES (?, ?, ?, ?, ?)
#         """, (bus_stop["id"], bus_stop["name"], bus_stop["lat"], bus_stop["lng"], bus_stop["address"]))
#         db.commit()
#     else:
#         print(f"Could not get info for bus stop #{i}", file = sys.stderr)

busses = ["002","003","004","005","006","007","008","012","013","014","015","016","017","018","020","021","022","023","024","025","026","028","029","030","031","032","033","034","035","036","037","038","039","040","042","044","045","047","050","052","054","055","057","070","073","074","078","079","085","0N1","0N2","0N3","150","151","153","154","155","156","157","158","159","160","161","162","301","302","303","912","917","922","950","Bia","Zie"]

routes = {}

for bus in busses:
    driver.get(f"https://mpk.lublin.pl/index.php?s=rozklady&lin={bus}")

    # time.sleep(1)

    html = driver.page_source
    soup = BeautifulSoup(html, 'html.parser')

    tbody = soup.find('tbody')

    direction = None

    for row in tbody.find_all('tr'):
        text = row.get_text(strip=True)
        if "Kierunek:" in text:
            direction = text[len("Kierunek:"):]
            key = (bus, direction)
            if key not in routes:
                routes[key] = []
        else:
            columns = row.find_all('td')
            if len(columns) >= 2:
                if direction:
                    name = columns[1].get_text(strip=True)
                    if name[-2] == '0':
                        routes[(bus, direction)].append(re.sub(r'\d+ - +', '', name.upper()).replace(' - ', ' '))

driver.quit()

cursor = db.cursor()

for (bus_name, direction), stops in routes.items():
    def print_and_insert(sql, tuple):
        print(sql, tuple)
        cursor.execute(sql, tuple)

    def get_route_id():
        cursor.execute("""
            SELECT id FROM bus_routes WHERE bus_name = ? AND direction = ?
        """, (bus_name, direction))

        route = cursor.fetchone()

        if route is None:
            return

        return route[0]

    route_id = get_route_id()

    if route_id is None:
        print_and_insert("""
            INSERT INTO bus_routes (bus_name, direction)
            VALUES (?, ?)
        """, (bus_name, direction))
        db.commit()
        route_id = cursor.lastrowid
        cursor.execute("UPDATE bus_routes SET id = ? WHERE bus_name = ? AND direction = ?", (route_id, bus_name, direction))

    for stop_name in stops:
        cursor.execute("""
            SELECT id FROM bus_stops WHERE name = ?
        """, (stop_name,))

        stop = cursor.fetchone()

        if not stop:
            print(f"Stop {stop_name} not found", file = sys.stderr)
            continue

        stop_id = stop[0]

        cursor.execute("""
            SELECT 1 FROM route_stops WHERE route_id = ? AND stop_id = ?
        """, (route_id, stop_id))

        if not cursor.fetchone():
            print_and_insert("""
                INSERT INTO route_stops (route_id, stop_id) VALUES (?, ?)
            """, (route_id, stop_id))

db.commit()
db.close()