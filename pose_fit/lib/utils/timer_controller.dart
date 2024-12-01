class TimerController {
  Stopwatch _stopwatch;
  Function()? onUpdate; // Callback to inform the widget of updates

  TimerController(this._stopwatch);

  void start() {
    if (!_stopwatch.isRunning) {
      _stopwatch.start();
      onUpdate?.call();
    }
  }

  void stop() {
    if (_stopwatch.isRunning) {
      _stopwatch.stop();
      onUpdate?.call();
    }
  }

  void reset() {
    _stopwatch.reset();
    onUpdate?.call();
  }

  bool isRunning() => _stopwatch.isRunning;
}
