import 'package:flutter/material.dart';
import 'package:pdfreadx/pdfreadx.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  //String _platformVersion = 'Unknown';
  final _pdfreadxPlugin = Pdfreadx();

  @override
  void initState() {
    super.initState();
    //initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
/*   Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    // We also handle the message potentially returning null.
    try {
      platformVersion =
          await _pdfreadxPlugin.getPlatformVersion() ?? 'Unknown platform version';
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  } */

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
              child: ElevatedButton(
              onPressed: () async {
                Map<String, dynamic> args = {
                  'message': 'Hello from Flutter to PDF View, very nice!',
                  'filePath': '/data/data/com.ahmed.pdfx.pdfreadx/app_flutter/',
                  'fileName': 'sample.pdf',
                };
                String? result = await _pdfreadxPlugin.startPDFViewActivity(args);
                debugPrint('PDF View Activity started: $result');
              },
              child: const Text('Start PDF View Activity'),
            ),
            ),
        ),
    );
  }
}
