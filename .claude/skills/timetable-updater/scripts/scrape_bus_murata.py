#!/usr/bin/env python3
"""
村田製作所発バス時刻表スクレイピング（to_yasu方向）

NAVITIMEのチェックボックスを自動発見して、
野洲駅行・野洲駅北口行の各経由別に時刻を取得する。

Usage:
  python3 scrape_bus_murata.py

Output:
  /tmp/bus_murata_scraped.json
"""

from playwright.sync_api import sync_playwright
import json
import re
import time

# 村田製作所発（to_yasu方向）
MURATA_URL = "https://transfer-cloud.navitime.biz/ohmitetudo/courses/timetables?busstop=00480508&course-sequence=0007900395-1"

OUTPUT_PATH = "/tmp/bus_murata_scraped.json"


def discover_checkboxes(page):
    """ページ上のすべてのチェックボックスと行先・経由情報を発見する。"""
    results = []
    checkboxes = page.query_selector_all("input[type='checkbox']")

    for cb in checkboxes:
        cb_id = cb.get_attribute("id")
        if not cb_id or cb_id == "selectUnselectAllCourses":
            continue

        row_info = cb.evaluate("""el => {
            let row = el.closest('tr') || el.closest('li') || el.closest('div');
            if (!row) return {full: ''};
            return { full: row.innerText.trim() };
        }""")

        full_text = row_info.get("full", "")
        label_el = page.query_selector(f"label[for='{cb_id}']")
        label_text = label_el.inner_text().strip() if label_el else ""

        is_yasu = "野洲駅" in full_text
        is_kitaguchi = "北口" in full_text

        results.append({
            "id": cb_id,
            "label": label_text,
            "full_text": full_text,
            "is_yasu": is_yasu,
            "is_kitaguchi": is_kitaguchi,
        })

    return results


def extract_via(text):
    """経由情報を抽出する"""
    match = re.search(r'\[(.+?)経由\]', text)
    return match.group(1) if match else ""


def scrape_timetable_cells(page):
    """
    現在表示されている時刻表から全時刻を抽出する。
    2カラム構成（平日/休日）の場合は各カラムを分けて返す。
    """
    col1_times = []  # 平日
    col2_times = []  # 休日

    rows = page.query_selector_all("tr")

    for row in rows:
        cells = row.query_selector_all("td, th")
        if len(cells) < 2:
            continue

        hour_text = cells[0].inner_text().strip()
        if not hour_text.isdigit():
            continue

        hour = int(hour_text)
        if hour >= 24:
            continue

        if len(cells) >= 2:
            min_text = cells[1].inner_text().strip()
            minutes = re.findall(r'(\d+)', min_text)
            for m in minutes:
                col1_times.append(f"{hour:02d}:{int(m):02d}")

        if len(cells) >= 3:
            min_text = cells[2].inner_text().strip()
            minutes = re.findall(r'(\d+)', min_text)
            for m in minutes:
                col2_times.append(f"{hour:02d}:{int(m):02d}")

    return sorted(col1_times), sorted(col2_times)


def uncheck_all_then_check(page, target_ids):
    """全チェックボックスを外してから、指定IDだけをチェックする。"""
    select_all = page.query_selector("#selectUnselectAllCourses")
    if select_all and select_all.is_checked():
        select_all.click()
        time.sleep(0.5)

    checkboxes = page.query_selector_all("input[type='checkbox']")
    for cb in checkboxes:
        cb_id = cb.get_attribute("id")
        if cb_id == "selectUnselectAllCourses":
            continue
        if cb and cb.is_checked():
            cb.click()
            time.sleep(0.2)

    for tid in target_ids:
        cb = page.query_selector(f"input[id='{tid}']")
        if cb and not cb.is_checked():
            cb.click()
            time.sleep(0.3)

    time.sleep(1)


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        print(f"Scraping: 村田製作所発 → 野洲駅方面")
        print(f"URL: {MURATA_URL}")

        page.goto(MURATA_URL, wait_until="networkidle")
        time.sleep(2)

        # Step 1: チェックボックスを発見
        checkboxes = discover_checkboxes(page)
        print(f"\nDiscovered {len(checkboxes)} checkboxes:")
        for cb in checkboxes:
            mark = ""
            if cb["is_kitaguchi"]:
                mark = " ★ 北口行"
            elif cb["is_yasu"]:
                mark = " ★ 野洲駅行"
            print(f"  ID: {cb['id']}: [{cb['label']}] full=[{cb['full_text'][:80]}]{mark}")

        yasu_cbs = [cb for cb in checkboxes if cb["is_yasu"] and not cb["is_kitaguchi"]]
        kita_cbs = [cb for cb in checkboxes if cb["is_kitaguchi"]]

        print(f"\n野洲駅行: {len(yasu_cbs)} routes")
        print(f"野洲駅北口行: {len(kita_cbs)} routes")

        # Step 2: 各経由を個別にスクレイピング
        route_results = []

        for cb in yasu_cbs:
            via = extract_via(cb["full_text"])
            print(f"\n--- 野洲駅行: {cb['full_text'][:60]} (via={via}) ---")
            page.goto(MURATA_URL, wait_until="networkidle")
            time.sleep(2)
            uncheck_all_then_check(page, [cb["id"]])
            wd, hd = scrape_timetable_cells(page)
            print(f"  weekday: {len(wd)} times: {wd}")
            print(f"  holiday: {len(hd)} times: {hd}")
            route_results.append({
                "checkbox_id": cb["id"],
                "full_text": cb["full_text"],
                "destination": "野洲駅",
                "via": via,
                "weekday": wd,
                "holiday": hd,
            })

        for cb in kita_cbs:
            via = extract_via(cb["full_text"])
            print(f"\n--- 北口行: {cb['full_text'][:60]} (via={via}) ---")
            page.goto(MURATA_URL, wait_until="networkidle")
            time.sleep(2)
            uncheck_all_then_check(page, [cb["id"]])
            wd, hd = scrape_timetable_cells(page)
            print(f"  weekday: {len(wd)} times: {wd}")
            print(f"  holiday: {len(hd)} times: {hd}")
            route_results.append({
                "checkbox_id": cb["id"],
                "full_text": cb["full_text"],
                "destination": "野洲駅(北口行)",
                "via": via,
                "weekday": wd,
                "holiday": hd,
            })

        # Step 3: 全選択で検証
        all_ids = [cb["id"] for cb in yasu_cbs + kita_cbs]
        print(f"\n--- Verification: all routes ---")
        page.goto(MURATA_URL, wait_until="networkidle")
        time.sleep(2)
        uncheck_all_then_check(page, all_ids)
        all_wd, all_hd = scrape_timetable_cells(page)
        individual_wd = sum(len(r["weekday"]) for r in route_results)
        individual_hd = sum(len(r["holiday"]) for r in route_results)
        print(f"  Combined weekday: {len(all_wd)} (individual sum: {individual_wd})")
        print(f"  Combined holiday: {len(all_hd)} (individual sum: {individual_hd})")

        output = {
            "description": "村田製作所発 → 野洲駅方面 スクレイピング結果",
            "scraped_at": time.strftime("%Y-%m-%d %H:%M:%S"),
            "routes": route_results,
        }

        with open(OUTPUT_PATH, "w") as f:
            json.dump(output, f, ensure_ascii=False, indent=2)

        print(f"\n=== Results saved to {OUTPUT_PATH} ===")

        print("\n=== Summary ===")
        for r in route_results:
            print(f"  {r['destination']} via {r['via']}: wd={len(r['weekday'])}, hd={len(r['holiday'])}")

        browser.close()


if __name__ == "__main__":
    main()
