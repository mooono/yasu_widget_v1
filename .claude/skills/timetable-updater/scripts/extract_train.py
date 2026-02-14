#!/usr/bin/env python3
"""
電車時刻表JSON生成スクリプト

JRおでかけネットから保存したHTMLファイルを解析し、
15駅分の時刻表を列車種別（普通/快速/新快速）付きで生成する。

入力:
  /tmp/jr_down_wd.html  - 下り・平日
  /tmp/jr_down.html     - 下り・土休日
  /tmp/jr_up_wd.html    - 上り・平日
  /tmp/jr_up.html       - 上り・土休日

出力:
  /tmp/train_timetable_with_type.json
"""
import re
import json
import sys

# LocationConstants.kt の TOKAIDO_STATIONS と一致させること
STATIONS = [
    ("Nagaokakyo", "長岡京"),
    ("Mukomachi", "向日町"),
    ("Katsuragawa", "桂川"),
    ("NishiOji", "西大路"),
    ("Kyoto", "京都"),
    ("Yamashina", "山科"),
    ("Otsu", "大津"),
    ("Zeze", "膳所"),
    ("Ishiyama", "石山"),
    ("Seta", "瀬田"),
    ("MinamiKusatsu", "南草津"),
    ("Kusatsu", "草津"),
    ("Ritto", "栗東"),
    ("Moriyama", "守山"),
    ("Yasu", "野洲"),
]

HTML_FILES = {
    ('down', 'weekday'): '/tmp/jr_down_wd.html',
    ('down', 'holiday'): '/tmp/jr_down.html',
    ('up', 'weekday'): '/tmp/jr_up_wd.html',
    ('up', 'holiday'): '/tmp/jr_up.html',
}

OUTPUT_PATH = '/tmp/train_timetable_with_type.json'


def extract_all_stations(html_path, direction):
    """HTMLから15駅分の時刻＋種別を抽出する"""
    with open(html_path) as f:
        html = f.read()

    rows = re.split(r'<tr[^>]*>', html)

    # 列車種別行を取得
    train_types = []
    for row in rows:
        if '列車種別' in row:
            cells_raw = re.findall(r'<td class="cell[^"]*">(.*?)</td>', row)
            for c in cells_raw:
                m = re.search(r'train_info">([^<]+)', c)
                train_types.append(m.group(1) if m else '')
            break

    # 行先行を取得
    destinations = []
    for row in rows:
        if 'station-name' in row:
            cells_raw = re.findall(r'<td class="cell[^"]*">(.*?)</td>', row)
            for c in cells_raw:
                m = re.search(r'station-name">([^<]+)', c)
                destinations.append(m.group(1) if m else '')
            break

    num_trains = len(train_types)

    # 駅ごとの時刻行を収集（「発」を優先、なければ「着」にフォールバック）
    station_rows_dep = {}
    station_rows_arr = {}
    for row in rows:
        if 'tbl-header' not in row:
            continue
        m = re.search(r'tbl-header[^>]*>([^<]+)<', row)
        if not m:
            m = re.search(r'tbl-header[^"]*"[^>]*rowspan="[^"]*">([^<]+)', row)
        if not m:
            continue
        station_name = m.group(1).strip()

        cells = re.findall(r'<td class="cell[^"]*">([^<]*)</td>', row)
        if len(cells) != num_trains:
            continue

        dep_m = re.search(r'departure-arrival[^>]*>([^<]*)', row)
        dep_type = dep_m.group(1).strip() if dep_m else ''

        if dep_type == '発':
            station_rows_dep[station_name] = cells
        elif dep_type == '着':
            station_rows_arr[station_name] = cells
        else:
            station_rows_dep[station_name] = cells

    # マージ: 発 > 着
    station_rows = {}
    all_names = set(list(station_rows_dep.keys()) + list(station_rows_arr.keys()))
    for name in all_names:
        station_rows[name] = station_rows_dep.get(name, station_rows_arr.get(name))

    results = {}
    for station_id, station_jp in STATIONS:
        if station_jp not in station_rows:
            print(f"  WARNING: {station_jp} not found in {html_path}")
            results[station_id] = []
            continue

        cells = station_rows[station_jp]
        entries = []
        for i, cell in enumerate(cells):
            cell = cell.strip()
            if not re.match(r'\d{2}:\d{2}', cell):
                continue
            ttype = train_types[i] if i < len(train_types) else ''
            if ttype in ('特急', '寝台特急'):
                continue
            dest = destinations[i] if i < len(destinations) else ''
            if cell.startswith('24:'):
                continue
            entries.append({
                'time': cell,
                'destination': dest,
                'type': ttype,
            })

        entries.sort(key=lambda x: x['time'])
        seen = set()
        unique = []
        for e in entries:
            if e['time'] not in seen:
                seen.add(e['time'])
                unique.append(e)
        results[station_id] = unique

    return results


def map_destination(dest, direction):
    """遠方行先を短縮ラベルに変換する"""
    if direction == 'up':
        mapping = {
            '網干': '姫路方面', '播州赤穂': '姫路方面', '上郡': '姫路方面',
            '新三田': '大阪方面', '関西空港': '大阪方面', '宝塚': '大阪方面',
            '倉吉': '大阪方面', '鳥取': '大阪方面', '香住': '大阪方面',
            '豊岡': '大阪方面', '白浜': '大阪方面', '浜坂': '大阪方面',
            '城崎温泉': '大阪方面',
        }
        return mapping.get(dest, dest if dest else '京都方面')
    else:
        mapping = {
            '四条畷': '米原方面', '松井山手': '米原方面', '長尾': '米原方面',
            '京田辺': '米原方面', '同志社前': '米原方面', '木津': '米原方面',
            '高山': '米原方面',
        }
        return mapping.get(dest, dest if dest else '米原方面')


def map_train_type(ttype):
    """列車種別を正規化する"""
    type_map = {'新快速': '新快速', '快速': '快速', '普通': '普通'}
    return type_map.get(ttype, ttype)


def build_train_json():
    """15駅分の train_timetable.json を生成する"""

    # 入力ファイルの存在チェック
    for (direction, day), path in HTML_FILES.items():
        try:
            with open(path) as f:
                pass
        except FileNotFoundError:
            print(f"ERROR: {path} が見つかりません")
            print("JRおでかけネットからHTMLを取得し /tmp/ に保存してください")
            sys.exit(1)

    all_data = {}
    for (direction, day), path in HTML_FILES.items():
        print(f"\nExtracting {direction}/{day} from {path}")
        station_data = extract_all_stations(path, direction)
        for station_id, entries in station_data.items():
            types_in = set(e['type'] for e in entries)
            print(f"  {station_id}: {len(entries)} trains, types: {types_in}")
        all_data[(direction, day)] = station_data

    # JSON構造を構築
    stations_json = {}
    for station_id, station_jp in STATIONS:
        def build_entries(direction, day, _sid=station_id):
            return [
                {
                    'time': e['time'],
                    'destination': map_destination(e['destination'], direction),
                    'type': map_train_type(e['type']),
                }
                for e in all_data[(direction, day)].get(_sid, [])
            ]

        stations_json[station_id] = {
            'name': station_jp,
            'lines': {
                'Tokaido': {
                    'name': '琵琶湖線',
                    'up': {
                        'weekday': build_entries('up', 'weekday'),
                        'holiday': build_entries('up', 'holiday'),
                    },
                    'down': {
                        'weekday': build_entries('down', 'weekday'),
                        'holiday': build_entries('down', 'holiday'),
                    }
                }
            }
        }

    train_json = {'stations': stations_json}

    with open(OUTPUT_PATH, 'w') as f:
        json.dump(train_json, f, ensure_ascii=False, indent=2)

    # サマリー出力
    print("\n=== Summary ===")
    for station_id, station_jp in STATIONS:
        s = stations_json[station_id]['lines']['Tokaido']
        print(f"{station_jp:5s}  上り平日:{len(s['up']['weekday']):3d}  "
              f"上り休日:{len(s['up']['holiday']):3d}  "
              f"下り平日:{len(s['down']['weekday']):3d}  "
              f"下り休日:{len(s['down']['holiday']):3d}")

    print(f"\nWritten to {OUTPUT_PATH}")
    print(f"\nデプロイ: cp {OUTPUT_PATH} app/src/main/assets/train_timetable.json")


if __name__ == '__main__':
    build_train_json()
