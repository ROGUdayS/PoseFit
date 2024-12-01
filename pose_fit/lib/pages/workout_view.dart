
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:flutter/services.dart';
import 'package:pose_fit/utils/platform_view.dart';
import 'package:pose_fit/utils/timer.dart';

class MyWorkoutView extends StatefulWidget {
  final String selectedWorkout;
  const MyWorkoutView({super.key, required this.selectedWorkout});

  @override
  State<MyWorkoutView> createState() => _MyWorkoutViewState();
}

class _MyWorkoutViewState extends State<MyWorkoutView> {

  int selectedModel = 0;
  bool isGPUDelagate = false;
  List<double> thresholds = [0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8];
  double detectionThreshold = 0.5;
  double trackingThreshold = 0.5;
  double presenceThreshold = 0.5;
  String selectedWorkout = "";
  bool switchCamera = false;
  Future<void>? _configurationFuture;
  static const platform = MethodChannel('com.example.pose_fit/configuration');
  bool isTimerRunning = false;


  @override
  void initState() {
    super.initState();
    selectedWorkout = widget.selectedWorkout;
    _loadSettings();
    platform.setMethodCallHandler(_handleMethod);
  }

  Future<dynamic> _handleMethod(MethodCall call) async {
    switch (call.method) {
        case "sendResultData":
            debugPrint("Received data from native: ${call.arguments}");
            break;
        default:
            throw 'Method not implemented';
    }
  }

  Future<void> _sendConfigurationChanges() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setInt('selectedModel', selectedModel);
      await prefs.setBool('isGPUDelagate', isGPUDelagate);
      await prefs.setDouble('detectionThreshold', detectionThreshold);
      await prefs.setDouble('trackingThreshold', trackingThreshold);
      await prefs.setDouble('presenceThreshold', presenceThreshold);
      await prefs.setBool('switchCamera', switchCamera);

      await platform.invokeMethod('updateConfig', {
        'selectedModel': selectedModel,
        'isGPUDelagate': isGPUDelagate,
        'detectionThreshold': detectionThreshold,
        'trackingThreshold': trackingThreshold,
        'presenceThreshold': presenceThreshold,
        'switchCamera': switchCamera,
      });
    } catch (e) {
      // Handle possible errors
      print('Failed to send configuration: $e');
    }
  }

  Future<void> _loadSettings() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      selectedModel = prefs.getInt('selectedModel') ?? 0;
      isGPUDelagate = prefs.getBool('isGPUDelagate') ?? false;
      detectionThreshold = prefs.getDouble('detectionThreshold') ?? 0.5;
      trackingThreshold = prefs.getDouble('trackingThreshold') ?? 0.5;
      presenceThreshold = prefs.getDouble('presenceThreshold') ?? 0.5;
      switchCamera = prefs.getBool('switchCamera') ?? false;
    });
     // Delay the configuration future
    _configurationFuture = Future.delayed(
      Duration(milliseconds: 30),
      () => _sendConfigurationChanges(),
    );
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Scaffold(
        backgroundColor: Colors.transparent,
        appBar: AppBar(
          title: Text(widget.selectedWorkout),
          backgroundColor: Colors.transparent,
          leading: IconButton(
            icon: const Icon(Icons.arrow_back, color: Color(0xFFDBAFA0), size: 36),
            onPressed: () async {
              // Add code to properly shut down camera and pose landmarker tasks
              await platform.invokeMethod('stopCamera');  // Assuming this method is defined in your native code to handle camera shutdown
              if (this.mounted) {
                Navigator.pop(context);
              }
            },
          ),
          elevation: 5,
          actionsIconTheme: const IconThemeData(color: Color(0xFFDBAFA0), size: 36),
          actions: _buildAppBarActions(),
        ),
        body: Column(
          children: [
            Expanded(
              child: FutureBuilder(
                future: _configurationFuture,
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.done) {
                    return MyPlatformView(selectedWorkout: widget.selectedWorkout);
                  } else {
                    return Center(child: CircularProgressIndicator());
                  }
                },
              ),
            ),
            Container(
              width: MediaQuery.of(context).size.width,
              height: 120,
              color: Colors.transparent, // Example color
              child: TimerWidget(
                onRunningChange: (isRunning) {
                  debugPrint("Timer is running: $isRunning");
                  platform.invokeMethod('setTimerRunning', {'isRunning': isRunning});
                },
                onReset: _sendResetEvent,
                isRunning: isTimerRunning,
              ),
            ),
          ],
        ),
      ),
    );
  }

  List<Widget> _buildAppBarActions() {
    return [
      IconButton(
        icon: Image.asset('assets/images/HD_ICON.png'),
        onPressed: () {
        },
      ),
      IconButton(
        icon: const Icon(Icons.switch_camera),
        onPressed: () {
          setState(() {
            switchCamera = !switchCamera; // Toggle the value
            _configurationFuture = _sendConfigurationChanges();
          });
        },
      ),
      IconButton(
        icon: const Icon(Icons.settings),
        onPressed: () {
          setState(() {
            isTimerRunning = false; // Pause the timer
          });
          showModalBottomSheet(
            context: context,
            builder: (context) => buildSettingsPanel(),
          ).whenComplete(() {
            setState(() {
              isTimerRunning = true; // Resume the timer
              _configurationFuture = _sendConfigurationChanges();
            });
          });
        },
      ),
    ];
  }


  Widget buildSettingsPanel() {
    return StatefulBuilder(
      builder: (BuildContext context, StateSetter setStateBottomSheet) {
        return Container(
          height: MediaQuery.of(context).size.height / 2,
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Model Selection:', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              DropdownButton<int>(
                value: selectedModel,
                onChanged: (int? newValue) {
                  setStateBottomSheet(() {
                    if (newValue != null) {
                      selectedModel = newValue;
                    }
                  });
                },
                items: const [
                  DropdownMenuItem<int>(
                    value: 0,
                    child: Text('MODEL_POSE_LANDMARKER_LITE'),
                  ),
                  DropdownMenuItem<int>(
                    value: 1,
                    child: Text('MODEL_POSE_LANDMARKER_FULL'),
                  ),
                  DropdownMenuItem<int>(
                    value: 2,
                    child: Text('MODEL_POSE_LANDMARKER_HEAVY'),
                  ),
                ],
              ),

              const SizedBox(height: 20),
              Row(
                children: [
                  const Text('Delegate GPU :', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                  Switch(
                    activeTrackColor: const Color(0xFFDBAFA0),
                    value: isGPUDelagate, // Directly use the boolean value
                    onChanged: (bool value) {
                      setStateBottomSheet(() {
                        isGPUDelagate = value; // Directly set the boolean
                      });
                    },
                  ),
                ],
              ),
              buildThresholdAdjuster(setStateBottomSheet, 'Detection Threshold:', detectionThreshold, (newValue) => detectionThreshold = newValue),
              buildThresholdAdjuster(setStateBottomSheet, 'Tracking Threshold:', trackingThreshold, (newValue) => trackingThreshold = newValue),
              buildThresholdAdjuster(setStateBottomSheet, 'Presence Threshold:', presenceThreshold, (newValue) => presenceThreshold = newValue),
            ],
          ),
        );
      },
    );
  }


  Widget buildThresholdAdjuster(StateSetter setStateBottomSheet, String label, double currentValue, Function(double) onNewValueSelected) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
        Row(
          children: [
            IconButton(
              icon: const Icon(Icons.remove),
              onPressed: () {
                final newValueIndex = thresholds.indexOf(currentValue) - 1;
                if (newValueIndex >= 0) {
                  final newValue = thresholds[newValueIndex];
                  setStateBottomSheet(() => onNewValueSelected(newValue));
                }
              },
            ),
            // ignore: unnecessary_string_interpolations
            Text('${currentValue.toStringAsFixed(2)}'),
            IconButton(
              icon: const Icon(Icons.add),
              onPressed: () {
                final newValueIndex = thresholds.indexOf(currentValue) + 1;
                if (newValueIndex < thresholds.length) {
                  final newValue = thresholds[newValueIndex];
                  setStateBottomSheet(() => onNewValueSelected(newValue));
                }
              },
            ),
          ],
        ),
      ],
    );
  }

  Future<void> _sendResetEvent() async {
    try {
      await platform.invokeMethod('resetEvent', {'isResetOptionClicked': true});
    } catch (e) {
      debugPrint('Failed to send reset event: $e');
    }
  }

}
