import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';


class MyPlatformView extends StatefulWidget {
  final String selectedWorkout;
  const MyPlatformView({super.key, required this.selectedWorkout});

  @override
  State<MyPlatformView> createState() => _MyPlatformViewState();
}

class _MyPlatformViewState extends State<MyPlatformView> {
  final MethodChannel channel = const MethodChannel("NativeView");

  @override
  Widget build(BuildContext context) {
    final Map<String, dynamic> creationParams = <String, dynamic>{
      'WorkoutType': widget.selectedWorkout,
    };

    Widget platformView() {
      // Method to return the platform-specific view
      return Platform.isAndroid
          ? AndroidView(
              viewType: 'NativeView',
              layoutDirection: TextDirection.ltr,
              creationParams: creationParams,
              creationParamsCodec: const StandardMessageCodec(),
              onPlatformViewCreated: _onPlatformViewCreated,
            )
          : UiKitView(
              viewType: 'NativeView',
              layoutDirection: TextDirection.ltr,
              creationParams: creationParams,
              creationParamsCodec: const StandardMessageCodec(),
              onPlatformViewCreated: _onPlatformViewCreated,
            );
    }

    return Scaffold(
      body: Stack(
        alignment: Alignment.center,
        children: <Widget>[
          platformView(), // This is your camera preview or other native view
        ],
      ),
    );
  }

  void _onPlatformViewCreated(int id) {
    channel.setMethodCallHandler(_handleMethod);
  }

  Future<void> _handleMethod(MethodCall call) async {
    if (call.method == 'navigateToHome') {
      Navigator.pop(context);
    }
  }
}
