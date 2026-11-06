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

create policy "Allow public read of active products"
    on public.products for select
    using (is_active = true);

create policy "Allow public insert of proxy orders"
    on public.orders for insert
    with check (service_fee_mad = 15.00 and total_mad = subtotal_mad + 15.00);
