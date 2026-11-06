#!/usr/bin/env python3
"""
PART 1: THE "SMART EMPLOYEE" BACKEND (Massive Catalog Pagination & Batch Automation)
- Scrapes 500 to 1,000+ best-selling / highly-rated Temu products via RapidAPI pagination (`page` & `offset`)
- Respects API rate limits (`time.sleep(2)` between page requests + 429 retry handling)
- Processes, translates into El Jadida Moroccan Darija via Gemini SDK, and upserts to Supabase in batches of 50
- Isolates page, LLM, and database errors so a single failure never crashes the pipeline
"""
import logging
import os
import time
from typing import Any
import requests
from google import genai
from supabase import create_client, Client

# Configure structured logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
)
logger = logging.getLogger("SmartEmployee")

# Environment Configuration
RAPIDAPI_KEY = os.environ["RAPIDAPI_KEY"]
RAPIDAPI_HOST = os.getenv(
    "RAPIDAPI_HOST",
    "temu-com-shopping-api-realtime-data-scraper.p.rapidapi.com",
)
GEMINI_API_KEY = os.environ["GEMINI_API_KEY"]
SUPABASE_URL = os.environ["SUPABASE_URL"]
SUPABASE_SERVICE_ROLE_KEY = os.environ["SUPABASE_SERVICE_ROLE_KEY"]

# Pagination & Batch Constants
TARGET_PRODUCT_COUNT = int(os.getenv("TARGET_PRODUCT_COUNT", "1000"))
PAGE_SIZE = int(os.getenv("PAGE_SIZE", "50"))
MAX_PAGES = int(os.getenv("MAX_PAGES", "30"))
BATCH_SIZE = 50
API_RATE_LIMIT_DELAY_SEC = 2.0
LLM_RATE_LIMIT_DELAY_SEC = 0.35

gemini_client = genai.Client(api_key=GEMINI_API_KEY)
supabase: Client = create_client(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY)


def fetch_temu_page(page: int, page_size: int = PAGE_SIZE) -> list[dict[str, Any]]:
    """
    Fetches a single page of best-selling Temu products in MAD (0% markup).
    Uses both `page` and `offset` parameters for compatibility across RapidAPI Temu endpoints.
    If the request fails, logs the error and returns an empty list so pagination continues.
    """
    url = f"https://{RAPIDAPI_HOST}/products/bestsellers"
    headers = {
        "x-rapidapi-key": RAPIDAPI_KEY,
        "x-rapidapi-host": RAPIDAPI_HOST,
    }
    offset = (page - 1) * page_size
    params = {
        "currency": "MAD",
        "region": "MA",
        "sort": "top_rated_bestsellers",
        "page": str(page),
        "offset": str(offset),
        "limit": str(page_size),
    }

    try:
        response = requests.get(url, headers=headers, params=params, timeout=30)
        if response.status_code == 429:
            logger.warning("Rate limited (HTTP 429) on page %d. Backing off for 10s...", page)
            time.sleep(10)
            response = requests.get(url, headers=headers, params=params, timeout=30)

        response.raise_for_status()
        payload = response.json()
        raw_items = (
            payload.get("data", {}).get("items")
            or payload.get("result", {}).get("items")
            or payload.get("items")
            or []
        )

        extracted: list[dict[str, Any]] = []
        for item in raw_items:
            try:
                goods_id = str(item.get("goods_id") or item.get("id") or "").strip()
                if not goods_id:
                    continue

                rating = float(item.get("rating") or 4.8)
                if rating < 4.5:
                    continue

                # STRICT RULE: Extracted price MUST be the exact regular Temu price in MAD (0% markup)
                exact_temu_price_mad = round(
                    float(item.get("regular_price_mad") or item.get("price") or 0), 2
                )
                if exact_temu_price_mad <= 0:
                    continue

                extracted.append(
                    {
                        "temu_goods_id": goods_id,
                        "original_title": str(item.get("title") or "").strip(),
                        "original_description": str(item.get("description") or "").strip(),
                        "image_url": str(
                            item.get("image_url") or item.get("thumb_url") or ""
                        ).strip(),
                        "sales_volume": str(item.get("sales_tip") or "+1,000 مبيعة").strip(),
                        "price_mad": exact_temu_price_mad,
                        "rating": rating,
                    }
                )
            except (ValueError, TypeError) as item_err:
                logger.warning("Skipping malformed item on page %d: %s", page, item_err)
                continue

        logger.info("Page %d (offset %d): extracted %d valid products.", page, offset, len(extracted))
        return extracted

    except requests.RequestException as req_err:
        logger.error("Failed to fetch page %d (offset %d): %s. Continuing to next page...", page, offset, req_err)
        return []
    except Exception as unexpected_err:
        logger.error("Unexpected error parsing page %d: %s. Continuing...", page, unexpected_err)
        return []


def localize_to_el_jadida_darija(original_title: str, original_description: str) -> str:
    """
    Translates product title into natural Moroccan Darija targeting buyers in El Jadida.
    Falls back to the original title if the LLM call fails so the batch never crashes.
    """
    system_instruction = (
        "Translate and rewrite this product title into natural, engaging, and simplified "
        "Moroccan Darija (الدارجة المغربية), targeting buyers in El Jadida. "
        "Avoid formal Arabic or Egyptian dialects. Return ONLY the translation."
    )
    prompt = f"{system_instruction}\n\nTitle: {original_title}\nDescription: {original_description}"

    try:
        response = gemini_client.models.generate_content(
            model="gemini-2.5-flash",
            contents=prompt,
        )
        translated = (response.text or "").strip()
        return translated if translated else original_title
    except Exception as llm_err:
        logger.error("LLM translation failed for '%s': %s. Using fallback title.", original_title[:50], llm_err)
        return original_title


def process_and_upsert_batch(batch_items: list[dict[str, Any]], batch_number: int) -> int:
    """
    Translates a batch of up to 50 products into El Jadida Darija and upserts them into Supabase.
    Returns the number of successfully upserted records.
    """
    if not batch_items:
        return 0

    logger.info("Processing Batch #%d (%d products)...", batch_number, len(batch_items))
    upsert_rows: list[dict[str, Any]] = []

    for prod in batch_items:
        darija_title = localize_to_el_jadida_darija(
            prod["original_title"],
            prod["original_description"],
        )
        upsert_rows.append(
            {
                "temu_goods_id": prod["temu_goods_id"],
                "original_title": prod["original_title"],
                "darija_title": darija_title,
                "image_url": prod["image_url"],
                "sales_volume": prod["sales_volume"],
                "price_mad": prod["price_mad"],  # 0% markup guaranteed
                "rating": prod["rating"],
                "is_active": True,
            }
        )
        time.sleep(LLM_RATE_LIMIT_DELAY_SEC)

    try:
        supabase.table("products").upsert(
            upsert_rows,
            on_conflict="temu_goods_id",
        ).execute()
        logger.info("Batch #%d: successfully upserted %d products to Supabase.", batch_number, len(upsert_rows))
        return len(upsert_rows)
    except Exception as db_err:
        logger.error("Supabase upsert failed for Batch #%d: %s", batch_number, db_err)
        return 0


def run_smart_employee_pipeline(target_count: int = TARGET_PRODUCT_COUNT) -> None:
    """
    Paginates through the Temu API to scrape 500-1,000+ best-selling products,
    respecting rate limits (`time.sleep(2)`) and flushing every 50 products to Supabase.
    """
    logger.info(
        "Starting Smart Employee massive sync: target=%d products, batch_size=%d",
        target_count,
        BATCH_SIZE,
    )

    seen_goods_ids: set[str] = set()
    pending_batch: list[dict[str, Any]] = []
    total_scraped = 0
    total_upserted = 0
    batch_number = 0
    consecutive_empty_pages = 0

    for page in range(1, MAX_PAGES + 1):
        if total_scraped >= target_count:
            logger.info("Reached target product count (%d). Stopping pagination.", target_count)
            break

        page_items = fetch_temu_page(page=page, page_size=PAGE_SIZE)

        if not page_items:
            consecutive_empty_pages += 1
            logger.warning("Page %d returned 0 items (consecutive empty pages: %d).", page, consecutive_empty_pages)
            if consecutive_empty_pages >= 3:
                logger.warning("3 consecutive empty/failed pages encountered. Ending pagination early.")
                break
        else:
            consecutive_empty_pages = 0

        for item in page_items:
            goods_id = item["temu_goods_id"]
            if goods_id in seen_goods_ids:
                continue
            seen_goods_ids.add(goods_id)
            pending_batch.append(item)
            total_scraped += 1

            # Flush immediately whenever pending_batch hits BATCH_SIZE (50 products)
            if len(pending_batch) >= BATCH_SIZE:
                batch_number += 1
                total_upserted += process_and_upsert_batch(pending_batch, batch_number)
                pending_batch.clear()

            if total_scraped >= target_count:
                break

        # Rate-limit delay between API page requests to avoid RapidAPI/Temu blocks
        if page < MAX_PAGES and total_scraped < target_count:
            logger.info("Sleeping %.1fs before next API page request...", API_RATE_LIMIT_DELAY_SEC)
            time.sleep(API_RATE_LIMIT_DELAY_SEC)

    # Flush any remaining products (< 50) in the final partial batch
    if pending_batch:
        batch_number += 1
        total_upserted += process_and_upsert_batch(pending_batch, batch_number)
        pending_batch.clear()

    logger.info(
        "Smart Employee run complete! Total unique scraped: %d | Total upserted to Supabase: %d",
        total_scraped,
        total_upserted,
    )


if __name__ == "__main__":
    run_smart_employee_pipeline()
