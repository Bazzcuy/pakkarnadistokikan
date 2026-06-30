package com.bagas.stokikan.service;

import com.bagas.stokikan.db.Database;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.List;
import java.util.Map;

public class ExcelService {
    private final StockService stockService = new StockService();
    private static final List<String> BACKUP_TABLES = List.of(
            "users", "jenis_ikan", "suppliers", "pelanggan", "stok_mentah", "stok_giling",
            "stok_masuk", "produksi_giling", "penjualan", "detail_penjualan", "pembayaran", "penyesuaian_stok", "riwayat_stok"
    );

    public void exportReport(Path file) {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle header = headerStyle(wb);
            CellStyle money = wb.createCellStyle();
            money.setDataFormat(wb.createDataFormat().getFormat("\"Rp\" #,##0"));
            writeKeyValueSheet(wb, "Ringkasan", List.of(
                    new String[]{"Total Stok Mentah (kg)", scalar("SELECT IFNULL(SUM(total_kg),0) AS v FROM stok_mentah")},
                    new String[]{"Total Stok Giling (kg)", scalar("SELECT IFNULL(SUM(total_kg),0) AS v FROM stok_giling")},
                    new String[]{"Total Penjualan Lunas", scalar("SELECT IFNULL(SUM(total),0) AS v FROM penjualan")},
                    new String[]{"Stok Perlu Dijual Dulu (kg)", scalar("SELECT IFNULL(SUM(total_kg),0) AS v FROM stok_giling WHERE total_kg>0 AND date(tanggal_produksi)<=date('now','-5 day')")}
            ), header);
            writeTable(wb, "Stok Mentah", header, Database.query("SELECT j.nama AS jenis_ikan, s.total_kg, s.updated_at FROM stok_mentah s JOIN jenis_ikan j ON j.id=s.jenis_ikan_id ORDER BY j.nama"));
            writeTable(wb, "Stok Giling", header, Database.query("SELECT g.batch_no, j.nama AS jenis_ikan, g.total_kg, g.harga_jual_per_kg, g.tanggal_produksi, g.status_stok FROM stok_giling g JOIN jenis_ikan j ON j.id=g.jenis_ikan_id ORDER BY g.id DESC"));
            writeTable(wb, "Penjualan", header, Database.query("SELECT p.nomor_transaksi, p.tanggal, pl.nama AS pelanggan, u.nama AS kasir, p.total, p.status_pembayaran FROM penjualan p LEFT JOIN pelanggan pl ON pl.id=p.pelanggan_id LEFT JOIN users u ON u.id=p.kasir_id ORDER BY p.id DESC"));
            writeTable(wb, "Riwayat Stok", header, Database.query("SELECT tanggal,jenis_transaksi,jenis_stok,referensi,perubahan_kg,stok_sebelum,stok_sesudah,keterangan FROM riwayat_stok ORDER BY id DESC"));
            save(wb, file);
        } catch (Exception e) {
            throw new RuntimeException("Gagal export laporan Excel: " + e.getMessage(), e);
        }
    }

    public void exportStockImportTemplate(Path file) {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle header = headerStyle(wb);
            Sheet input = wb.createSheet("Input Stok Masuk");
            String[] columns = {"Jenis Ikan", "Supplier", "Berat Kg", "Harga Beli per Kg", "Catatan"};
            Row head = input.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(header);
                input.setColumnWidth(i, 22 * 256);
            }
            writeTable(wb, "Referensi Jenis", header, Database.query("SELECT nama FROM jenis_ikan WHERE aktif=1 ORDER BY nama"));
            writeTable(wb, "Referensi Supplier", header, Database.query("SELECT nama FROM suppliers ORDER BY nama"));
            save(wb, file);
        } catch (Exception e) {
            throw new RuntimeException("Gagal membuat template Excel: " + e.getMessage(), e);
        }
    }

    public int importStockIn(Path file) {
        int count = 0;
        try (InputStream in = Files.newInputStream(file); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String jenis = text(row.getCell(0));
                String supplier = text(row.getCell(1));
                if (jenis.isBlank() && supplier.isBlank()) continue;
                double berat = number(row.getCell(2));
                double harga = number(row.getCell(3));
                String catatan = text(row.getCell(4));
                int jenisId = lookupId("jenis_ikan", jenis);
                int supplierId = lookupId("suppliers", supplier);
                stockService.inputStokMentah(jenisId, supplierId, berat, harga, catatan);
                count++;
            }
            return count;
        } catch (Exception e) {
            throw new RuntimeException("Gagal import stok dari Excel: " + e.getMessage(), e);
        }
    }

    public void exportBackup(Path file) {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle header = headerStyle(wb);
            for (String table : BACKUP_TABLES) writeBackupTable(wb, table, header);
            save(wb, file);
        } catch (Exception e) {
            throw new RuntimeException("Gagal export data aplikasi: " + e.getMessage(), e);
        }
    }

    public int importBackup(Path file) {
        try (InputStream in = Files.newInputStream(file); Workbook wb = WorkbookFactory.create(in); Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                int rows = 0;
                for (int i = BACKUP_TABLES.size() - 1; i >= 0; i--) Database.execute(c, "DELETE FROM " + BACKUP_TABLES.get(i));
                for (String table : BACKUP_TABLES) rows += importBackupTable(c, wb, table);
                c.commit();
                return rows;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException("Gagal import data aplikasi: " + e.getMessage(), e);
        }
    }

    private int lookupId(String table, String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Nama data master kosong");
        var rows = Database.query("SELECT id FROM " + table + " WHERE lower(nama)=lower(?)", name.trim());
        if (rows.isEmpty()) throw new IllegalArgumentException("Data master tidak ditemukan: " + name);
        return ((Number) rows.get(0).get("id")).intValue();
    }

    private String scalar(String sql) {
        Object value = Database.query(sql).get(0).get("v");
        return String.valueOf(value);
    }

    private void writeKeyValueSheet(Workbook wb, String name, List<String[]> rows, CellStyle header) {
        Sheet sheet = wb.createSheet(name);
        Row h = sheet.createRow(0);
        h.createCell(0).setCellValue("Metrik");
        h.createCell(1).setCellValue("Nilai");
        h.getCell(0).setCellStyle(header);
        h.getCell(1).setCellStyle(header);
        for (int i = 0; i < rows.size(); i++) {
            Row r = sheet.createRow(i + 1);
            r.createCell(0).setCellValue(rows.get(i)[0]);
            r.createCell(1).setCellValue(rows.get(i)[1]);
        }
        sheet.setColumnWidth(0, 30 * 256);
        sheet.setColumnWidth(1, 22 * 256);
    }

    private void writeTable(Workbook wb, String name, CellStyle header, List<Map<String, Object>> rows) {
        Sheet sheet = wb.createSheet(name);
        if (rows.isEmpty()) return;
        Row h = sheet.createRow(0);
        var keys = rows.get(0).keySet().toArray(new String[0]);
        for (int i = 0; i < keys.length; i++) {
            Cell cell = h.createCell(i);
            cell.setCellValue(keys[i].replace('_', ' ').toUpperCase());
            cell.setCellStyle(header);
            sheet.setColumnWidth(i, 22 * 256);
        }
        for (int r = 0; r < rows.size(); r++) {
            Row row = sheet.createRow(r + 1);
            for (int c = 0; c < keys.length; c++) {
                Object value = rows.get(r).get(keys[c]);
                if (value instanceof Number number) row.createCell(c).setCellValue(number.doubleValue());
                else row.createCell(c).setCellValue(value == null ? "" : String.valueOf(value));
            }
        }
    }

    private void writeBackupTable(Workbook wb, String table, CellStyle header) throws Exception {
        Sheet sheet = wb.createSheet(table);
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM " + table);
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData md = rs.getMetaData();
            Row h = sheet.createRow(0);
            for (int i = 1; i <= md.getColumnCount(); i++) {
                Cell cell = h.createCell(i - 1);
                cell.setCellValue(md.getColumnName(i));
                cell.setCellStyle(header);
                sheet.setColumnWidth(i - 1, 20 * 256);
            }
            int rowIndex = 1;
            while (rs.next()) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 1; i <= md.getColumnCount(); i++) {
                    Object value = rs.getObject(i);
                    if (value instanceof Number number) row.createCell(i - 1).setCellValue(number.doubleValue());
                    else row.createCell(i - 1).setCellValue(value == null ? "" : String.valueOf(value));
                }
            }
        }
    }

    private int importBackupTable(Connection c, Workbook wb, String table) throws Exception {
        Sheet sheet = wb.getSheet(table);
        if (sheet == null) throw new IllegalArgumentException("Sheet backup tidak lengkap: " + table);
        Row header = sheet.getRow(0);
        if (header == null) return 0;
        int cols = header.getLastCellNum();
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(table).append("(");
        StringBuilder marks = new StringBuilder();
        for (int i = 0; i < cols; i++) {
            if (i > 0) {
                sql.append(",");
                marks.append(",");
            }
            sql.append(text(header.getCell(i)));
            marks.append("?");
        }
        sql.append(") VALUES(").append(marks).append(")");
        int count = 0;
        try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                boolean empty = true;
                for (int col = 0; col < cols; col++) if (!text(row.getCell(col)).isBlank()) empty = false;
                if (empty) continue;
                for (int col = 0; col < cols; col++) {
                    Cell cell = row.getCell(col);
                    if (cell == null || text(cell).isBlank()) ps.setObject(col + 1, null);
                    else if (cell.getCellType() == CellType.NUMERIC) ps.setDouble(col + 1, cell.getNumericCellValue());
                    else ps.setString(col + 1, text(cell));
                }
                ps.executeUpdate();
                count++;
            }
        }
        return count;
    }

    private CellStyle headerStyle(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private String text(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf(cell.getNumericCellValue());
        if (cell.getCellType() == CellType.BOOLEAN) return String.valueOf(cell.getBooleanCellValue());
        return cell.toString().trim();
    }

    private double number(Cell cell) {
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
        String value = cell.toString().trim().replace(",", ".");
        return value.isBlank() ? 0 : Double.parseDouble(value);
    }

    private void save(Workbook wb, Path file) throws Exception {
        try (OutputStream out = Files.newOutputStream(file)) {
            wb.write(out);
        }
    }
}
