import 'package:intl/intl.dart';

final _rupiah = NumberFormat.currency(
  locale: 'id_ID',
  symbol: 'Rp ',
  decimalDigits: 0,
);

final _rupiahDecimal = NumberFormat.currency(
  locale: 'id_ID',
  symbol: 'Rp ',
  decimalDigits: 2,
);

String formatRupiah(num value) => _rupiah.format(value);
String formatRupiahDecimal(num value) => _rupiahDecimal.format(value);

/// Format kg — "12,5 kg" atau "0,5 kg" untuk desimal, tanpa desimal untuk bilangan bulat.
String formatKg(num value) {
  if (value == value.roundToDouble()) {
    return '${value.toInt()} kg';
  }
  final formatter = NumberFormat('#,##0.##', 'id_ID');
  return '${formatter.format(value)} kg';
}

String formatDate(DateTime dt) => DateFormat('d MMM yyyy', 'id_ID').format(dt);
String formatDateLong(DateTime dt) => DateFormat('EEEE, d MMMM yyyy', 'id_ID').format(dt);

/// Parse string input user ("5" / "5,5" / "5.5") → double, return null kalau invalid.
double? parseDouble(String? input) {
  if (input == null) return null;
  final cleaned = input.trim().replaceAll(',', '.');
  if (cleaned.isEmpty) return null;
  return double.tryParse(cleaned);
}