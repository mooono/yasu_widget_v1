#!/usr/bin/env python3
"""
野洲駅発バス時刻表スクレイピング（to_murata方向）

NAVITIMEのチェックボックスを操作して、
村田製作所行の便のみを経由別に正確に抽出する。

対象バス停:
  - 野洲駅のりば2（南口）: busstop=00480294
  - 野洲駅北口のりば1:     busstop=00480358

Usage:
  python3 scrape_bus_yasu.py

Output:
  /tmp/bus_yasu_scraped.json
"""

from playwright.sync_api import sync_playwright
import json
import re
import time

# 野洲駅のりば2（南口）
YASU_MINAMI_URL = "https://transfer-cloud.navitime.biz/ohmitetudo/courses/timetables?busstop=00480294&course-sequence=0007900384-1"

# 野洲駅北口のりば1
YASU_KITA_URL = "https://transfer-cloud.navitime.biz/ohmitetudo/courses/timetables?busstop=00480358&course-sequence=0007900402-1"

OUTPUT_PATH = "/tmp/bus_yasu_scraped.json"


def discover_checkboxes(page):
    """
    ページ上のすべてのチェックボックスとその系統・行先情報を発見する。
    チェックボックスは各行の1列目にあり、系統名は2列目、行先は3列目に表示される。
    returns: list of dict with keys: id, route_name, destination, full_text, is_murata
    """
    results = []

    # チェックボックスを含む全行（tr）を取得
    checkboxes = page.query_selector_all("input[type='checkbox']")

    for cb in checkboxes:
        cb_id = cb.get_attribute("id")
        if not cb_id or cb_id == "selectUnselectAllCourses":
            continue

        # チェックボックスを含む行（tr）の全テキストを取得
        row_info = cb.evaluate("""el => {
            // 親をたどってtr（テーブル行）またはli等を探す
            let row = el.closest('tr') || el.closest('li') || el.closest('div');
            if (!row) return {route: '', dest: '', full: ''};

            // 行内の全テキストセルを取得
            let cells = row.querySelectorAll('td, th, span, label');
            let texts = [];
            cells.forEach(c => {
                let t = c.innerText.trim();
                if (t) texts.push(t);
            });

            return {
                full: row.innerText.trim(),
                texts: texts
            };
        }""")

        full_text = row_info.get("full", "")

        # label[for]も試す
        label_el = page.query_selector(f"label[for='{cb_id}']")
        label_text = label_el.inner_text().strip() if label_el else ""

        is_murata = "村田製作所" in full_text
        results.append({
            "id": cb_id,
            "label": label_text,
            "full_text": full_text,
            "is_murata": is_murata,
        })

    return results


def extract_via(label):
    """
    ラベルから経由情報を抽出する。
    例: "村田製作所行 [生和神社経由]" → "生和神社"
    例: "村田製作所行 [三ツ坂経由]" → "三ツ坂"
    例: "村田製作所行 [西ゲート経由]" → "西ゲート"
    """
    match = re.search(r'\[(.+?)経由\]', label)
    if match:
        return match.group(1)
    return ""


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

        # column 1 (平日)
        if len(cells) >= 2:
            min_text = cells[1].inner_text().strip()
            # 「専」「■」「中循」等のマーカーを除去して数字だけ取る
            minutes = re.findall(r'(\d+)', min_text)
            for m in minutes:
                col1_times.append(f"{hour:02d}:{int(m):02d}")

        # column 2 (休日)
        if len(cells) >= 3:
            min_text = cells[2].inner_text().strip()
            minutes = re.findall(r'(\d+)', min_text)
            for m in minutes:
                col2_times.append(f"{hour:02d}:{int(m):02d}")

    return sorted(col1_times), sorted(col2_times)


def uncheck_all_then_check(page, target_ids):
    """
    全チェックボックスを外してから、指定IDだけをチェックする。
    """
    # まず「すべて選択・解除」で全部OFF
    select_all = page.query_selector("#selectUnselectAllCourses")
    if select_all and select_all.is_checked():
        select_all.click()
        time.sleep(0.5)

    # 全部OFFになったことを確認
    checkboxes = page.query_selector_all("input[type='checkbox']")
    for cb in checkboxes:
        cb_id = cb.get_attribute("id")
        if cb_id == "selectUnselectAllCourses":
            continue
        if cb and cb.is_checked():
            cb.click()
            time.sleep(0.2)

    # 指定IDだけをチェック
    for tid in target_ids:
        cb = page.query_selector(f"input[id='{tid}']")
        if cb and not cb.is_checked():
            cb.click()
            time.sleep(0.3)

    # 時刻表更新を待つ
    time.sleep(1)


def scrape_bus_stop(page, url, stop_name):
    """
    1つのバス停ページをスクレイピングして、
    村田製作所行の便を経由別に取得する。
    """
    print(f"\n{'='*60}")
    print(f"Scraping: {stop_name}")
    print(f"URL: {url}")
    print(f"{'='*60}")

    page.goto(url, wait_until="networkidle")
    time.sleep(2)

    # Step 1: チェックボックスを発見
    checkboxes = discover_checkboxes(page)
    print(f"\nDiscovered {len(checkboxes)} checkboxes:")
    for cb in checkboxes:
        mark = " ★ 村田製作所行" if cb["is_murata"] else ""
        print(f"  ID: {cb['id']}: [{cb['label']}] full=[{cb['full_text'][:80]}]{mark}")

    # 村田製作所行のチェックボックスだけを抽出
    murata_cbs = [cb for cb in checkboxes if cb["is_murata"]]
    print(f"\n村田製作所行: {len(murata_cbs)} routes")

    if not murata_cbs:
        print("  WARNING: No 村田製作所行 routes found!")
        return []

    # Step 2: 各経由を個別にスクレイピング
    route_results = []
    for cb in murata_cbs:
        via = extract_via(cb["full_text"])
        print(f"\n--- Route: {cb['full_text'][:60]} (via={via}) ---")

        page.goto(url, wait_until="networkidle")
        time.sleep(2)
        uncheck_all_then_check(page, [cb["id"]])
        weekday, holiday = scrape_timetable_cells(page)
        print(f"  weekday: {len(weekday)} times")
        print(f"  holiday: {len(holiday)} times")
        if weekday:
            print(f"  weekday sample: {weekday[:5]}...")
        if holiday:
            print(f"  holiday sample: {holiday[:5]}...")

        route_results.append({
            "checkbox_id": cb["id"],
            "label": cb["label"],
            "full_text": cb["full_text"],
            "via": via,
            "weekday": weekday,
            "holiday": holiday,
        })

    # Step 3: 全村田製作所行を選択して合計を検証
    all_murata_ids = [cb["id"] for cb in murata_cbs]
    print(f"\n--- Verification: all 村田製作所行 routes ---")
    page.goto(url, wait_until="networkidle")
    time.sleep(2)
    uncheck_all_then_check(page, all_murata_ids)
    all_wd, all_hd = scrape_timetable_cells(page)
    individual_wd = sum(len(r["weekday"]) for r in route_results)
    individual_hd = sum(len(r["holiday"]) for r in route_results)
    print(f"  Combined weekday: {len(all_wd)} (individual sum: {individual_wd})")
    print(f"  Combined holiday: {len(all_hd)} (individual sum: {individual_hd})")

    return route_results


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        # ============================
        # 野洲駅のりば2（南口）
        # ============================
        minami_results = scrape_bus_stop(
            page, YASU_MINAMI_URL, "野洲駅のりば2（南口）"
        )

        # ============================
        # 野洲駅北口のりば1
        # ============================
        kita_results = scrape_bus_stop(
            page, YASU_KITA_URL, "野洲駅北口のりば1"
        )

        # 結果をまとめて保存
        output = {
            "description": "野洲駅発 → 村田製作所行 スクレイピング結果",
            "scraped_at": time.strftime("%Y-%m-%d %H:%M:%S"),
            "南口_のりば2": minami_results,
            "北口_のりば1": kita_results,
        }

        with open(OUTPUT_PATH, "w") as f:
            json.dump(output, f, ensure_ascii=False, indent=2)

        print(f"\n{'='*60}")
        print(f"Results saved to {OUTPUT_PATH}")
        print(f"{'='*60}")

        # サマリー
        print("\n=== Summary ===")
        print("南口のりば2:")
        for r in minami_results:
            print(f"  {r['full_text'][:50]}: wd={len(r['weekday'])}, hd={len(r['holiday'])}")
        print("北口のりば1:")
        for r in kita_results:
            print(f"  {r['full_text'][:50]}: wd={len(r['weekday'])}, hd={len(r['holiday'])}")

        browser.close()


if __name__ == "__main__":
    main()
