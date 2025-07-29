import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'pdfreadx_platform_interface.dart';

/// An implementation of [PdfreadxPlatform] that uses method channels.
class MethodChannelPdfreadx extends PdfreadxPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('pdfreadx');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  Future<String?> startPDFViewActivity(Map<String, dynamic> args) async {
    final result = await methodChannel.invokeMethod<String>('startPDFViewActivity', {'data': args});
    return result;
  }
}
