-- ============================================================
-- CATOKAN Mobile — Seed Data
-- ============================================================
-- Seed ini aman di-run berulang karena pakai ON CONFLICT.
-- Tapi perlu konteks usaha_id — gunakan helper:
--
--   select seed_master_data((select us_usa_id from public.users_profile where user_id = auth.uid()));
-- ============================================================

create or replace function public.seed_master_data(target_usa uuid)
returns void
language plpgsql
security definer
as $$
begin
  -- Jenis ikan (8 macam, sama kayak skripsi)
  insert into public.jenis_ikan (usa_id, nama) values
    (target_usa, 'Tenggiri'),
    (target_usa, 'Gabus'),
    (target_usa, 'Kakap'),
    (target_usa, 'Patin'),
    (target_usa, 'Lele'),
    (target_usa, 'Belida'),
    (target_usa, 'Nila'),
    (target_usa, 'Tongkol')
  on conflict (usa_id, nama) do nothing;

  -- Inisialisasi stok_mentah = 0 untuk semua jenis
  insert into public.stok_mentah (usa_id, jenis_ikan_id, total_kg)
  select target_usa, j.id, 0
  from public.jenis_ikan j
  where j.usa_id = target_usa
  on conflict (usa_id, jenis_ikan_id) do nothing;

  -- Suppliers demo (Palembang, sama kayak skripsi)
  insert into public.suppliers (usa_id, nama, no_hp, alamat, catatan) values
    (target_usa, 'Supplier Ikan Segar Palembang', '081271001001', 'Jl. Demang Lebar Daun', 'Tenggiri premium'),
    (target_usa, 'Pasar Ikan 16 Ilir', '081271001002', 'Pasar 16 Ilir', 'Supplier harian'),
    (target_usa, 'Nelayan Sungai Musi', '081271001003', 'Seberang Ulu', 'Ikan sungai'),
    (target_usa, 'Jakabaring Fresh Fish', '081271001004', 'Jakabaring', 'Partai besar'),
    (target_usa, 'Agen Ikan Kertapati', '081271001005', 'Kertapati', 'Supplier cadangan')
  on conflict do nothing;

  -- Pelanggan demo
  insert into public.pelanggan (usa_id, nama, no_hp, alamat, tipe) values
    (target_usa, 'Bu Sari pempek', '082171002001', 'Ilir Barat I', 'Retail'),
    (target_usa, 'Dapur pempek Aisyah', '082171002002', 'Bukit Kecil', 'Grosir'),
    (target_usa, 'Warung Model Pak Rudi', '082171002003', 'Sukarami', 'Retail'),
    (target_usa, 'Rumah Makan Musi Jaya', '082171002004', 'Seberang Ulu I', 'Grosir'),
    (target_usa, 'Pelanggan Umum', '082171002005', 'Palembang', 'Retail')
  on conflict do nothing;
end $$;

-- Cara panggil setelah register user pertama:
--   select public.seed_master_data((select usa_id from public.users_profile where user_id = auth.uid()));
-- (Dipanggil otomatis dari app setelah login pertama)