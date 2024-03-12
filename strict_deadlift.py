import cv2
import mediapipe as mp
import numpy as np
mp_drawing = mp.solutions.drawing_utils
mp_pose = mp.solutions.pose
cap = cv2.VideoCapture(0)

#0 - up ; 1 - down
stage = 0
count = 0

#Co-ordinates
'''
left shoulder
left hip 
left knee
left ankle 

right of 
 shoulder
 hip 
 knee
 ankle 
'''

def calculateAngle(a,b,c):

        radians = np.arctan2(c[1] - b[1],c[0] - b[0]) - np.arctan2(a[1] - b[1],a[0] - b[0])
        angles = np.abs(radians*180/np.pi)
        if angles > 180:
            angles = 360-angles 

        return angles

def strictDeadlifts(img, pose_landmark, stage, count):
     
    #Get the co-ordinates and calculate angle
    
    
    l_shoulder = [pose_landmark[mp_pose.PoseLandmark.LEFT_SHOULDER.value].x, pose_landmark[mp_pose.PoseLandmark.LEFT_SHOULDER.value].y]
    l_hip = [pose_landmark[mp_pose.PoseLandmark.LEFT_HIP.value].x, pose_landmark[mp_pose.PoseLandmark.LEFT_HIP.value].y]
    l_knee = [pose_landmark[mp_pose.PoseLandmark.LEFT_KNEE.value].x, pose_landmark[mp_pose.PoseLandmark.LEFT_KNEE.value].y]
    l_ankle = [pose_landmark[mp_pose.PoseLandmark.LEFT_ANKLE.value].x, pose_landmark[mp_pose.PoseLandmark.LEFT_ANKLE.value].y]

    r_shoulder = [pose_landmark[mp_pose.PoseLandmark.RIGHT_SHOULDER.value].x, pose_landmark[mp_pose.PoseLandmark.RIGHT_SHOULDER.value].y]
    r_hip = [pose_landmark[mp_pose.PoseLandmark.RIGHT_HIP.value].x, pose_landmark[mp_pose.PoseLandmark.RIGHT_HIP.value].y]
    r_knee = [pose_landmark[mp_pose.PoseLandmark.RIGHT_KNEE.value].x, pose_landmark[mp_pose.PoseLandmark.RIGHT_KNEE.value].y]
    r_ankle = [pose_landmark[mp_pose.PoseLandmark.RIGHT_ANKLE.value].x, pose_landmark[mp_pose.PoseLandmark.RIGHT_ANKLE.value].y]
    
    #Calculate angles left view

    angle_at_l_hip = calculateAngle(l_shoulder, l_hip, l_knee)
    angle_at_l_knee = calculateAngle(l_hip, l_knee, l_ankle)

    angle_at_r_hip = calculateAngle(r_shoulder, r_hip, r_knee)
    angle_at_r_knee = calculateAngle(r_hip, r_knee, r_ankle)

    cv2.putText(img, str(angle_at_l_hip),tuple(np.multiply(l_hip, [640,480]).astype(int)),cv2.FONT_HERSHEY_SIMPLEX, 
                    0.5, (255,255,255), 1, cv2.LINE_AA
                    )
    
    cv2.putText(img, str(angle_at_l_knee),tuple(np.multiply(l_knee, [640,480]).astype(int)),cv2.FONT_HERSHEY_SIMPLEX, 
                    0.5, (255,255,255), 1, cv2.LINE_AA
                    )

    if(angle_at_l_hip > 174 and angle_at_l_knee > 174):
                  
         if (stage):
            stage = 0
            count += 1
            # print(count)

            return stage, count
         
         else: 
              # Warning to go a bit more down
              pass
    
    if(angle_at_l_hip < 85 and angle_at_l_knee > 140):
         
         if(angle_at_l_hip > 61):
              stage = 1

              return stage, count
         else:
              # Warning to not bend the back much
              pass
    
    if(angle_at_l_knee < 125):
         # Warning maintain tension at hamstrings 
         pass
    
    return
         

with mp_pose.Pose(min_detection_confidence=0.8,min_tracking_confidence=0.8) as pose:
        while cap.isOpened():
            ret, frame=cap.read()

            if cv2.waitKey(1) & 0xFF == ord('q'):
                break

            # Processing Starts From Here
            #Recolor Image to RGB
            image=cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            image.flags.writeable=False

            #Make Detection
            results=pose.process(image)

            #Recolor back to BGR
            image.flags.writeable=True
            image=cv2.cvtColor(image, cv2.COLOR_RGB2BGR)

            # Extract Landmarks
            try:

                landmarks = results.pose_landmarks.landmark

                #Strict deadlift 
                stage, count = strictDeadlifts(image, landmarks, stage, count)
                    
            except Exception as e:
                pass    

            # Render Detection
            mp_drawing.draw_landmarks(image, results.pose_landmarks, mp_pose.POSE_CONNECTIONS, landmark_drawing_spec=mp_drawing.DrawingSpec(color=(255,255,255)),connection_drawing_spec=mp_drawing.DrawingSpec(color= (255,153,255) ) )
            
            cv2.imshow('Mediapipe Feed', image)

        cap.release()
        cv2.destroyAllWindows()
