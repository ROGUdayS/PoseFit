import 'dart:async';
import 'package:flutter/material.dart';

class TimerWidget extends StatefulWidget {
  final Function(bool) onRunningChange; // Callback to pass running state
  final Function onReset; // Callback when reset is clicked
  final bool isRunning;

  TimerWidget({Key? key, required this.onRunningChange, required this.onReset, this.isRunning = true}) : super(key: key);

  @override
  _TimerWidgetState createState() => _TimerWidgetState();
}


class _TimerWidgetState extends State<TimerWidget> {
  late Stopwatch stopwatch;
  late Timer timer;

  @override
  void initState() {
    super.initState();
    stopwatch = Stopwatch();
    timer = Timer.periodic(Duration(milliseconds: 30), (timer) {
      setState(() {});
    });
  }
  @override
  void didUpdateWidget(covariant TimerWidget oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.isRunning && !stopwatch.isRunning) {
      stopwatch.start();
    } else if (!widget.isRunning && stopwatch.isRunning) {
      stopwatch.stop();
    }
  }

  @override
  void dispose() {
    stopwatch.stop();
    timer.cancel();
    super.dispose();
  }


  void handleStartStop() {
    if (stopwatch.isRunning) {
      stopwatch.stop();
    } else {
      stopwatch.start();
    }
    widget.onRunningChange(stopwatch.isRunning);
    setState(() {});
  }

  void resetTimer() {
    stopwatch.reset();
    widget.onReset();
    setState(() {});
  }


  String returnFormattedText() {
    final milli = stopwatch.elapsed.inMilliseconds;
    String milliseconds = (milli % 1000).toString().padLeft(3, "0");
    String seconds = ((milli ~/ 1000) % 60).toString().padLeft(2, "0");
    String minutes = ((milli ~/ 1000) ~/ 60).toString().padLeft(2, "0");
    return "$minutes:$seconds:$milliseconds";
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Text(
          returnFormattedText(), 
          style: Theme.of(context).textTheme.headline4?.copyWith(color: Colors.white) ?? TextStyle(color: Colors.white),
        ),
        SizedBox(height: 10),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            ElevatedButton(
              onPressed: handleStartStop,
              child: Text(stopwatch.isRunning ? 'Pause' : 'Play'),
            ),
            SizedBox(width: 10),
            if (stopwatch.isRunning || stopwatch.elapsedMilliseconds > 0) 
              ElevatedButton(
                onPressed: resetTimer,
                child: Text('Reset'),
              ),
          ],
        ),
      ],
    );
  }
}
