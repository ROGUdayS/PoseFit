import 'package:flutterflow_ui/flutterflow_ui.dart';
import 'package:flutter/material.dart';
import 'package:pose_fit/pages/workout_view.dart';
import 'package:pose_fit/utils/workouts.dart';
import '../model/home_page_model.dart';
export '../model/home_page_model.dart';

class HomePageWidget extends StatefulWidget {
  const HomePageWidget({
    super.key,
    this.iconDialogue,
  });

  final bool? iconDialogue;

  @override
  State<HomePageWidget> createState() => _HomePageWidgetState();
}

class _HomePageWidgetState extends State<HomePageWidget> {
  late HomePageModel _model;
  final scaffoldKey = GlobalKey<ScaffoldState>();
  Map<String, bool> infoVisibility = {};
  String? visibleInfoKey;

  
  List<Workout> workouts = [
    Workout(
      title: 'BICEP CURLS',
      imagePath: 'assets/images/bicep_curls.png',
      info: 'Targets Left and Right Bicep Muscles',
      workoutKey: 'bicep_curls',
    ),
    Workout(
      title: 'SHOULDER PRESS',
      imagePath: 'assets/images/shoulder_press.png',
      info: 'Targets Shoulder Muscles',
      workoutKey: 'shoulder_press',
    ),
    Workout(
      title: 'SQUATS',
      imagePath: 'assets/images/squats.png',
      info: 'Targets Quads, Glutes ang Hamstring Muscles',
      workoutKey: 'squats',
    ),
    // Add more workouts here
  ];

  @override
  void initState() {
    super.initState();
    _model = createModel(context, () => HomePageModel());
  }

  @override
  void dispose() {
    _model.dispose();
    super.dispose();
  }

  Widget buildWorkout(Workout workout) {
    bool showInfo = visibleInfoKey == workout.workoutKey;
    return Padding(
      padding: const EdgeInsets.only(bottom: 20), // Adds space between each workout
      child: Container(
        width: MediaQuery.of(context).size.width * 0.941,
        height: 237,
        decoration: BoxDecoration(
          color: FlutterFlowTheme.of(context).secondaryBackground,
          image: DecorationImage(
            fit: BoxFit.cover,
            image: AssetImage(workout.imagePath),
          ),
          borderRadius: BorderRadius.circular(24),
        ),
        child: Stack(
          children: [
            Opacity(
              opacity: 0.1,
              child: Container(
                decoration: BoxDecoration(
                  color: Color(0xFF512B81),
                  borderRadius: BorderRadius.circular(24),
                ),
              ),
            ),
            Padding(
              padding: EdgeInsetsDirectional.fromSTEB(30, 178, 0, 0),
              child: Text(
                workout.title,
                style: FlutterFlowTheme.of(context).titleLarge.override(
                      fontFamily: 'Nunito',
                      color: Color(0xFFFAF0E6),
                      fontSize: 28,
                      fontWeight: FontWeight.w900,
                    ),
              ),
            ),
            Positioned(
              right: 15,
              bottom: 20,
              child: InkWell(
                onTap: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(builder: (context) => MyWorkoutView(selectedWorkout: workout.workoutKey)),
                  );
                },
                child: Icon(
                  Icons.play_circle,
                  color: FlutterFlowTheme.of(context).tertiary,
                  size: 44,
                ),
              ),
            ),
            Positioned(
              right: 25,
              top: 22,
              child: InkWell(
                onTap: () {
                  setState(() {
                    if (visibleInfoKey == workout.workoutKey) {
                      visibleInfoKey = null; // Hide currently shown info
                    } else {
                      visibleInfoKey = workout.workoutKey; // Show this workout's info
                    }
                  });
                },
                child: Icon(
                  Icons.info_outline,
                  color: FlutterFlowTheme.of(context).primaryBackground,
                  size: 24,
                ),
              ),
            ),
            if (showInfo)
              Positioned(
                right: 60,
                top: 20,
                child: Container(
                  padding: EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: Colors.black.withOpacity(0.6),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text(
                    workout.info,
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 10,
                    ),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  @override
Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => _model.unfocusNode.canRequestFocus
          ? FocusScope.of(context).requestFocus(_model.unfocusNode)
          : FocusScope.of(context).unfocus(),
      child: Scaffold(
        key: scaffoldKey,
        backgroundColor: FlutterFlowTheme.of(context).primaryBackground,
        appBar: PreferredSize(
          preferredSize: Size.fromHeight(100.0), // Set the desired height here
          child: AppBar(
            backgroundColor: Color(0xFF040D12),
            automaticallyImplyLeading: false,
            flexibleSpace: SafeArea(
              child: FlexibleSpaceBar(
                title: Container(
                  width: double.infinity,
                  height: 100,
                  decoration: BoxDecoration(
                    color: Color(0xFF040D12),
                  ),
                  child: Stack(
                    children: [
                      Align(
                        alignment: AlignmentDirectional(-0.5, 0),
                        child: Padding(
                          padding: EdgeInsetsDirectional.fromSTEB(10, 0, 10, 15),
                          child: Text(
                            'PoseFit',
                            style: FlutterFlowTheme.of(context).displaySmall.override(
                                  fontFamily: 'Readex Pro',
                                  color: Color(0xFFDBAFA0),
                                  letterSpacing: 0,
                                  fontSize: 50,
                                  fontWeight: FontWeight.w800,
                                ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                centerTitle: false,
                expandedTitleScale: 1.0,
              ),
            ),
          ),
        ),
        body: NestedScrollView(
          floatHeaderSlivers: false,
          headerSliverBuilder: (context, _) => [],
          body: Builder(
            builder: (context) {
              return SafeArea(
                top: false,
                child: SingleChildScrollView(
                  child: Container(
                    width: MediaQuery.of(context).size.width,
                    padding: EdgeInsets.only(bottom: 20), // Added padding to prevent overflow
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        colors: [Color(0xFF040D12), Color(0xFF183D3D)],
                        stops: [0.2, 1],
                        begin: AlignmentDirectional(0, -1),
                        end: AlignmentDirectional(0, 1),
                      ),
                    ),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: workouts.map(buildWorkout).toList(),
                    ),
                  ),
                ),
              );
            },
          ),
        ),
      ),
    );
  }

}