-- ============================================================
-- CATOKAN Mobile — Schema Postgres (Supabase)
-- ============================================================
-- Jalankan di Supabase SQL Editor.
-- Aman di-run berulang (idempotent).
-- ============================================================

-- 1. Tabel usaha (tenant)
create table if not exists public.usa (
  id uuid primary key default gen_random_uuid(),
  nama_usa text not null,
  created_by uuid references auth.users(id) default auth.uid(),
  created_at timestamptz default now()
);

-- 2. Profil pengguna (linked ke auth.users + usaha)
do $$ begin
  if not exists (select 1 from pg_type where typname = 'user_role') then
    create type public.user_role as enum ('admin', 'user');
  end if;
end $$;

create table if not exists public.users_profile (
  user_id uuid primary key references auth.users(id) on delete cascade,
  usa_id uuid not null references public.usa(id) on delete cascade,
  nama text not null,
  role public.user_role not null default 'user',
  created_at timestamptz default now()
);

-- 3. Master: jenis_ikan
create table if not exists public.jenis_ikan (
  id uuid primary key default gen_random_uuid(),
  usa_id uuid not null references public.usa(id) on delete cascade,
  nama text not null,
  aktif boolean default true,
  created_at timestamptz default now(),
  updated_at timestamptz default now(),
  unique (usa_id, nama)
);

-- 4. Master: suppliers
create table if not exists public.suppliers (
  id uuid primary key default gen_random_uuid(),
  usa_id uuid not null references public.usa(id) on delete cascade,
  nama text not null,
  no_hp text,
  alamat text,
  catatan text,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

-- 5. Master: pelanggan
create table if not exists public.pelanggan (
  id uuid primary key default gen_random_uuid(),
  usa_id uuid not null references public.usa(id) on delete cascade,
  nama text not null,
  no_hp text,
  alamat text,
  tipe text default 'Retail',
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

-- 6. Stok mentah (running total per jenis_ikan)
create table if not exists public.stok_mentah (
  id uuid primary key default gen_random_uuid(),
  usa_id uuid not null references public.usa(id) on delete cascade,
  jenis_ikan_id uuid not null references public.jenis_ikan(id) on delete cascade,
  total_kg numeric(12,2) not null default 0,
  updated_at timestamptz default now(),
  unique (usa_id, jenis_ikan_id)
);

-- 7. Riwayat stok (audit trail, sama kayak skripsi)
create table if not exists public.riwayat_stok (
  id uuid primary key default gen_random_uuid(),
  usa_id uuid not null references public.usa(id) on delete cascade,
  tanggal timestamptz default now(),
  jenis_ikan_id uuid references public.jenis_ikan(id) on delete set null,
  jenis_transaksi text not null,
  jenis_stok text not null,
  referensi text,
  perubahan_kg numeric(12,2) not null,
  stok_sebelum numeric(12,2) not null,
  stok_sesudah numeric(12,2) not null,
  keterangan text
);
create index if not exists idx_riwayat_us_ago on public.riwayat_stok (usa_id, tanggal desc);

-- 8. Stok masuk (pembelian ikan mentah)
do $$ begin
  if not exists (select 1 from pg_type where typname = 'status_bayar') then
    create type public.status_bayar as enum ('LUNAS', 'UTANG');
  end if;
end $$;

create table if not exists public.stok_masuk (
  id uuid primary key default gen_random_uuid(),
  usa_id uuid not null references public.usa(id) on delete cascade,
  tanggal date not null default current_date,
  jenis_ikan_id uuid not null references public.jenis_ikan(id) on delete restrict,
  supplier_id uuid not null references public.suppliers(id) on delete restrict,
  berat_kg numeric(12,2) not null check (berat_kg > 0),
  harga_beli_perkg numeric(12,2) not null check (harga_beli_perkg > 0),
  total_beli numeric(12,2) generated always as (berat_kg * harga_beli_perkg) stored,
  status_bayar public.status_bayar not null default 'LUNAS',
  catatan text,
  created_by uuid references auth.users(id) default auth.uid(),
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);
create index if not exists idx_stok_masuk_us_ago on public.stok_masuk (usa_id, tanggal desc);

-- 9. Piutang supplier (mutasi: TAMBAH utang dari stok_masuk, BAYAR dari cicilan)
create table if not exists public.piutang_supplier (
  id uuid primary key default gen_random_uuid(),
  usa_id uuid not null references public.usa(id) on delete cascade,
  supplier_id uuid not null references public.suppliers(id) on delete cascade,
  tanggal date not null default current_date,
  jenis text not null check (jenis in ('TAMBAH', 'BAYAR')),
  nominal numeric(12,2) not null check (nominal > 0),
  stok_masuk_id uuid references public.stok_masuk(id) on delete set null,
  catatan text,
  created_by uuid references auth.users(id) default auth.uid(),
  created_at timestamptz default now()
);
create index if not exists idx_piutang_sup_us_ago on public.piutang_supplier (usa_id, supplier_id, tanggal desc);

-- 10. Stok giling / produksi (gabung: saat input produksi langsung jadi row stok)
create table if not exists public.stok_giling (
  id uuid primary key default gen_random_uuid(),
  usa_id uuid not null references public.usa(id) on delete cascade,
  batch_no text not null,
  jenis_ikan_id uuid not null references public.jenis_ikan(id) on delete restrict,
  berat_mentah_kg numeric(12,2) not null check (berat_mentah_kg > 0),
  berat_hasil_kg numeric(12,2) not null check (berat_hasil_kg > 0),
  penyusutan_kg numeric(12,2) generated always as (berat_mentah_kg - berat_hasil_kg) stored,
  biaya_produksi numeric(12,2) default 0,
  harga_jual_perkg numeric(12,2) not null check (harga_jual_perkg > 0),
  tanggal_produksi date not null default current_date,
  jenis_batch text default 'FULL' check (jenis_batch in ('FULL', 'CAMPUR')),
  sisa_kg numeric(12,2) not null,
  status text default 'TERSEDIA' check (status in ('TERSEDIA', 'HABIS')),
  created_by uuid references auth.users(id) default auth.uid(),
  created_at timestamptz default now(),
  updated_at timestamptz default now(),
  unique (usa_id, batch_no)
);
create index if not exists idx_stok_giling_fifo on public.stok_giling (usa_id, jenis_ikan_id, tanggal_produksi, id) where status = 'TERSEDIA';

-- 11. Penjualan (header)
create table if not exists public.penjualan (
  id uuid primary key default gen_random_uuid(),
  usa_id uuid not null references public.usa(id) on delete cascade,
  nomor_transaksi text not null,
  tanggal date not null default current_date,
  pelanggan_id uuid references public.pelanggan(id) on delete set null,
  user_id uuid references auth.users(id) default auth.uid(),
  total numeric(12,2) not null default 0,
  status_bayar public.status_bayar not null default 'LUNAS',
  catatan text,
  created_at timestamptz default now(),
  updated_at timestamptz default now(),
  unique (usa_id, nomor_transaksi)
);
create index if not exists idx_penjualan_us_ago on public.penjualan (usa_id, tanggal desc);

-- 12. Detail penjualan (item per batch giling)
create table if not exists public.detail_penjualan (
  id uuid primary key default gen_random_uuid(),
  usa_id uuid not null references public.usa(id) on delete cascade,
  penjualan_id uuid not null references public.penjualan(id) on delete cascade,
  stok_giling_id uuid not null references public.stok_giling(id) on delete restrict,
  jenis_ikan_id uuid not null references public.jenis_ikan(id) on delete restrict,
  jumlah_kg numeric(12,2) not null check (jumlah_kg > 0),
  harga_perkg numeric(12,2) not null,
  subtotal numeric(12,2) generated always as (jumlah_kg * harga_perkg) stored
);
create index if not exists idx_detail_penjualan_penjualan on public.detail_penjualan (penjualan_id);

-- 13. Pembayaran penjualan (cicilan piutang pelanggan)
create table if not exists public.pembayaran_penjualan (
  id uuid primary key default gen_random_uuid(),
  usa_id uuid not null references public.usa(id) on delete cascade,
  penjualan_id uuid not null references public.penjualan(id) on delete cascade,
  tanggal date not null default current_date,
  nominal numeric(12,2) not null check (nominal > 0),
  metode text default 'Tunai',
  catatan text,
  created_by uuid references auth.users(id) default auth.uid(),
  created_at timestamptz default now()
);
create index if not exists idx_pembayaran_penjualan_penj on public.pembayaran_penjualan (penjualan_id);

-- 14. Koreksi stok (audit koreksi sistem vs fisik)
create table if not exists public.koreksi_stok (
  id uuid primary key default gen_random_uuid(),
  usa_id uuid not null references public.usa(id) on delete cascade,
  tanggal timestamptz default now(),
  jenis_stok text not null check (jenis_stok in ('MENTAH', 'GILING')),
  referensi_id uuid not null,
  stok_sistem numeric(12,2) not null,
  stok_fisik numeric(12,2) not null,
  selisih numeric(12,2) generated always as (stok_fisik - stok_sistem) stored,
  alasan text not null,
  created_by uuid references auth.users(id) default auth.uid()
);

-- ============================================================
-- Row Level Security
-- ============================================================
alter table public.usa enable row level security;
alter table public.users_profile enable row level security;
alter table public.jenis_ikan enable row level security;
alter table public.suppliers enable row level security;
alter table public.pelanggan enable row level security;
alter table public.stok_mentah enable row level security;
alter table public.riwayat_stok enable row level security;
alter table public.stok_masuk enable row level security;
alter table public.piutang_supplier enable row level security;
alter table public.stok_giling enable row level security;
alter table public.penjualan enable row level security;
alter table public.detail_penjualan enable row level security;
alter table public.pembayaran_penjualan enable row level security;
alter table public.koreksi_stok enable row level security;

-- Helper: usaha_id dari user yang login
create or replace function public.current_usa_id()
returns uuid
language sql stable
security definer
as $$
  select usa_id
  from public.users_profile
  where user_id = auth.uid()
  limit 1;
$$;

-- Drop existing policies (idempotent)
do $$
declare r record;
begin
  for r in (
    select policyname, tablename from pg_policies
    where schemaname = 'public'
      and tablename in ('usa','users_profile','jenis_ikan','suppliers','pelanggan',
                        'stok_mentah','riwayat_stok','stok_masuk','piutang_supplier',
                        'stok_giling','penjualan','detail_penjualan','pembayaran_penjualan',
                        'koreksi_stok')
  ) loop
    execute format('drop policy if exists %I on public.%I', r.policyname, r.tablename);
  end loop;
end $$;

-- Policy: setiap user hanya akses data usaha-nya sendiri
-- usaha
create policy "own_usaha_select" on public.usa for select using (
  id in (select usa_id from public.users_profile where user_id = auth.uid())
);
create policy "own_usaha_insert" on public.usa for insert with check (created_by = auth.uid());
create policy "own_usaha_update" on public.usa for update using (
  id in (select usa_id from public.users_profile where user_id = auth.uid())
);

-- users_profile
create policy "own_users_profile_all" on public.users_profile for all using (
  usa_id in (select usa_id from public.users_profile where user_id = auth.uid())
) with check (
  usa_id in (select usa_id from public.users_profile where user_id = auth.uid())
);

-- Generic policy maker for tables with usa_id
do $$
declare tbl text;
begin
  foreach tbl in array array[
    'jenis_ikan','suppliers','pelanggan','stok_mentah','riwayat_stok',
    'stok_masuk','piutang_supplier','stok_giling','penjualan',
    'detail_penjualan','pembayaran_penjualan','koreksi_stok'
  ]
  loop
    execute format($f$
      create policy "own_select" on public.%I for select using (usa_id = public.current_usa_id());
      create policy "own_insert" on public.%I for insert with check (usa_id = public.current_usa_id());
      create policy "own_update" on public.%I for update using (usa_id = public.current_usa_id()) with check (usa_id = public.current_usa_id());
      create policy "own_delete" on public.%I for delete using (usa_id = public.current_usa_id());
    $f$, tbl, tbl, tbl, tbl);
  end loop;
end $$;

-- ============================================================
-- Trigger: auto-update updated_at
-- ============================================================
create or replace function public.set_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end $$;

do $$
declare tbl text;
begin
  foreach tbl in array array['jenis_ikan','suppliers','pelanggan','stok_mentah','stok_masuk','stok_giling','penjualan']
  loop
    execute format($f$
      drop trigger if exists trg_set_updated_at on public.%I;
      create trigger trg_set_updated_at before update on public.%I
        for each row execute function public.set_updated_at();
    $f$, tbl, tbl);
  end loop;
end $$;

-- ============================================================
-- Trigger: auto-create users_profile saat user baru daftar
-- Dipanggil dari app via RPC register_user
-- ============================================================
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer as $$
declare
  new_usa_id uuid;
  new_numa text;
begin
  new_numa := coalesce(new.raw_user_meta_data->>'nama', split_part(new.email, '@', 1));
  -- Buat usaha baru untuk user pertama kali
  insert into public.usa (nama_usa, created_by)
  values (new_numa || ' - Usaha', new.id)
  returning id into new_usa_id;

  insert into public.users_profile (user_id, usa_id, nama, role)
  values (new.id, new_usa_id, new_numa, 'admin');

  return new;
end $$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();