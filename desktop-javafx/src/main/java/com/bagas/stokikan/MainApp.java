package com.bagas.stokikan;

import com.bagas.stokikan.db.Database;
import com.bagas.stokikan.model.OptionItem;
import com.bagas.stokikan.model.User;
import com.bagas.stokikan.service.*;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainApp extends Application {
    private final AuthService authService = new AuthService();
    private final MasterDataService masterService = new MasterDataService();
    private final StockService stockService = new StockService();
    private final ProductionService productionService = new ProductionService();
    private final SalesService salesService = new SalesService();
    private final ExcelService excelService = new ExcelService();
    private User currentUser;
    private BorderPane root;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Database.initialize();
        stage.setTitle("CATOKAN - Stok Ikan Giling");
        stage.setScene(loginScene(stage));
        stage.show();
    }

    private Scene loginScene(Stage stage) {
        VBox outer = new VBox(16);
        outer.setPadding(new Insets(24));
        outer.setAlignment(Pos.TOP_CENTER);
        outer.getStyleClass().add("login-root");

        ImageView banner = image("/images/catokan_banner.png", 720, 200);
        HBox brand = new HBox(14, image("/images/catokan_logo.png", 78, 78), vbox(title("CATOKAN"), sub("Catat Stok Ikan - satu akun pengguna, semua fitur operasional")));
        brand.getStyleClass().add("card");
        brand.setAlignment(Pos.CENTER_LEFT);

        TabPane tabs = new TabPane();
        tabs.setMaxWidth(560);
        tabs.getTabs().add(new Tab("Login", loginForm(stage)));
        tabs.getTabs().add(new Tab("Daftar Akun", registerForm(stage)));
        tabs.getTabs().forEach(t -> t.setClosable(false));

        outer.getChildren().addAll(banner, brand, tabs);
        Scene scene = new Scene(outer, 900, 780);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        return scene;
    }

    private VBox loginForm(Stage stage) {
        TextField username = field("Username");
        username.setText("pengguna");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        password.setText("pengguna123");
        Button login = primary("Masuk");
        login.setMaxWidth(Double.MAX_VALUE);
        login.setOnAction(e -> {
            currentUser = authService.login(username.getText(), password.getText());
            if (currentUser == null) alert("Login gagal", "Username atau password salah.");
            else stage.setScene(appScene(stage));
        });
        TextArea akun = area("AKUN AWAL\n\npengguna / pengguna123");
        akun.setPrefRowCount(3);
        return form(title("Masuk Pengguna"), sub("Gunakan akun awal atau daftar akun baru."), akun, new Label("Username"), username, new Label("Password"), password, login);
    }

    private VBox registerForm(Stage stage) {
        TextField nama = field("Nama pengguna");
        TextField username = field("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password minimal 6 karakter");
        TextField usaha = field("Nama usaha");
        TextField hp = field("Nomor HP");
        TextField alamat = field("Alamat");
        Button daftar = primary("Daftar dan Masuk");
        daftar.setMaxWidth(Double.MAX_VALUE);
        daftar.setOnAction(e -> {
            try {
                currentUser = authService.register(nama.getText(), username.getText(), password.getText(), usaha.getText(), hp.getText(), alamat.getText());
                stage.setScene(appScene(stage));
            } catch (Exception ex) {
                alert("Gagal daftar", ex.getMessage());
            }
        });
        return form(title("Daftar Akun Pengguna"), sub("Satu akun bisa mengakses semua fitur stok, produksi, penjualan, dan laporan."), nama, username, password, usaha, hp, alamat, daftar);
    }

    private Scene appScene(Stage stage) {
        root = new BorderPane();
        root.setPadding(new Insets(14));
        VBox menu = new VBox(10);
        menu.setPadding(new Insets(14));
        menu.setPrefWidth(260);
        menu.getStyleClass().add("card");

        Button akun = secondary("Akun / Profil");
        akun.setMaxWidth(Double.MAX_VALUE);
        akun.setOnAction(e -> setCenter(profileView()));
        HBox brand = new HBox(10, image("/images/catokan_logo.png", 48, 48), vbox(title("CATOKAN"), sub(currentUser.getNama()), akun));
        brand.setAlignment(Pos.CENTER_LEFT);
        Button dashboard = nav("Dashboard", () -> setCenter(dashboardView()));
        Button jenis = nav("Kelola Jenis Ikan", () -> setCenter(fishMasterView(stage)));
        Button supplier = nav("Data Supplier", () -> setCenter(supplierView()));
        Button pelanggan = nav("Data Pelanggan", () -> setCenter(customerView()));
        Button stokMentah = nav("Stok Mentah", () -> setCenter(rawStockView(stage)));
        Button produksi = nav("Produksi Giling", () -> setCenter(productionView()));
        Button stokGiling = nav("Stok Giling", () -> setCenter(milledStockView()));
        Button penjualan = nav("Penjualan", () -> setCenter(salesView()));
        Button kontrol = nav("Kontrol Stok", () -> setCenter(controlView()));
        Button riwayat = nav("Riwayat", () -> setCenter(historyView()));
        Button laporan = nav("Laporan & Excel", () -> setCenter(reportView(stage)));
        Button logout = nav("Logout", () -> stage.setScene(loginScene(stage)));
        menu.getChildren().addAll(brand, dashboard, jenis, supplier, pelanggan, stokMentah, produksi, stokGiling, penjualan, kontrol, riwayat, laporan, logout);

        root.setLeft(menu);
        root.setCenter(dashboardView());
        Scene scene = new Scene(root, 1240, 780);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        return scene;
    }

    private VBox dashboardView() {
        HBox stats = new HBox(12,
                stat("Stok Mentah", scalar("SELECT IFNULL(SUM(total_kg),0) FROM stok_mentah") + " kg"),
                stat("Stok Giling", scalar("SELECT IFNULL(SUM(total_kg),0) FROM stok_giling") + " kg"),
                stat("Penjualan", "Rp " + scalar("SELECT IFNULL(SUM(total),0) FROM penjualan")),
                stat("Stok Lama", scalar("SELECT IFNULL(SUM(total_kg),0) FROM stok_giling WHERE total_kg>0 AND date(tanggal_produksi)<=date('now','-5 day')") + " kg"));
        VBox chart = stockChart();
        GridPane grid = fishCards(Database.query("SELECT j.nama,j.kategori,j.gambar_path,IFNULL(sm.total_kg,0) AS mentah,IFNULL(SUM(sg.total_kg),0) AS giling FROM jenis_ikan j LEFT JOIN stok_mentah sm ON sm.jenis_ikan_id=j.id LEFT JOIN stok_giling sg ON sg.jenis_ikan_id=j.id GROUP BY j.id ORDER BY j.nama"));
        return page("Dashboard Stok", sub("Ringkasan stok, stok yang perlu dijual lebih dulu, grafik stok giling, dan produk per jenis ikan."), stats, chart, scroll(grid));
    }

    private VBox profileView() {
        Map<String, Object> p = authService.profile(currentUser.getId());
        TextField nama = field("Nama");
        nama.setText(value(p, "nama"));
        TextField usaha = field("Nama usaha");
        usaha.setText(value(p, "nama_usaha"));
        TextField hp = field("Nomor HP");
        hp.setText(value(p, "nomor_hp"));
        TextField alamat = field("Alamat");
        alamat.setText(value(p, "alamat"));
        Button save = primary("Simpan Profil");
        save.setOnAction(e -> {
            try {
                authService.updateProfile(currentUser.getId(), nama.getText(), usaha.getText(), hp.getText(), alamat.getText());
                currentUser = new User(currentUser.getId(), nama.getText(), currentUser.getUsername(), "PENGGUNA");
                alert("Berhasil", "Profil pengguna tersimpan.");
            } catch (Exception ex) {
                alert("Gagal", ex.getMessage());
            }
        });
        return page("Profil Pengguna", sub("Informasi ini dipakai sebagai identitas usaha di aplikasi."), form(nama, usaha, hp, alamat, save));
    }

    private VBox fishMasterView(Stage stage) {
        TextField nama = field("Nama jenis ikan");
        TextField kategori = field("Kategori");
        TextField desc = field("Deskripsi");
        TextField gambar = field("Path gambar");
        Button pilih = secondary("Pilih Gambar");
        pilih.setOnAction(e -> {
            File file = openImage(stage);
            if (file != null) gambar.setText(file.getAbsolutePath());
        });
        Button tambah = primary("Tambah Jenis Ikan");
        tambah.setOnAction(e -> {
            try {
                masterService.tambahJenisIkan(nama.getText(), kategori.getText(), desc.getText(), gambar.getText());
                setCenter(fishMasterView(stage));
            } catch (Exception ex) {
                alert("Gagal", ex.getMessage());
            }
        });
        TableView<Map<String, Object>> table = table(Database.query("SELECT nama,kategori,deskripsi,gambar_path FROM jenis_ikan WHERE aktif=1 ORDER BY nama"));
        HBox wrap = new HBox(12, form(title("Input Jenis Ikan"), nama, kategori, desc, gambar, pilih, tambah), table);
        HBox.setHgrow(table, Priority.ALWAYS);
        return page("Jenis Ikan", sub("Kelola master ikan dan gambar produk."), wrap);
    }

    private VBox supplierView() {
        TextField id = field("ID");
        id.setEditable(false);
        TextField nama = field("Nama supplier");
        TextField hp = field("Nomor HP");
        TextField alamat = field("Alamat");
        TextField catatan = field("Catatan");
        TableView<Map<String, Object>> table = table(Database.query("SELECT id,nama,nomor_hp,alamat,catatan FROM suppliers ORDER BY nama"));
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> {
            if (row == null) return;
            id.setText(value(row, "id"));
            nama.setText(value(row, "nama"));
            hp.setText(value(row, "nomor_hp"));
            alamat.setText(value(row, "alamat"));
            catatan.setText(value(row, "catatan"));
        });
        Button baru = secondary("Input Baru");
        baru.setOnAction(e -> clear(id, nama, hp, alamat, catatan));
        Button simpan = primary("Simpan Supplier");
        simpan.setOnAction(e -> {
            try {
                masterService.simpanSupplier(id.getText().isBlank() ? null : Integer.parseInt(id.getText()), nama.getText(), hp.getText(), alamat.getText(), catatan.getText());
                setCenter(supplierView());
            } catch (Exception ex) {
                alert("Gagal", ex.getMessage());
            }
        });
        Button hapus = secondary("Hapus Supplier");
        hapus.setOnAction(e -> {
            try {
                if (id.getText().isBlank()) throw new IllegalArgumentException("Pilih supplier dari tabel dulu.");
                masterService.hapusSupplier(Integer.parseInt(id.getText()));
                setCenter(supplierView());
            } catch (Exception ex) {
                alert("Gagal", ex.getMessage());
            }
        });
        HBox wrap = new HBox(12, form(title("Kelola Supplier"), id, nama, hp, alamat, catatan, new HBox(8, baru, simpan, hapus)), table);
        HBox.setHgrow(table, Priority.ALWAYS);
        return page("Supplier", sub("Supplier adalah master pemasok ikan mentah yang dipakai saat input stok masuk."), wrap);
    }

    private VBox customerView() {
        TextField id = field("ID");
        id.setEditable(false);
        TextField nama = field("Nama pelanggan");
        TextField hp = field("Nomor HP");
        TextField alamat = field("Alamat");
        TextField tipe = field("Tipe pelanggan");
        TableView<Map<String, Object>> table = table(Database.query("SELECT id,nama,nomor_hp,alamat,tipe_pelanggan FROM pelanggan ORDER BY nama"));
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> {
            if (row == null) return;
            id.setText(value(row, "id"));
            nama.setText(value(row, "nama"));
            hp.setText(value(row, "nomor_hp"));
            alamat.setText(value(row, "alamat"));
            tipe.setText(value(row, "tipe_pelanggan"));
        });
        Button baru = secondary("Input Baru");
        baru.setOnAction(e -> clear(id, nama, hp, alamat, tipe));
        Button simpan = primary("Simpan Pelanggan");
        simpan.setOnAction(e -> {
            try {
                masterService.simpanPelanggan(id.getText().isBlank() ? null : Integer.parseInt(id.getText()), nama.getText(), hp.getText(), alamat.getText(), tipe.getText());
                setCenter(customerView());
            } catch (Exception ex) {
                alert("Gagal", ex.getMessage());
            }
        });
        Button hapus = secondary("Hapus Pelanggan");
        hapus.setOnAction(e -> {
            try {
                if (id.getText().isBlank()) throw new IllegalArgumentException("Pilih pelanggan dari tabel dulu.");
                masterService.hapusPelanggan(Integer.parseInt(id.getText()));
                setCenter(customerView());
            } catch (Exception ex) {
                alert("Gagal", ex.getMessage());
            }
        });
        HBox wrap = new HBox(12, form(title("Kelola Pelanggan"), id, nama, hp, alamat, tipe, new HBox(8, baru, simpan, hapus)), table);
        HBox.setHgrow(table, Priority.ALWAYS);
        return page("Pelanggan", sub("Pelanggan adalah master pembeli yang dipakai saat transaksi penjualan."), wrap);
    }

    private VBox rawStockView(Stage stage) {
        ComboBox<OptionItem> jenis = combo(masterService.jenisIkan());
        ComboBox<OptionItem> supplier = combo(masterService.suppliers());
        TextField berat = field("Berat masuk kg");
        TextField harga = field("Harga beli per kg");
        TextField catatan = field("Catatan");
        Button simpan = primary("Simpan Stok Masuk");
        simpan.setOnAction(e -> {
            try {
                stockService.inputStokMentah(jenis.getValue().getId(), supplier.getValue().getId(), toDouble(berat), toDouble(harga), catatan.getText());
                setCenter(rawStockView(stage));
            } catch (Exception ex) {
                alert("Gagal", ex.getMessage());
            }
        });
        TableView<Map<String, Object>> table = table(Database.query("SELECT j.nama AS jenis_ikan, j.gambar_path, s.total_kg, s.updated_at FROM stok_mentah s JOIN jenis_ikan j ON j.id=s.jenis_ikan_id ORDER BY j.nama"));
        HBox wrap = new HBox(12, form(title("Input Stok Awal/Masuk"), jenis, supplier, berat, harga, catatan, simpan), table);
        HBox.setHgrow(table, Priority.ALWAYS);
        return page("Stok Mentah", sub("Daftar stok mentah per jenis ikan lengkap dengan data update."), wrap);
    }

    private VBox productionView() {
        ComboBox<OptionItem> jenis = combo(masterService.jenisIkan());
        TextField mentah = field("Berat mentah digunakan kg");
        TextField hasil = field("Berat hasil giling kg");
        TextField biaya = field("Biaya produksi");
        TextField harga = field("Harga jual per kg");
        TextField catatan = field("Catatan");
        Button proses = primary("Proses Produksi");
        proses.setOnAction(e -> {
            try {
                productionService.prosesProduksi(jenis.getValue().getId(), toDouble(mentah), toDouble(hasil), toDoubleOrZero(biaya), toDouble(harga), catatan.getText());
                alert("Berhasil", "Produksi ikan giling berhasil disimpan.");
                setCenter(milledStockView());
            } catch (Exception ex) {
                alert("Gagal", ex.getMessage());
            }
        });
        TableView<Map<String, Object>> table = table(Database.query("SELECT p.tanggal,j.nama AS jenis_ikan,p.berat_mentah_kg,p.berat_hasil_kg,p.penyusutan_kg,p.harga_jual_per_kg,p.catatan FROM produksi_giling p JOIN jenis_ikan j ON j.id=p.jenis_ikan_id ORDER BY p.id DESC"));
        HBox wrap = new HBox(12, form(title("Input Produksi Giling"), jenis, mentah, hasil, biaya, harga, catatan, proses), table);
        HBox.setHgrow(table, Priority.ALWAYS);
        return page("Produksi Ikan Giling", sub("Produksi mengurangi stok mentah dan menambah stok ikan giling siap jual."), wrap);
    }

    private VBox milledStockView() {
        TableView<Map<String, Object>> table = table(Database.query("SELECT j.nama AS jenis_ikan,j.gambar_path,g.total_kg,g.harga_jual_per_kg,g.tanggal_produksi,g.status_stok FROM stok_giling g JOIN jenis_ikan j ON j.id=g.jenis_ikan_id ORDER BY date(g.tanggal_produksi),g.id"));
        GridPane cards = fishCards(Database.query("SELECT j.nama,j.kategori,j.gambar_path,0 AS mentah,IFNULL(SUM(g.total_kg),0) AS giling FROM jenis_ikan j LEFT JOIN stok_giling g ON g.jenis_ikan_id=j.id GROUP BY j.id ORDER BY j.nama"));
        return page("Stok Ikan Giling", sub("Stok giling per jenis ikan, tanggal produksi, harga jual, dan jumlah tersedia."), scroll(cards), table);
    }

    private VBox salesView() {
        ComboBox<OptionItem> pelanggan = combo(masterService.pelanggan());
        ComboBox<OptionItem> jenis = combo(masterService.jenisIkan());
        TextField kg = field("Jumlah kg");
        TextField metode = field("Metode bayar");
        metode.setText("Tunai");
        TextField bayar = field("Jumlah bayar lunas");
        Button simpan = primary("Simpan Penjualan");
        simpan.setOnAction(e -> {
            try {
                String result = salesService.jualFifo(currentUser, pelanggan.getValue().getId(), jenis.getValue().getId(), toDouble(kg), metode.getText(), toDoubleOrZero(bayar));
                alert("Berhasil", result);
                setCenter(salesView());
            } catch (Exception ex) {
                alert("Gagal", ex.getMessage());
            }
        });
        TableView<Map<String, Object>> table = table(Database.query("SELECT p.nomor_transaksi,p.tanggal,pl.nama AS pelanggan,j.nama AS jenis_ikan,SUM(d.jumlah_kg) AS kg,p.total,p.status_pembayaran FROM penjualan p LEFT JOIN pelanggan pl ON pl.id=p.pelanggan_id JOIN detail_penjualan d ON d.penjualan_id=p.id JOIN jenis_ikan j ON j.id=d.jenis_ikan_id GROUP BY p.id,j.id ORDER BY p.id DESC"));
        HBox wrap = new HBox(12, form(title("Input Penjualan"), sub("Pilih jenis ikan. Sistem otomatis mengambil stok produksi yang lebih lama terlebih dahulu. Jika jumlah bayar dikosongkan, transaksi dianggap lunas sesuai total."), pelanggan, jenis, kg, metode, bayar, simpan), table);
        HBox.setHgrow(table, Priority.ALWAYS);
        return page("Penjualan", sub("Transaksi keluar otomatis mengurangi stok giling dan wajib lunas."), wrap);
    }

    private VBox historyView() {
        ComboBox<String> periode = new ComboBox<>(FXCollections.observableArrayList("Semua", "Hari Ini", "Minggu Ini", "Bulan Ini"));
        periode.getSelectionModel().selectFirst();
        ComboBox<OptionItem> jenis = combo(masterService.jenisIkan());
        Button filter = primary("Terapkan Filter");
        VBox box = page("Riwayat Stok", sub("Riwayat masuk, produksi, dan keluar keseluruhan atau per ikan."));
        Runnable refresh = () -> {
            box.getChildren().removeIf(n -> n instanceof TableView);
            box.getChildren().add(table(historyRows(periode.getValue(), jenis.getValue())));
        };
        filter.setOnAction(e -> refresh.run());
        box.getChildren().add(1, new HBox(10, new Label("Periode"), periode, new Label("Jenis"), jenis, filter));
        refresh.run();
        return box;
    }

    private VBox controlView() {
        ComboBox<OptionItem> transaksi = combo(masterService.transaksiBerhasil());
        TextField alasanBatal = field("Alasan pembatalan");
        Button batal = primary("Batalkan Penjualan");
        batal.setOnAction(e -> {
            try {
                if (transaksi.getValue() == null) throw new IllegalArgumentException("Belum ada transaksi berhasil yang bisa dibatalkan.");
                alert("Berhasil", salesService.batalkanPenjualan(transaksi.getValue().getId(), alasanBatal.getText()));
                setCenter(controlView());
            } catch (Exception ex) {
                alert("Gagal", ex.getMessage());
            }
        });

        ComboBox<OptionItem> jenisMentah = combo(masterService.jenisIkan());
        TextField fisikMentah = field("Jumlah hasil hitung kg");
        TextField alasanMentah = field("Alasan perubahan stok mentah");
        Button cekMentah = secondary("Simpan Cek Ulang Stok Mentah");
        cekMentah.setOnAction(e -> {
            try {
                if (jenisMentah.getValue() == null) throw new IllegalArgumentException("Pilih jenis ikan dulu.");
                alert("Berhasil", stockService.sesuaikanStokMentah(jenisMentah.getValue().getId(), toDouble(fisikMentah), alasanMentah.getText()));
                setCenter(controlView());
            } catch (Exception ex) {
                alert("Gagal", ex.getMessage());
            }
        });

        ComboBox<OptionItem> stokGiling = combo(masterService.stokGilingUntukKoreksi());
        TextField fisikGiling = field("Jumlah hasil hitung kg");
        TextField alasanGiling = field("Alasan perubahan stok giling");
        Button cekGiling = secondary("Simpan Cek Ulang Stok Giling");
        cekGiling.setOnAction(e -> {
            try {
                if (stokGiling.getValue() == null) throw new IllegalArgumentException("Pilih stok giling dulu.");
                alert("Berhasil", stockService.sesuaikanStokGiling(stokGiling.getValue().getId(), toDouble(fisikGiling), alasanGiling.getText()));
                setCenter(controlView());
            } catch (Exception ex) {
                alert("Gagal", ex.getMessage());
            }
        });

        HBox forms = new HBox(12,
                form(title("Batalkan Penjualan"), sub("Pilih transaksi yang sudah berhasil. Saat dibatalkan, stok akan dikembalikan otomatis."), transaksi, alasanBatal, batal),
                form(title("Cek Ulang Stok Mentah"), sub("Masukkan jumlah stok hasil hitung di tempat. Selisihnya akan masuk riwayat."), jenisMentah, fisikMentah, alasanMentah, cekMentah),
                form(title("Cek Ulang Stok Giling"), sub("Pilih stok giling yang dihitung ulang agar perubahan stok tetap jelas."), stokGiling, fisikGiling, alasanGiling, cekGiling));
        TableView<Map<String, Object>> table = table(Database.query("SELECT tanggal,jenis_stok,stok_sistem,stok_fisik AS stok_hasil_hitung,selisih,alasan FROM penyesuaian_stok ORDER BY id DESC"));
        return page("Kontrol Stok", sub("Menu untuk membatalkan penjualan dan memperbaiki jumlah stok setelah dihitung ulang."), forms, table);
    }

    private VBox reportView(Stage stage) {
        ComboBox<String> periode = new ComboBox<>(FXCollections.observableArrayList("Semua", "Hari Ini", "Minggu Ini", "Bulan Ini"));
        periode.getSelectionModel().selectFirst();
        ComboBox<OptionItem> jenis = combo(masterService.jenisIkan());
        Button export = primary("Simpan Laporan Excel");
        export.setOnAction(e -> {
            File file = saveExcel(stage, "laporan-catokan.xlsx");
            if (file == null) return;
            try {
                excelService.exportReport(file.toPath());
                alert("Berhasil", "Laporan Excel dibuat:\n" + file.getAbsolutePath());
            } catch (Exception ex) {
                alert("Gagal", ex.getMessage());
            }
        });
        Button backup = secondary("Cadangkan Data Aplikasi");
        backup.setOnAction(e -> {
            File file = saveExcel(stage, "data-catokan.xlsx");
            if (file == null) return;
            try {
                excelService.exportBackup(file.toPath());
                alert("Berhasil", "Data aplikasi berhasil dicadangkan:\n" + file.getAbsolutePath());
            } catch (Exception ex) {
                alert("Gagal", ex.getMessage());
            }
        });
        Button restore = secondary("Pulihkan Data Aplikasi");
        restore.setOnAction(e -> {
            File file = openExcel(stage);
            if (file == null) return;
            try {
                int rows = excelService.importBackup(file.toPath());
                alert("Berhasil", rows + " baris data aplikasi berhasil dipulihkan.");
                setCenter(dashboardView());
            } catch (Exception ex) {
                alert("Gagal", ex.getMessage());
            }
        });
        Button template = secondary("Template Stok Excel");
        template.setOnAction(e -> {
            File file = saveExcel(stage, "template-import-stok.xlsx");
            if (file == null) return;
            excelService.exportStockImportTemplate(file.toPath());
            alert("Berhasil", "Template dibuat.");
        });
        Button importExcel = secondary("Masukkan Stok dari Excel");
        importExcel.setOnAction(e -> {
            File file = openExcel(stage);
            if (file == null) return;
            int rows = excelService.importStockIn(file.toPath());
            alert("Berhasil", rows + " baris stok masuk berhasil dimasukkan.");
            setCenter(reportView(stage));
        });
        VBox box = page("Laporan Ringkas", sub("Laporan bisa dilihat semua, harian, mingguan, bulanan, dan per jenis ikan."), new HBox(10, new Label("Periode"), periode, new Label("Jenis"), jenis, export, backup, restore, template, importExcel));
        Runnable refresh = () -> {
            box.getChildren().removeIf(n -> n instanceof TableView || (n instanceof HBox h && h.getStyleClass().contains("stats-row")));
            String salesWhere = salesFilterSql(periode.getValue(), jenis.getValue());
            HBox stats = new HBox(12,
                    stat("Penjualan", "Rp " + scalar("SELECT IFNULL(SUM(p.total),0) FROM penjualan p WHERE 1=1 " + salesWhere)),
                    stat("Stok Mentah", scalar("SELECT IFNULL(SUM(total_kg),0) FROM stok_mentah") + " kg"),
                    stat("Stok Giling", scalar("SELECT IFNULL(SUM(total_kg),0) FROM stok_giling") + " kg"));
            stats.getStyleClass().add("stats-row");
            box.getChildren().add(2, stats);
            box.getChildren().add(table(Database.query("SELECT p.nomor_transaksi,p.tanggal,j.nama AS jenis_ikan,d.jumlah_kg,p.total,p.status_pembayaran FROM penjualan p JOIN detail_penjualan d ON d.penjualan_id=p.id JOIN jenis_ikan j ON j.id=d.jenis_ikan_id WHERE 1=1 " + salesWhere + " ORDER BY p.tanggal DESC,p.id DESC")));
        };
        periode.setOnAction(e -> refresh.run());
        jenis.setOnAction(e -> refresh.run());
        refresh.run();
        return box;
    }

    private List<Map<String, Object>> historyRows(String period, OptionItem jenis) {
        String sql = "SELECT r.tanggal,j.nama AS jenis_ikan,r.jenis_transaksi,r.jenis_stok,r.referensi,r.perubahan_kg,r.stok_sebelum,r.stok_sesudah,r.keterangan FROM riwayat_stok r LEFT JOIN jenis_ikan j ON j.id=r.jenis_ikan_id WHERE 1=1";
        sql = periodSql(sql, "r.tanggal", period);
        if (jenis != null) sql += " AND r.jenis_ikan_id=" + jenis.getId();
        return Database.query(sql + " ORDER BY tanggal DESC");
    }

    private String salesFilterSql(String period, OptionItem jenis) {
        String sql = "";
        sql = periodSql(sql, "p.tanggal", period);
        if (jenis != null) sql += " AND EXISTS (SELECT 1 FROM detail_penjualan dx WHERE dx.penjualan_id=p.id AND dx.jenis_ikan_id=" + jenis.getId() + ")";
        return sql;
    }

    private String periodSql(String base, String column, String period) {
        if ("Hari Ini".equals(period)) return base + " AND date(" + column + ")=date('now')";
        if ("Minggu Ini".equals(period)) return base + " AND date(" + column + ")>=date('now','-7 day')";
        if ("Bulan Ini".equals(period)) return base + " AND strftime('%Y-%m'," + column + ")=strftime('%Y-%m','now')";
        return base;
    }

    private GridPane fishCards(List<Map<String, Object>> rows) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        int col = 0, row = 0;
        for (Map<String, Object> r : rows) {
            VBox card = new VBox(8);
            card.getStyleClass().add("card");
            card.setPrefWidth(210);
            ImageView img = image(value(r, "gambar_path"), 190, 96);
            Label name = title(value(r, "nama"));
            Label meta = sub("Mentah: " + value(r, "mentah") + " kg | Giling: " + value(r, "giling") + " kg");
            card.getChildren().addAll(img, name, meta);
            grid.add(card, col, row);
            if (++col == 3) { col = 0; row++; }
        }
        return grid;
    }

    private VBox stockChart() {
        VBox box = new VBox(8);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(12));
        box.getChildren().addAll(title("Grafik Stok Giling"), sub("Bar menunjukkan stok giling per jenis ikan. Stok yang lebih lama akan dijual lebih dulu oleh sistem."));
        List<Map<String, Object>> rows = Database.query("SELECT j.nama,IFNULL(SUM(g.total_kg),0) AS total FROM jenis_ikan j LEFT JOIN stok_giling g ON g.jenis_ikan_id=j.id GROUP BY j.id ORDER BY total DESC LIMIT 8");
        double max = rows.stream().mapToDouble(r -> Double.parseDouble(value(r, "total").isBlank() ? "0" : value(r, "total"))).max().orElse(1);
        for (Map<String, Object> row : rows) {
            ProgressBar bar = new ProgressBar(max <= 0 ? 0 : Double.parseDouble(value(row, "total")) / max);
            bar.setMaxWidth(Double.MAX_VALUE);
            box.getChildren().add(new HBox(10, new Label(value(row, "nama")), bar, new Label(value(row, "total") + " kg")));
        }
        return box;
    }

    private TableView<Map<String, Object>> table(List<Map<String, Object>> rows) {
        TableView<Map<String, Object>> table = new TableView<>(FXCollections.observableArrayList(rows));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        if (!rows.isEmpty()) {
            for (String key : rows.get(0).keySet()) {
                TableColumn<Map<String, Object>, String> col = new TableColumn<>(key.replace("_", " ").toUpperCase());
                col.setCellValueFactory(data -> new SimpleStringProperty(value(data.getValue(), key)));
                table.getColumns().add(col);
            }
        }
        VBox.setVgrow(table, Priority.ALWAYS);
        HBox.setHgrow(table, Priority.ALWAYS);
        return table;
    }

    private HBox stat(String label, String value) {
        VBox box = new VBox(4, sub(label), title(value));
        box.getStyleClass().add("card");
        box.setPrefWidth(210);
        return new HBox(box);
    }

    private VBox page(String heading, Node... nodes) {
        VBox box = new VBox(12);
        box.setPadding(new Insets(12));
        box.getStyleClass().add("card");
        box.getChildren().add(title(heading));
        box.getChildren().addAll(nodes);
        return box;
    }

    private VBox form(Node... nodes) {
        VBox box = new VBox(8);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(12));
        box.setPrefWidth(300);
        box.getChildren().addAll(nodes);
        return box;
    }

    private VBox vbox(Node... nodes) {
        VBox box = new VBox(4);
        box.getChildren().addAll(nodes);
        return box;
    }

    private ScrollPane scroll(Node node) {
        ScrollPane sp = new ScrollPane(node);
        sp.setFitToWidth(true);
        sp.setPrefHeight(260);
        return sp;
    }

    private Button nav(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("nav-btn");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> action.run());
        return b;
    }

    private Button primary(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("primary-btn");
        return b;
    }

    private Button secondary(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("secondary-btn");
        return b;
    }

    private Label title(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("panel-title");
        return l;
    }

    private Label sub(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("brand-sub");
        l.setWrapText(true);
        return l;
    }

    private TextField field(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        return f;
    }

    private void clear(TextField... fields) {
        for (TextField field : fields) field.clear();
    }

    private TextArea area(String text) {
        TextArea a = new TextArea(text);
        a.setEditable(false);
        a.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 13px;");
        return a;
    }

    private ComboBox<OptionItem> combo(List<OptionItem> items) {
        FilteredList<OptionItem> filtered = new FilteredList<>(FXCollections.observableArrayList(items), item -> true);
        ComboBox<OptionItem> c = new ComboBox<>(filtered);
        c.setEditable(true);
        c.getEditor().setPromptText("Ketik untuk mencari...");
        c.getEditor().textProperty().addListener((obs, old, text) -> {
            OptionItem selected = c.getSelectionModel().getSelectedItem();
            if (selected != null && selected.toString().equals(text)) return;
            String key = text == null ? "" : text.toLowerCase();
            filtered.setPredicate(item -> key.isBlank() || item.getLabel().toLowerCase().contains(key));
            if (!c.isShowing()) c.show();
        });
        if (!items.isEmpty()) c.getSelectionModel().selectFirst();
        c.setMaxWidth(Double.MAX_VALUE);
        return c;
    }

    private ImageView image(String path, double width, double height) {
        Image image;
        try {
            if (path != null && path.startsWith("/")) image = new Image(getClass().getResourceAsStream(path));
            else if (path != null && !path.isBlank()) image = new Image(new File(path).toURI().toString());
            else image = new Image(getClass().getResourceAsStream("/images/catokan_banner.png"));
        } catch (Exception e) {
            image = new Image(getClass().getResourceAsStream("/images/catokan_banner.png"));
        }
        ImageView view = new ImageView(image);
        view.setFitWidth(width);
        view.setFitHeight(height);
        view.setPreserveRatio(false);
        view.setSmooth(true);
        return view;
    }

    private String scalar(String sql) {
        return String.valueOf(Database.query(sql).get(0).values().iterator().next());
    }

    private String value(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v == null ? "" : String.valueOf(v);
    }

    private void setCenter(Pane pane) {
        root.setCenter(pane);
    }

    private double toDouble(TextField f) {
        return Double.parseDouble(f.getText().trim().replace(",", "."));
    }

    private double toDoubleOrZero(TextField f) {
        String v = f.getText() == null ? "" : f.getText().trim();
        return v.isEmpty() ? 0 : Double.parseDouble(v.replace(",", "."));
    }

    private File saveExcel(Stage stage, String name) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Simpan Excel");
        chooser.setInitialFileName(name);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook", "*.xlsx"));
        return chooser.showSaveDialog(stage);
    }

    private File openExcel(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Pilih File Excel");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook", "*.xlsx"));
        return chooser.showOpenDialog(stage);
    }

    private File openImage(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Pilih Gambar Ikan");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Gambar", "*.png", "*.jpg", "*.jpeg"));
        return chooser.showOpenDialog(stage);
    }

    private void alert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
