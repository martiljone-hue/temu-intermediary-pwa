package com.example.data.blueprint

data class BlueprintArtifactFile(
    val id: String,
    val titleDarija: String,
    val relativePath: String,
    val language: String,
    val descriptionDarija: String,
    val content: String
)

object AutomationBlueprintCode {

    val folderStructureTree = """
el-jadida-temu-proxy/
├── .github/
│   └── workflows/
│       └── daily_temu_sync.yml          # Cron Job: Runs every day at 02:00 AM UTC
├── backend/
│   ├── requirements.txt                 # requests, google-genai, supabase, python-dotenv
│   ├── smart_employee.py                # RapidAPI Temu -> 0% Markup MAD -> Gemini Darija -> Supabase
│   └── schema.sql                       # Supabase PostgreSQL schema + RLS policies
└── frontend-pwa/
    ├── public/
    │   ├── manifest.json                # RTL Arabic/Darija PWA Manifest (standalone)
    │   └── sw.js                        # Offline-capable Service Worker
    ├── src/
    │   ├── components/
    │   │   ├── FullScreenModal.tsx      # 100dvh body-scroll-locked modal (NO bottom sheets)
    │   │   ├── OrderForm.tsx            # Single-step Checkout + +15 MAD Fee + WhatsApp + Confirmation
    │   │   └── ReceiptGenerator.tsx     # Single-page White-Label A4 PDF Receipt + Dynamic 2-3 Image Scaling
    │   ├── App.tsx                      # 100% RTL Cairo/Tajawal Storefront & Install Prompt
    │   └── index.css                    # RTL rules + @media print single-page A4 enforcement
    └── vite.config.ts
    """.trimIndent()

    val supabaseSchemaSql = """
-- ============================================================================
-- SUPABASE SCHEMA: El Jadida Temu Proxy (0% Product Markup + 15 MAD Service Fee)
-- ============================================================================
create extension if not exists "pgcrypto";

create table if not exists public.products (
    id uuid primary key default gen_random_uuid(),
    temu_goods_id text unique not null,
    original_title text not null,
    darija_title text not null,
    image_url text not null,
    sales_volume text not null default '+1,000 مبيعة',
    price_mad numeric(10, 2) not null check (price_mad > 0), -- Exact regular Temu price in MAD (0% markup)
    rating numeric(3, 2) default 4.80,
    is_active boolean not null default true,
    updated_at timestamptz not null default now(),
    created_at timestamptz not null default now()
);

create table if not exists public.orders (
    id uuid primary key default gen_random_uuid(),
    order_code text unique not null,
    customer_name text not null,
    customer_phone text not null,
    customer_address text not null,
    subtotal_mad numeric(10, 2) not null,
    service_fee_mad numeric(10, 2) not null default 15.00 check (service_fee_mad = 15.00),
    total_mad numeric(10, 2) not null,
    status text not null default 'pending_whatsapp' check (status in ('pending_whatsapp', 'confirmed', 'delivered')),
    items jsonb not null default '[]'::jsonb,
    created_at timestamptz not null default now()
);

alter table public.products enable row level security;
alter table public.orders enable row level security;

-- Public storefront read access for active localized products
create policy "Allow public read of active products"
    on public.products for select
    using (is_active = true);

-- Anonymous frictionless order creation from PWA
create policy "Allow public insert of proxy orders"
    on public.orders for insert
    with check (service_fee_mad = 15.00 and total_mad = subtotal_mad + 15.00);
    """.trimIndent()

    val pythonSmartEmployeeScript = """
#!/usr/bin/env python3
\"\"\"
PART 1: THE "SMART EMPLOYEE" BACKEND (Massive Catalog Pagination & Batch Automation)
- Scrapes 500 to 1,000+ best-selling / highly-rated Temu products via RapidAPI pagination (`page` & `offset`)
- Respects API rate limits (`time.sleep(2)` between page requests + 429 retry handling)
- Processes, translates into El Jadida Moroccan Darija via Gemini SDK, and upserts to Supabase in batches of 50
- Isolates page, LLM, and database errors so a single failure never crashes the pipeline
\"\"\"
import logging
import os
import time
from typing import Any
import requests
from google import genai
from supabase import create_client, Client

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
)
logger = logging.getLogger("SmartEmployee")

RAPIDAPI_KEY = os.environ["RAPIDAPI_KEY"]
RAPIDAPI_HOST = os.getenv(
    "RAPIDAPI_HOST",
    "temu-com-shopping-api-realtime-data-scraper.p.rapidapi.com",
)
GEMINI_API_KEY = os.environ["GEMINI_API_KEY"]
SUPABASE_URL = os.environ["SUPABASE_URL"]
SUPABASE_SERVICE_ROLE_KEY = os.environ["SUPABASE_SERVICE_ROLE_KEY"]

TARGET_PRODUCT_COUNT = int(os.getenv("TARGET_PRODUCT_COUNT", "1000"))
PAGE_SIZE = int(os.getenv("PAGE_SIZE", "50"))
MAX_PAGES = int(os.getenv("MAX_PAGES", "30"))
BATCH_SIZE = 50
API_RATE_LIMIT_DELAY_SEC = 2.0
LLM_RATE_LIMIT_DELAY_SEC = 0.35

gemini_client = genai.Client(api_key=GEMINI_API_KEY)
supabase: Client = create_client(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY)


def fetch_temu_page(page: int, page_size: int = PAGE_SIZE) -> list[dict[str, Any]]:
    \"\"\"
    Fetches a single page of best-selling Temu products in MAD (0% markup).
    Uses both `page` and `offset` parameters for compatibility across RapidAPI Temu endpoints.
    If the request fails, logs the error and returns an empty list so pagination continues.
    \"\"\"
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
    \"\"\"
    Translates product title into natural Moroccan Darija targeting buyers in El Jadida.
    Falls back to the original title if the LLM call fails so the batch never crashes.
    \"\"\"
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
    \"\"\"
    Translates a batch of up to 50 products into El Jadida Darija and upserts them into Supabase.
    Returns the number of successfully upserted records.
    \"\"\"
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
    \"\"\"
    Paginates through the Temu API to scrape 500-1,000+ best-selling products,
    respecting rate limits (`time.sleep(2)`) and flushing every 50 products to Supabase.
    \"\"\"
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

            if len(pending_batch) >= BATCH_SIZE:
                batch_number += 1
                total_upserted += process_and_upsert_batch(pending_batch, batch_number)
                pending_batch.clear()

            if total_scraped >= target_count:
                break

        if page < MAX_PAGES and total_scraped < target_count:
            logger.info("Sleeping %.1fs before next API page request...", API_RATE_LIMIT_DELAY_SEC)
            time.sleep(API_RATE_LIMIT_DELAY_SEC)

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
    """.trimIndent()

    val githubActionsYaml = """
name: Smart Employee Daily Temu Sync (El Jadida)

on:
  schedule:
    # Runs automatically every day at 02:00 AM
    - cron: '0 2 * * *'
  workflow_dispatch:

jobs:
  sync-temu-to-supabase:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up Python 3.11
        uses: actions/setup-python@v5
        with:
          python-version: '3.11'
          cache: 'pip'

      - name: Install dependencies
        run: |
          pip install --upgrade pip
          pip install requests google-genai supabase python-dotenv

      - name: Execute Smart Employee Localization Pipeline
        env:
          RAPIDAPI_KEY: ${'$'}{{ secrets.RAPIDAPI_KEY }}
          RAPIDAPI_HOST: ${'$'}{{ secrets.RAPIDAPI_HOST }}
          GEMINI_API_KEY: ${'$'}{{ secrets.GEMINI_API_KEY }}
          SUPABASE_URL: ${'$'}{{ secrets.SUPABASE_URL }}
          SUPABASE_SERVICE_ROLE_KEY: ${'$'}{{ secrets.SUPABASE_SERVICE_ROLE_KEY }}
        run: python backend/smart_employee.py
    """.trimIndent()

    val fullScreenModalTsx = """
import React, { useEffect } from 'react';

interface FullScreenModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
}

/**
 * CRUCIAL RULE: 100dvh Full-Screen Modal with strict body scroll locking.
 * Strictly NO bottom sheets to prevent iOS Safari rubber-banding & viewport scrolling bugs.
 */
export const FullScreenModal: React.FC<FullScreenModalProps> = ({
  isOpen,
  onClose,
  title,
  children,
}) => {
  useEffect(() => {
    if (!isOpen) return;
    const scrollY = window.scrollY;
    const originalStyle = {
      overflow: document.body.style.overflow,
      position: document.body.style.position,
      top: document.body.style.top,
      width: document.body.style.width,
    };

    document.body.style.overflow = 'hidden';
    document.body.style.position = 'fixed';
    document.body.style.top = `-${'$'}{scrollY}px`;
    document.body.style.width = '100%';

    return () => {
      document.body.style.overflow = originalStyle.overflow;
      document.body.style.position = originalStyle.position;
      document.body.style.top = originalStyle.top;
      document.body.style.width = originalStyle.width;
      window.scrollTo(0, scrollY);
    };
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <div
      dir="rtl"
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex flex-col bg-[#FAF7F2] text-[#141D1A]"
      style={{ height: '100dvh', width: '100vw', fontFamily: "'Cairo', 'Tajawal', sans-serif" }}
    >
      <header className="flex items-center justify-between px-5 py-4 bg-[#0F2922] text-white shrink-0 shadow-md">
        <h2 className="text-lg font-bold truncate">{title}</h2>
        <button
          type="button"
          onClick={onClose}
          className="px-4 py-2 rounded-xl bg-white/15 hover:bg-white/25 text-sm font-bold"
        >
          إغلاق ✕
        </button>
      </header>
      <main className="flex-1 overflow-y-auto overscroll-contain p-5">
        {children}
      </main>
    </div>
  );
};
    """.trimIndent()

    val orderFormTsx = """
import React, { useState } from 'react';

export interface OrderItem {
  id: string;
  darijaTitle: string;
  priceMad: number; // Exact Temu regular price in MAD (0% markup)
  quantity: number;
  imageUrl: string;
}

interface OrderFormProps {
  items: OrderItem[];
  whatsappBusinessNumber?: string; // e.g. "212661000000"
  onOrderCompleted?: () => void;
}

const FIXED_SERVICE_FEE_MAD = 15;
const MOROCCAN_PHONE_REGEX = /^(?:(?:\+|00)212|0)[5-7]\d{8}$/;

export const OrderForm: React.FC<OrderFormProps> = ({
  items,
  whatsappBusinessNumber = '212661000000',
  onOrderCompleted,
}) => {
  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [address, setAddress] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submittedOrderCode, setSubmittedOrderCode] = useState<string | null>(null);

  const subtotalMad = items.reduce((sum, item) => sum + item.priceMad * item.quantity, 0);
  const finalTotalMad = subtotalMad + FIXED_SERVICE_FEE_MAD;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (submittedOrderCode) return; // Duplicate submission guard

    const cleanPhone = phone.replace(/[\s-]/g, '');
    if (fullName.trim().length < 2) {
      setError('المرجو إدخال الاسم الكامل');
      return;
    }
    if (!MOROCCAN_PHONE_REGEX.test(cleanPhone)) {
      setError('رقم الهاتف المغربي غير صحيح (مثال: 0661234567)');
      return;
    }
    if (address.trim().length < 4) {
      setError('المرجو تحديد العنوان أو الحي فالجديدة');
      return;
    }

    setError(null);
    const orderCode = `JD-${'$'}{Math.floor(1000 + Math.random() * 9000)}`;

    const darijaSummary = [
      `السلام عليكم! بغيت نأكد هاد الطلبية فالجديدة:`,
      `🔖 رقم الطلب: ${'$'}{orderCode}`,
      `👤 الاسم: ${'$'}{fullName.trim()}`,
      `📱 الهاتف: ${'$'}{cleanPhone}`,
      `📍 العنوان: ${'$'}{address.trim()}`,
      `📦 السلع:`,
      ...items.map((i, idx) => `${'$'}{idx + 1}. ${'$'}{i.darijaTitle} (x${'$'}{i.quantity})`),
      `💰 المجموع النهائي الواجب أداؤه: ${'$'}{finalTotalMad} درهم`,
    ].join('\n');

    // Lock local UI into confirmation state BEFORE redirecting to WhatsApp
    setSubmittedOrderCode(orderCode);
    onOrderCompleted?.();

    const waUrl = `https://wa.me/${'$'}{whatsappBusinessNumber}?text=${'$'}{encodeURIComponent(darijaSummary)}`;
    window.location.href = waUrl;
  };

  if (submittedOrderCode) {
    return (
      <div dir="rtl" className="p-6 rounded-2xl bg-emerald-950 text-white text-center space-y-4">
        <div className="text-3xl font-bold text-amber-400">✓ تم تسجيل طلبك بنجاح</div>
        <p className="text-base">
          رقم الطلبية: <strong>{submittedOrderCode}</strong>
        </p>
        <p className="text-sm text-emerald-200">
          تم تحويلك إلى واتساب لتأكيد الطلبية. لا داعي لإعادة إرسال الاستمارة.
        </p>
      </div>
    );
  }

  return (
    <form dir="rtl" onSubmit={handleSubmit} className="space-y-4 max-w-lg mx-auto">
      <div>
        <label className="block font-bold mb-1">الاسم الكامل</label>
        <input
          type="text"
          required
          value={fullName}
          onChange={(e) => setFullName(e.target.value)}
          placeholder="مثال: أمين الجديدي"
          className="w-full rounded-xl border border-stone-300 px-4 py-3"
        />
      </div>

      <div>
        <label className="block font-bold mb-1">رقم الهاتف (06 أو 07)</label>
        <input
          type="tel"
          required
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
          placeholder="0661234567"
          className="w-full rounded-xl border border-stone-300 px-4 py-3"
        />
      </div>

      <div>
        <label className="block font-bold mb-1">العنوان فالجديدة</label>
        <input
          type="text"
          required
          value={address}
          onChange={(e) => setAddress(e.target.value)}
          placeholder="سيدي بوزيد، حي المطار، القلعة..."
          className="w-full rounded-xl border border-stone-300 px-4 py-3"
        />
      </div>

      {error && <div className="p-3 rounded-xl bg-red-100 text-red-800 text-sm font-bold">{error}</div>}

      <div className="p-4 rounded-2xl bg-[#0F2922] text-white flex items-center justify-between">
        <span className="text-sm">المجموع النهائي (شامل خدمة التوصيل):</span>
        <span className="text-2xl font-extrabold text-[#FFB300]">{finalTotalMad} درهم</span>
      </div>

      <button
        type="submit"
        className="w-full py-4 rounded-2xl bg-[#1EBE57] hover:bg-[#19a34a] text-white font-extrabold text-lg shadow-lg"
      >
        أكد الطلب عبر واتساب
      </button>
    </form>
  );
};
    """.trimIndent()

    val receiptGeneratorTsx = """
import React from 'react';

export interface ReceiptOrderItem {
  darijaTitle: string; // White-labeled Darija title ONLY (no supplier/Temu titles)
  priceMad: number;
  quantity: number;
  imageUrl: string;
}

export interface ReceiptGeneratorProps {
  orderCode: string;
  customerName: string;
  customerPhone: string;
  customerAddress: string;
  items: ReceiptOrderItem[];
  serviceFeeMad?: number;
}

/**
 * White-Labeled Single-Page A4 Receipt Generator Component.
 * - Excludes ALL original supplier branding/logos/titles (Zero Temu references).
 * - Dynamically scales and distributes product images across 1, 2, or 3 neat cards on a single A4 page.
 */
export const ReceiptGenerator: React.FC<ReceiptGeneratorProps> = ({
  orderCode,
  customerName,
  customerPhone,
  customerAddress,
  items,
  serviceFeeMad = 15,
}) => {
  const subtotalMad = items.reduce((acc, item) => acc + item.priceMad * item.quantity, 0);
  const totalMad = subtotalMad + serviceFeeMad;
  const featuredCards = items.slice(0, 3);
  const gridColsClass =
    featuredCards.length === 1
      ? 'grid-cols-1 max-w-xs mx-auto'
      : featuredCards.length === 2
      ? 'grid-cols-2'
      : 'grid-cols-3';

  return (
    <div dir="rtl" className="space-y-4">
      <div className="flex justify-end print:hidden">
        <button
          type="button"
          onClick={() => window.print()}
          className="px-5 py-2.5 rounded-xl bg-[#E65100] text-white font-bold shadow"
        >
          طباعة / تحميل توصيل A4 PDF
        </button>
      </div>

      <section
        className="mx-auto bg-white text-[#141D1A] border border-stone-300 rounded-2xl p-6 flex flex-col justify-between"
        style={{
          width: '210mm',
          height: '297mm',
          maxHeight: '297mm',
          boxSizing: 'border-box',
          pageBreakAfter: 'avoid',
          pageBreakInside: 'avoid',
          fontFamily: "'Cairo', 'Tajawal', sans-serif",
        }}
      >
        {/* White-Label Local Header */}
        <header className="bg-[#0F2922] text-white p-5 rounded-xl flex justify-between items-center">
          <div>
            <h1 className="text-xl font-extrabold">مازاغان إكسبريس • الجديدة</h1>
            <p className="text-xs text-amber-400">توصيل تسليم محلي معتمد (White-Label Delivery Receipt)</p>
          </div>
          <div className="text-left text-xs">
            <div className="font-bold text-sm">رقم التوصيل: {orderCode}</div>
            <div>التاريخ: {new Date().toLocaleDateString('ar-MA')}</div>
          </div>
        </header>

        {/* Customer Info */}
        <div className="grid grid-cols-3 gap-3 bg-stone-50 p-4 rounded-xl border border-stone-200 text-sm">
          <div><strong>الزبون:</strong> {customerName}</div>
          <div><strong>الهاتف:</strong> {customerPhone}</div>
          <div><strong>العنوان:</strong> {customerAddress}</div>
        </div>

        {/* Dynamic 1 / 2 / 3 Product Photo Cards fitted onto the same A4 page */}
        <div className={`grid gap-3 ${'$'}{gridColsClass}`}>
          {featuredCards.map((item, idx) => (
            <div
              key={idx}
              className="border border-stone-200 rounded-xl p-2.5 flex flex-col items-center bg-stone-50"
            >
              <img
                src={item.imageUrl}
                alt={item.darijaTitle}
                className={featuredCards.length === 1 ? 'h-44 object-contain' : 'h-32 object-contain'}
              />
              <span className="mt-2 text-xs font-bold text-center line-clamp-1">
                {item.darijaTitle}
              </span>
            </div>
          ))}
        </div>

        {/* Line Items Table */}
        <table className="w-full text-sm border-collapse">
          <thead>
            <tr className="border-b-2 border-stone-300 text-right">
              <th className="py-2">المنتج (بالدارجة)</th>
              <th className="py-2">الكمية</th>
              <th className="py-2 text-left">الثمن (MAD)</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item, idx) => (
              <tr key={idx} className="border-b border-stone-200">
                <td className="py-2 font-medium">{item.darijaTitle}</td>
                <td className="py-2">x{item.quantity}</td>
                <td className="py-2 text-left font-bold">{item.priceMad * item.quantity} درهم</td>
              </tr>
            ))}
          </tbody>
        </table>

        {/* Single-Page Totals Footer */}
        <footer className="bg-stone-100 p-4 rounded-xl flex justify-between items-center border border-stone-300">
          <div className="text-xs text-stone-600">
            <div>مجموع السلع الأصلي: {subtotalMad} درهم</div>
            <div>خدمة التوصيل المحلي بالجديدة: +{serviceFeeMad} درهم</div>
          </div>
          <div className="text-xl font-extrabold text-[#E65100]">
            المجموع النهائي: {totalMad} درهم
          </div>
        </footer>
      </section>
    </div>
  );
};
    """.trimIndent()

    val pwaManifestAndSw = """
// ============================================================================
// 1. public/manifest.json (100% RTL Arabic/Darija PWA Configuration)
// ============================================================================
{
  "name": "مازاغان بروكسي - وسيط تيمو بالجديدة",
  "short_name": "Mazagan Proxy",
  "description": "تقدّى من تيمو بثمنها الأصلي (0% زيادة) والتوصيل حتى لباب دارك فالجديدة",
  "lang": "ar-MA",
  "dir": "rtl",
  "start_url": "/",
  "scope": "/",
  "display": "standalone",
  "orientation": "portrait",
  "background_color": "#FAF7F2",
  "theme_color": "#0F2922",
  "icons": [
    {
      "src": "/icons/icon-192x192.png",
      "sizes": "192x192",
      "type": "image/png",
      "purpose": "any maskable"
    },
    {
      "src": "/icons/icon-512x512.png",
      "sizes": "512x512",
      "type": "image/png",
      "purpose": "any maskable"
    }
  ]
}

// ============================================================================
// 2. public/sw.js (Offline-First App Shell + Stale-While-Revalidate Catalog)
// ============================================================================
const CACHE_NAME = 'mazagan-proxy-eljadida-v1';
const APP_SHELL = ['/', '/index.html', '/manifest.json'];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(APP_SHELL))
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k)))
    )
  );
  self.clients.claim();
});

self.addEventListener('fetch', (event) => {
  if (event.request.method !== 'GET') return;
  event.respondWith(
    caches.match(event.request).then((cached) => {
      const networkFetch = fetch(event.request)
        .then((response) => {
          const clone = response.clone();
          caches.open(CACHE_NAME).then((cache) => cache.put(event.request, clone));
          return response;
        })
        .catch(() => cached);
      return cached || networkFetch;
    })
  );
});
    """.trimIndent()

    val allArtifacts = listOf(
        BlueprintArtifactFile(
            id = "structure",
            titleDarija = "هيكل المشروع (Folder Structure)",
            relativePath = "PROJECT_STRUCTURE.txt",
            language = "Tree",
            descriptionDarija = "الهيكلة الكاملة للمشروع: الباك-إند بـ Python + GitHub Actions + Supabase والفرونت-إند بـ React + Vite + TypeScript PWA.",
            content = folderStructureTree
        ),
        BlueprintArtifactFile(
            id = "supabase",
            titleDarija = "قاعدة البيانات (schema.sql)",
            relativePath = "backend/schema.sql",
            language = "SQL",
            descriptionDarija = "جداول products و orders فـ Supabase مع التحقق الإجباري من عمولة 15 درهم و0% زيادة فالثمن.",
            content = supabaseSchemaSql
        ),
        BlueprintArtifactFile(
            id = "python",
            titleDarija = "الموظف الذكي (smart_employee.py)",
            relativePath = "backend/smart_employee.py",
            language = "Python",
            descriptionDarija = "سكريبت Python كيجبد أحسن المبيعات من Temu API بالدرهم المغربي، كيترجمهم للدارجة الجديدية بـ Gemini SDK، وكيخزنهم فـ Supabase.",
            content = pythonSmartEmployeeScript
        ),
        BlueprintArtifactFile(
            id = "cron",
            titleDarija = "الكرون اليومي (daily_temu_sync.yml)",
            relativePath = ".github/workflows/daily_temu_sync.yml",
            language = "YAML",
            descriptionDarija = "ملف GitHub Actions كيخدم أوتوماتيكياً كل ليلة مع 2:00 صباحاً لتحديث السلع.",
            content = githubActionsYaml
        ),
        BlueprintArtifactFile(
            id = "modal",
            titleDarija = "نافذة الشاشة الكاملة (FullScreenModal.tsx)",
            relativePath = "frontend-pwa/src/components/FullScreenModal.tsx",
            language = "TypeScript / React",
            descriptionDarija = "نافذة 100dvh مع قفل التمرير (Body Scroll Lock) لتفادي مشاكل Safari على iOS ومنع الـ Bottom Sheets.",
            content = fullScreenModalTsx
        ),
        BlueprintArtifactFile(
            id = "orderform",
            titleDarija = "استمارة الطلب والواتساب (OrderForm.tsx)",
            relativePath = "frontend-pwa/src/components/OrderForm.tsx",
            language = "TypeScript / React",
            descriptionDarija = "استمارة من خطوة واحدة مع التحقق من الرقم المغربي، زيادة 15 درهم، التوجيه لواتساب بالدارجة، وشاشة تأكيد محلية لمنع التكرار.",
            content = orderFormTsx
        ),
        BlueprintArtifactFile(
            id = "receipt",
            titleDarija = "مولد توصيل A4 (ReceiptGenerator.tsx)",
            relativePath = "frontend-pwa/src/components/ReceiptGenerator.tsx",
            language = "TypeScript / React",
            descriptionDarija = "مكون طباعة توصيل A4 بدون أي علامة تجارية لـ Temu، مع توزيع وتصغير الصور أوتوماتيكياً (1 أو 2 أو 3 بطاقات) في صفحة واحدة.",
            content = receiptGeneratorTsx
        ),
        BlueprintArtifactFile(
            id = "pwa",
            titleDarija = "إعدادات PWA (manifest.json & sw.js)",
            relativePath = "frontend-pwa/public/manifest.json",
            language = "JSON / JS",
            descriptionDarija = "إعدادات تطبيق الويب التقدمي (PWA) باللغة العربية/الدارجة RTL مع دعم العمل بدون إنترنت وزر التثبيت.",
            content = pwaManifestAndSw
        )
    )
}
