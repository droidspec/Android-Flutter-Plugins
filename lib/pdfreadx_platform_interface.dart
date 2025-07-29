import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'pdfreadx_method_channel.dart';

abstract class PdfreadxPlatform extends PlatformInterface {
  /// Constructs a PdfreadxPlatform.
  PdfreadxPlatform() : super(token: _token);

  static final Object _token = Object();

  static PdfreadxPlatform _instance = MethodChannelPdfreadx();

  /// The default instance of [PdfreadxPlatform] to use.
  ///
  /// Defaults to [MethodChannelPdfreadx].
  static PdfreadxPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [PdfreadxPlatform] when
  /// they register themselves.
  static set instance(PdfreadxPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  Future<String?> startPDFViewActivity(Map<String, dynamic> args) {
    throw UnimplementedError('startPDFViewActivity() has not been implemented.');
  }
}
