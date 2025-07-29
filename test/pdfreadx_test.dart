import 'package:flutter_test/flutter_test.dart';
import 'package:pdfreadx/pdfreadx.dart';
import 'package:pdfreadx/pdfreadx_platform_interface.dart';
import 'package:pdfreadx/pdfreadx_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockPdfreadxPlatform
    with MockPlatformInterfaceMixin
    implements PdfreadxPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
  
  @override
  Future<String?> startPDFViewActivity(Map<String, dynamic> args) {
    return Future.value('PDF View Activity started with args: $args'); 
  }
}

void main() {
  final PdfreadxPlatform initialPlatform = PdfreadxPlatform.instance;

  test('$MethodChannelPdfreadx is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelPdfreadx>());
  });

  test('getPlatformVersion', () async {
    Pdfreadx pdfreadxPlugin = Pdfreadx();
    MockPdfreadxPlatform fakePlatform = MockPdfreadxPlatform();
    PdfreadxPlatform.instance = fakePlatform;

    expect(await pdfreadxPlugin.getPlatformVersion(), '42');
  });
}
