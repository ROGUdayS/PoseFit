import 'package:flutter/material.dart';
import 'package:another_flutter_splash_screen/another_flutter_splash_screen.dart';
import 'package:pose_fit/pages/home.dart';

class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  @override
  Widget build(BuildContext context) {
    return FlutterSplashScreen.scale(
          backgroundColor: const Color(0xFFfcfbf0),
          childWidget: SizedBox(
            height: 500,
            child: Image.asset("assets/images/HD_ICON.png"),
          ),
          duration: const Duration(seconds: 5),
          animationDuration: const Duration(seconds: 4),
          onAnimationEnd: () => debugPrint("On Scale End"),
          nextScreen: const HomePageWidget(),
        );
  }
}