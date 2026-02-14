#!/usr/bin/env python3
"""
バス時刻表JSON生成スクリプト

スクレイピング結果のJSONファイルから bus_timetable.json を生成する。
路線: 野洲駅 ⇄ 村田製作所（野洲）

使い方:
  1. scrape_bus_yasu.py と scrape_bus_murata.py を実行して
     /tmp/bus_yasu_scraped.json と /tmp/bus_murata_scraped.json を生成
  2. python3 generate_bus.py を実行
  3. 出力を app/src/main/assets/bus_timetable.json にコピー

出力:
  /tmp/bus_timetable_v2.json
"""
import json
import sys

OUTPUT_PATH = '/tmp/bus_timetable_v2.json'
YASU_SCRAPED = '/tmp/bus_yasu_scraped.json'
MURATA_SCRAPED = '/tmp/bus_murata_scraped.json'


def load_scraped(path):
    """スクレイピング結果のJSONを読み込む"""
    try:
        with open(path) as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"ERROR: {path} not found. Run the scraper first.")
        sys.exit(1)


def merge_routes_to_entries(routes, day_type):
    """
    複数のroute結果を経由情報付きの time entries にマージして時刻順ソートする。
    routes: list of {destination, via, weekday, holiday}
    day_type: 'weekday' or 'holiday'
    returns: list of {time, destination, via} sorted by time
    """
    entries = []
    for route in routes:
        dest = route["destination"]
        via = route.get("via", "")
        times = route.get(day_type, [])
        for t in times:
            entries.append({
                "time": t,
                "destination": dest,
                "via": via,
                "_sort": t + "_" + dest,
            })

    entries.sort(key=lambda e: e["_sort"])
    return [{"time": e["time"], "destination": e["destination"], "via": e["via"]} for e in entries]


def build_to_murata(yasu_data):
    """
    野洲駅発 → 村田製作所方面のデータを構築する。
    南口のりば2 と 北口のりば1 のスクレイピング結果をマージする。
    """
    routes = []

    # 南口のりば2（destination = "村田製作所"）
    for r in yasu_data.get("南口_のりば2", []):
        routes.append({
            "destination": "村田製作所",
            "via": r["via"],
            "weekday": r["weekday"],
            "holiday": r["holiday"],
        })

    # 北口のりば1（destination = "村田製作所(北口発)"）
    for r in yasu_data.get("北口_のりば1", []):
        routes.append({
            "destination": "村田製作所(北口発)",
            "via": r["via"],
            "weekday": r["weekday"],
            "holiday": r["holiday"],
        })

    return {
        "weekday": merge_routes_to_entries(routes, "weekday"),
        "holiday": merge_routes_to_entries(routes, "holiday"),
    }


def build_to_yasu(murata_data):
    """
    村田製作所発 → 野洲駅方面のデータを構築する。
    """
    routes = murata_data.get("routes", [])
    return {
        "weekday": merge_routes_to_entries(routes, "weekday"),
        "holiday": merge_routes_to_entries(routes, "holiday"),
    }


def build_bus_json():
    yasu_data = load_scraped(YASU_SCRAPED)
    murata_data = load_scraped(MURATA_SCRAPED)

    to_murata = build_to_murata(yasu_data)
    to_yasu = build_to_yasu(murata_data)

    bus_json = {
        "route_name": "野洲駅 ⇄ 村田製作所（野洲）",
        "to_yasu": to_yasu,
        "to_murata": to_murata,
        "notes": [
            "村田製作所休業日は運休の可能性があります（v1は休業日判定しません）",
            "(北口発) = 野洲駅北口のりば1から発車",
            "それ以外 = 野洲駅のりば2（南口）から発車",
            "via = 経由地（三ツ坂/生和神社/野洲中学校/西ゲート）",
            # ↓ ダイヤ改正時にここを更新する
            "2026年2月時点のダイヤに基づきます",
        ],
    }

    with open(OUTPUT_PATH, "w") as f:
        json.dump(bus_json, f, ensure_ascii=False, indent=2)

    # サマリー
    for direction in ["to_murata", "to_yasu"]:
        for day in ["weekday", "holiday"]:
            entries = bus_json[direction][day]
            total = len(entries)
            kita = sum(1 for e in entries if "北口" in e.get("destination", ""))
            vias = {}
            for e in entries:
                v = e.get("via", "")
                vias[v] = vias.get(v, 0) + 1
            via_str = ", ".join(f"{k}:{v}" for k, v in sorted(vias.items()))
            print(f"{direction}/{day}: {total} entries ({kita} 北口) [{via_str}]")

    print(f"\nWritten to {OUTPUT_PATH}")
    print(f"デプロイ: cp {OUTPUT_PATH} app/src/main/assets/bus_timetable.json")


if __name__ == "__main__":
    build_bus_json()
