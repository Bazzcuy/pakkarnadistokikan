package com.bagas.stokikan;

import com.bagas.stokikan.db.Database;
import com.bagas.stokikan.model.OptionItem;
import com.bagas.stokikan.model.User;
import com.bagas.stokikan.service.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class MainApp extends Application {
    private final AuthService authService = new AuthService();
    private final MasterDataService masterService = new MasterDataService();
    private final StockService stockService = new StockService();
    private final ProductionService productionService = new ProductionService();
    private final SalesService salesService = new SalesService();
    private final ReportService reportService = new ReportService();
    private final ExcelService excelService = new ExcelService();
    private User currentUser;
    private BorderPane root;
    private VBox menu;
    private String workspaceMode;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Database.initialize();
        stage.setTitle("CATOKAN - Desktop Presentation App");
        stage.setScene(loginScene(stage));
        stage.show();
    }

    private Scene loginScene(Stage stage) {
        VBox outer = new VBox(16);
        outer.setPadding(new Insets(24));
        outer.setAlignment(Pos.TOP_CENTER);
        outer.getStyleClass().add("login-root");

        ImageView banner = new ImageView(new Image(getClass().getResourceAsStream("/images/catokan_banner.png")));
        banner.setFitWidth(720);
        banner.setFitHeight(220);
        banner.setPreserveRatio(false);
        banner.setSmooth(true);

        HBox brand = new HBox(14);
        brand.getStyleClass().add("card");
        brand.setAlignment(Pos.CENTER_LEFT);
        ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/images/catokan_logo.png")));
        logo.setFitWidth(80); logo.setFitHeight(80);
        VBox brandText = new VBox(4);
        Label title = new Label("CATOKAN"); title.getStyleClass().add("brand-title");
        Label subtitle = new Label("Catat Stok Ikan - UI lebih modern dan nyaman"); subtitle.getStyleClass().add("brand-sub");
        brandText.getChildren().addAll(title, subtitle);
        brand.getChildren().addAll(logo, brandText);

        VBox loginCard = new VBox(10);
        loginCard.getStyleClass().add("card");
        loginCard.setMaxWidth(520);
        Label loginTitle = new Label("Masuk ke Dashboard"); loginTitle.getStyleClass().add("panel-title");
        Label info = new Label("Akun demo: admin/admin123 - kasir/kasir123 - operator/operator123");
        info.getStyleClass().add("brand-sub");
        TextArea accounts = area("DAFTAR AKUN DEMO\n\nAdmin      : admin / admin123\nKasir      : kasir / kasir123\nProduksi   : operator / operator123");
        accounts.setPrefRowCount(5);
        accounts.setWrapText(false);
        TextField username = new TextField("admin");
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        password.setText("admin123");
        Button login = new Button("Login"); login.getStyleClass().add("primary-btn");
        login.setMaxWidth(Double.MAX_VALUE);
        login.setOnAction(e -> {
            currentUser = authService.login(username.getText(), password.getText());
            if (currentUser == null) {
                alert("Login gagal", "Username atau password salah.");
            } else {
                stage.setScene(appScene(stage));
            }
        });
        HBox quick = new HBox(8);
        Button admin = quickLogin("Admin", username, password, "admin", "admin123");
        Button kasir = quickLogin("Kasir", username, password, "kasir", "kasir123");
        Button operator = quickLogin("Produksi", username, password, "operator", "operator123");
        quick.getChildren().addAll(admin, kasir, operator);

        loginCard.getChildren().addAll(loginTitle, info, accounts, new Label("Username"), username, new Label("Password"), password, quick, login);

        outer.getChildren().addAll(banner, brand, loginCard);
        Scene scene = new Scene(outer, 860, 760);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        return scene;
    }

    private Scene appScene(Stage stage) {
        root = new BorderPane();
        root.setPadding(new Insets(14));

        if (workspaceMode == null || !workspaceAllowed(workspaceMode)) workspaceMode = defaultWorkspace();
        menu = new VBox(10);
        menu.setPadding(new Insets(14));
        menu.setPrefWidth(260);
        menu.getStyleClass().add("card");

        HBox brand = new HBox(10);
        brand.setAlignment(Pos.CENTER_LEFT);
        ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/images/catokan_logo.png")));
        logo.setFitWidth(48); logo.setFitHeight(48);
        VBox userWrap = new VBox(2);
        Label brandLabel = new Label("CATOKAN"); brandLabel.getStyleClass().add("panel-title");
        Label user = new Label("Login: " + currentUser.getNama() + " (" + currentUser.getRole() + ")");
        user.getStyleClass().add("brand-sub");
        userWrap.getChildren().addAll(brandLabel, user);
        brand.getChildren().addAll(logo, userWrap);

        Label workspace = new Label("Ruang kerja");
        workspace.getStyleClass().add("brand-sub");
        HBox switcher = new HBox(8);
        Button adminSpace = nav("Admin", () -> setWorkspace(stage, "ADMIN"));
        Button produksiSpace = nav("Produksi", () -> setWorkspace(stage, "PRODUKSI"));
        Button kasirSpace = nav("Kasir", () -> setWorkspace(stage, "KASIR"));
        adminSpace.setDisable(!canAccess("ADMIN"));
        produksiSpace.setDisable(!canAccess("ADMIN", "OPERATOR"));
        kasirSpace.setDisable(!canAccess("ADMIN", "KASIR"));
        switcher.getChildren().addAll(adminSpace, produksiSpace, kasirSpace);

        Button dashboard = nav("Dashboard", () -> setCenter(dashboardHome()));
        Button stokMentah = nav("Stok Mentah", () -> setCenter(stokMentahView()));
        Button produksi = nav("Produksi Giling", () -> setCenter(produksiView()));
        Button stokGiling = nav("Stok Giling", () -> setCenter(textPane(stockService.stokGilingText())));
        Button penjualan = nav("Penjualan", () -> setCenter(penjualanView()));
        Button pembayaran = nav("Pembayaran", () -> setCenter(pembayaranView()));
        Button laporan = nav("Laporan", () -> setCenter(laporanView(stage)));
        Button logout = nav("Logout", () -> stage.setScene(loginScene(stage)));

        stokMentah.setDisable(!canAccess("ADMIN", "OPERATOR"));
        produksi.setDisable(!canAccess("ADMIN", "OPERATOR"));
        penjualan.setDisable(!canAccess("ADMIN", "KASIR"));
        pembayaran.setDisable(!canAccess("ADMIN", "KASIR"));

        menu.getChildren().addAll(brand, workspace, switcher, dashboard, stokMentah, produksi, stokGiling, penjualan, pembayaran, laporan, logout);
        root.setLeft(menu);
        applyWorkspaceVisibility(stokMentah, produksi, stokGiling, penjualan, pembayaran, laporan);
        root.setCenter(dashboardHome());
        Scene scene = new Scene(root, 1180, 760);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        return scene;
    }

    private Button nav(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("nav-btn");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> action.run());
        return b;
    }

    private Button quickLogin(String label, TextField username, PasswordField password, String u, String p) {
        Button b = new Button(label);
        b.getStyleClass().add("secondary-btn");
        b.setOnAction(e -> {
            username.setText(u);
            password.setText(p);
        });
        return b;
    }

    private String defaultWorkspace() {
        if (canAccess("KASIR") && !canAccess("ADMIN")) return "KASIR";
        if (canAccess("OPERATOR") && !canAccess("ADMIN")) return "PRODUKSI";
        return "ADMIN";
    }

    private boolean workspaceAllowed(String mode) {
        if ("ADMIN".equals(mode)) return canAccess("ADMIN");
        if ("PRODUKSI".equals(mode)) return canAccess("ADMIN", "OPERATOR");
        if ("KASIR".equals(mode)) return canAccess("ADMIN", "KASIR");
        return false;
    }

    private void setWorkspace(Stage stage, String mode) {
        workspaceMode = mode;
        stage.setScene(appScene(stage));
    }

    private void applyWorkspaceVisibility(Button stokMentah, Button produksi, Button stokGiling, Button penjualan, Button pembayaran, Button laporan) {
        stokMentah.setVisible("ADMIN".equals(workspaceMode) || "PRODUKSI".equals(workspaceMode));
        stokMentah.setManaged(stokMentah.isVisible());
        produksi.setVisible("ADMIN".equals(workspaceMode) || "PRODUKSI".equals(workspaceMode));
        produksi.setManaged(produksi.isVisible());
        stokGiling.setVisible(true);
        stokGiling.setManaged(true);
        penjualan.setVisible("ADMIN".equals(workspaceMode) || "KASIR".equals(workspaceMode));
        penjualan.setManaged(penjualan.isVisible());
        pembayaran.setVisible("ADMIN".equals(workspaceMode) || "KASIR".equals(workspaceMode));
        pembayaran.setManaged(pembayaran.isVisible());
        laporan.setVisible("ADMIN".equals(workspaceMode) || "KASIR".equals(workspaceMode));
        laporan.setManaged(laporan.isVisible());
    }

    private VBox dashboardHome() {
        String label = switch (workspaceMode) {
            case "PRODUKSI" -> "Dashboard Produksi";
            case "KASIR" -> "Dashboard Kasir";
            default -> "Dashboard Admin";
        };
        String text = label + "\n" + "=".repeat(label.length()) + "\n\n" + reportService.dashboard();
        return textPane(text);
    }

    private boolean canAccess(String... roles) {
        if (currentUser == null || currentUser.getRole() == null) return false;
        for (String role : roles) if (role.equalsIgnoreCase(currentUser.getRole())) return true;
        return false;
    }

    private void setCenter(Pane pane) {
        root.setCenter(pane);
    }

    private VBox textPane(String text) {
        TextArea output = area(text);
        output.setWrapText(false);
        VBox box = new VBox(12);
        box.setPadding(new Insets(12));
        box.getStyleClass().add("card");
        Label head = new Label("Output Sistem");
        head.getStyleClass().add("panel-title");
        VBox.setVgrow(output, Priority.ALWAYS);
        box.getChildren().addAll(head, output);
        return box;
    }

    private VBox stokMentahView() {
        ComboBox<OptionItem> jenis = combo(masterService.jenisIkan());
        ComboBox<OptionItem> supplier = combo(masterService.suppliers());
        TextField berat = field("Berat kg");
        TextField harga = field("Harga beli per kg");
        TextField catatan = field("Catatan");
        Button simpan = new Button("Simpan Stok Masuk");
        simpan.getStyleClass().add("primary-btn");
        simpan.setOnAction(e -> {
            try {
                requireSelected(jenis, "Jenis ikan");
                requireSelected(supplier, "Supplier");
                stockService.inputStokMentah(jenis.getValue().getId(), supplier.getValue().getId(), toDouble(berat), toDouble(harga), catatan.getText());
                alert("Berhasil", "Stok mentah berhasil ditambahkan.");
                setCenter(stokMentahView());
            } catch (Exception ex) { alert("Gagal", ex.getMessage()); }
        });
        VBox form = new VBox(8, title("Input Stok Ikan Mentah"), new Label("Jenis Ikan"), jenis, new Label("Supplier"), supplier, berat, harga, catatan, simpan);
        form.setPadding(new Insets(10));
        form.getStyleClass().add("card");
        TextArea area = area(stockService.stokMentahText());
        HBox wrap = new HBox(12, form, area);
        HBox.setHgrow(area, Priority.ALWAYS);
        VBox box = new VBox(wrap); box.setPadding(new Insets(12));
        return box;
    }

    private VBox produksiView() {
        ComboBox<OptionItem> jenis = combo(masterService.jenisIkan());
        TextField mentah = field("Berat mentah digunakan (kg)");
        TextField hasil = field("Berat hasil giling (kg)");
        TextField biaya = field("Biaya produksi tambahan");
        TextField hargaJual = field("Harga jual per kg");
        TextField catatan = field("Catatan produksi");
        Button proses = new Button("Proses Produksi"); proses.getStyleClass().add("primary-btn");
        proses.setOnAction(e -> {
            try {
                requireSelected(jenis, "Jenis ikan");
                String batch = productionService.prosesProduksi(jenis.getValue().getId(), toDouble(mentah), toDouble(hasil), toDoubleOrZero(biaya), toDouble(hargaJual), catatan.getText());
                alert("Berhasil", "Produksi berhasil. Batch: " + batch);
                setCenter(textPane(stockService.stokGilingText()));
            } catch (Exception ex) { alert("Gagal", ex.getMessage()); }
        });
        VBox form = new VBox(8, title("Input Produksi Ikan Giling"), jenis, mentah, hasil, biaya, hargaJual, catatan, proses);
        form.setPadding(new Insets(10)); form.getStyleClass().add("card");
        TextArea area = area(stockService.stokMentahText() + "\n" + stockService.stokGilingText());
        HBox wrap = new HBox(12, form, area); HBox.setHgrow(area, Priority.ALWAYS);
        VBox box = new VBox(wrap); box.setPadding(new Insets(12));
        return box;
    }

    private VBox penjualanView() {
        ComboBox<OptionItem> pelanggan = combo(masterService.pelanggan());
        ComboBox<OptionItem> batch = combo(masterService.batchGiling());
        TextField kg = field("Jumlah kg");
        TextField metode = field("Metode bayar (Tunai/Transfer)"); metode.setText("Tunai");
        TextField bayar = field("Jumlah bayar");
        Button simpan = new Button("Simpan Penjualan"); simpan.getStyleClass().add("primary-btn");
        simpan.setOnAction(e -> {
            try {
                requireSelected(pelanggan, "Pelanggan");
                requireSelected(batch, "Batch ikan giling");
                String hasil = salesService.jualCepat(currentUser, pelanggan.getValue().getId(), batch.getValue().getId(), toDouble(kg), metode.getText(), toDoubleOrZero(bayar));
                alert("Berhasil", hasil);
                setCenter(textPane(salesService.transaksiText() + "\n" + stockService.stokGilingText()));
            } catch (Exception ex) { alert("Gagal", ex.getMessage()); }
        });
        VBox form = new VBox(8, title("Input Transaksi Penjualan"), pelanggan, batch, kg, metode, bayar, simpan);
        form.setPadding(new Insets(10)); form.getStyleClass().add("card");
        TextArea area = area(salesService.transaksiText() + "\n" + stockService.stokGilingText());
        HBox wrap = new HBox(12, form, area); HBox.setHgrow(area, Priority.ALWAYS);
        VBox box = new VBox(wrap); box.setPadding(new Insets(12));
        return box;
    }

    private VBox pembayaranView() {
        TextField idTransaksi = field("ID penjualan belum lunas");
        TextField bayar = field("Jumlah bayar");
        TextField metode = field("Metode bayar"); metode.setText("Tunai");
        Button simpan = new Button("Simpan Pembayaran"); simpan.getStyleClass().add("primary-btn");
        simpan.setOnAction(e -> {
            try {
                salesService.bayarTransaksi(Integer.parseInt(idTransaksi.getText().trim()), toDouble(bayar), metode.getText());
                alert("Berhasil", "Pembayaran tersimpan.");
                setCenter(pembayaranView());
            } catch (Exception ex) { alert("Gagal", ex.getMessage()); }
        });
        VBox form = new VBox(8, title("Input Pembayaran Lanjutan"), idTransaksi, bayar, metode, simpan);
        form.setPadding(new Insets(10)); form.getStyleClass().add("card");
        TextArea area = area(salesService.pembayaranText());
        HBox wrap = new HBox(12, form, area); HBox.setHgrow(area, Priority.ALWAYS);
        VBox box = new VBox(wrap); box.setPadding(new Insets(12));
        return box;
    }

    private VBox laporanView(Stage stage) {
        VBox box = new VBox(12);
        box.setPadding(new Insets(12));
        box.getStyleClass().add("card");

        Label head = title("Laporan dan Excel");
        Label hint = new Label("Export laporan lengkap, buat template import, atau import stok masuk dari file Excel.");
        hint.getStyleClass().add("brand-sub");

        HBox actions = new HBox(10);
        Button export = new Button("Export Laporan Excel");
        export.getStyleClass().add("primary-btn");
        export.setOnAction(e -> {
            File file = saveExcel(stage, "laporan-catokan.xlsx");
            if (file == null) return;
            try {
                excelService.exportReport(file.toPath());
                alert("Berhasil", "Laporan Excel berhasil dibuat:\n" + file.getAbsolutePath());
            } catch (Exception ex) {
                alert("Gagal", ex.getMessage());
            }
        });

        Button template = new Button("Template Import");
        template.getStyleClass().add("secondary-btn");
        template.setOnAction(e -> {
            File file = saveExcel(stage, "template-import-stok.xlsx");
            if (file == null) return;
            try {
                excelService.exportStockImportTemplate(file.toPath());
                alert("Berhasil", "Template import berhasil dibuat:\n" + file.getAbsolutePath());
            } catch (Exception ex) {
                alert("Gagal", ex.getMessage());
            }
        });

        Button importExcel = new Button("Import Stok Excel");
        importExcel.getStyleClass().add("secondary-btn");
        importExcel.setOnAction(e -> {
            File file = openExcel(stage);
            if (file == null) return;
            try {
                int rows = excelService.importStockIn(file.toPath());
                alert("Berhasil", rows + " baris stok masuk berhasil diimport.");
                setCenter(laporanView(stage));
            } catch (Exception ex) {
                alert("Gagal", ex.getMessage());
            }
        });
        actions.getChildren().addAll(export, template, importExcel);

        TextArea area = area(reportService.laporanRingkas());
        VBox.setVgrow(area, Priority.ALWAYS);
        box.getChildren().addAll(head, hint, actions, area);
        return box;
    }

    private Label title(String t) { Label l = new Label(t); l.getStyleClass().add("panel-title"); return l; }
    private TextField field(String prompt) { TextField f = new TextField(); f.setPromptText(prompt); return f; }
    private TextArea area(String text) { TextArea a = new TextArea(text); a.setEditable(false); a.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 13px;"); HBox.setHgrow(a, Priority.ALWAYS); return a; }
    private ComboBox<OptionItem> combo(java.util.List<OptionItem> items) { ComboBox<OptionItem> c = new ComboBox<>(); c.getItems().addAll(items); if (!items.isEmpty()) c.getSelectionModel().selectFirst(); c.setMaxWidth(Double.MAX_VALUE); return c; }
    private void requireSelected(ComboBox<OptionItem> combo, String label) { if (combo.getValue() == null) throw new IllegalArgumentException(label + " belum dipilih"); }
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
    private double toDouble(TextField f) { return Double.parseDouble(f.getText().trim().replace(",", ".")); }
    private double toDoubleOrZero(TextField f) { String v = f.getText() == null ? "" : f.getText().trim(); return v.isEmpty() ? 0 : Double.parseDouble(v.replace(",", ".")); }
    private void alert(String title, String msg) { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait(); }
}
