package com.bagas.stokikan.service;

import com.bagas.stokikan.db.Database;
import com.bagas.stokikan.model.OptionItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MasterDataService {
    public List<OptionItem> jenisIkan() {
        return options("SELECT id, nama FROM jenis_ikan WHERE aktif=1 ORDER BY nama");
    }

    public List<OptionItem> suppliers() {
        return options("SELECT id, nama FROM suppliers ORDER BY nama");
    }

    public List<OptionItem> pelanggan() {
        return options("SELECT id, nama FROM pelanggan ORDER BY nama");
    }

    public List<OptionItem> batchGiling() {
        return options("SELECT id, batch_no || ' - ' || (SELECT nama FROM jenis_ikan WHERE id=stok_giling.jenis_ikan_id) || ' (' || total_kg || ' kg)' AS nama FROM stok_giling WHERE total_kg>0 ORDER BY id DESC");
    }

    private List<OptionItem> options(String sql) {
        List<OptionItem> list = new ArrayList<>();
        for (Map<String, Object> row : Database.query(sql)) {
            list.add(new OptionItem(((Number) row.get("id")).intValue(), String.valueOf(row.get("nama"))));
        }
        return list;
    }
}
