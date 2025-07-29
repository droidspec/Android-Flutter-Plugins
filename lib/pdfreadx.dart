
import 'pdfreadx_platform_interface.dart';

class Pdfreadx {
  Future<String?> getPlatformVersion() {
    return PdfreadxPlatform.instance.getPlatformVersion();
  }
  Future<String?> startPDFViewActivity(Map<String, dynamic> args) {
    return PdfreadxPlatform.instance.startPDFViewActivity(args);
  }
}
