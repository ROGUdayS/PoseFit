import cv2
import mediapipe as mp
import numpy as np
mp_drawing = mp.solutions.drawing_utils
mp_pose = mp.solutions.pose
cap = cv2.VideoCapture(0)
import winsound

#Frame count for warning 
warning = 0

#0 - up ; 1 - down
stage = 0
t_count = 0

# To keep track of incorrect and correct counter 
incorrect = 0
correct = 0

# Boolean to check if correct 
isCorrect = True

def showWarnings(img, warning):
     return

def calculateAngle(a,b,c):

        radians = np.arctan2(c[1] - b[1],c[0] - b[0]) - np.arctan2(a[1] - b[1],a[0] - b[0])
        angles = np.abs(radians*180/np.pi)
        if angles > 180:
            angles = 360-angles 

        return angles

def strictDeadlifts(img, pose_landmark, stage, count, warning_count, incorrect, correct, check):
     
    if(warning_count):
        warning_count -= 1
        # Calculate the width and height of the text
        text_width, text_height = cv2.getTextSize('Do not bend your spine', cv2.FONT_HERSHEY_DUPLEX, 0.4, 1)[0]

        # Padding for the text inside the rectangle
        padding = 8
        text_x = (640 - text_width)//2
        text_y = 480 - 2*padding
        rect_x = text_x - padding
        rect_y = rect_y = 480 - 3 * padding - text_height
        rect_width = text_width + 2 * padding
        rect_height = text_height + 2 * padding

        cv2.rectangle(img, (rect_x, rect_y), (rect_x + rect_width, rect_y + rect_height), (255, 255, 255), -1)
        cv2.putText(img, 'Do not bend your spine', (text_x, text_y), cv2.FONT_HERSHEY_DUPLEX, 0.4, (0, 0, 255), 1, cv2.LINE_AA)

        if warning_count > 50 :
             # Calculate the width and height of the text
            text_width, text_height = cv2.getTextSize('Maintain tension at hamstrings', cv2.FONT_HERSHEY_DUPLEX, 0.4, 1)[0]

            # Padding for the text inside the rectangle
            padding = 8
            text_x = (640 - text_width)//2
            text_y = 480 - 2*padding - 50
            rect_x = text_x - padding
            rect_y = rect_y = 480 - 3 * padding - text_height - 50
            rect_width = text_width + 2 * padding
            rect_height = text_height + 2 * padding

            cv2.rectangle(img, (rect_x, rect_y), (rect_x + rect_width, rect_y + rect_height), (255, 255, 255), -1)
            cv2.putText(img, 'Maintain tension at hamstrings', (text_x, text_y), cv2.FONT_HERSHEY_DUPLEX, 0.4, (0, 0, 255), 1, cv2.LINE_AA)
        
    # Get the co-ordinates and calculate angle
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

    if(angle_at_l_hip > 174 and angle_at_l_knee > 174):
         
                  
         if (stage):
            stage = 0
            count += 1

            if(check):
                correct += 1
            else:
                 incorrect += 1
                 check = True
         
    
    if(angle_at_l_hip < 85):
         
        stage = 1
        if(check and angle_at_l_hip <= 60):
            
            check = False
            warning_count += 50
            print("Do not bend your spine")

            if(angle_at_l_knee < 125):
                warning_count += 50
                print("Maintain tension at hamstrings")

            frequency = 2500  # Set frequency to 2500 Hertz
            duration = 550  # Set duration to 1000 milliseconds (550 milli-second)
            winsound.Beep(frequency, duration)
 
    # Calculate the width and height of the text
    text_width, text_height = cv2.getTextSize('total count: {}'.format(count), cv2.FONT_HERSHEY_DUPLEX, 0.4, 1)[0]

    # Padding for the text inside the rectangle
    padding = 10
    text_x = (640 - text_width)//2 - 150
    text_y = padding + text_height + padding
    rect_x = text_x - padding
    rect_y = padding
    rect_width = text_width + 2 * padding
    rect_height = text_height + 2 * padding

    cv2.rectangle(img, (rect_x, rect_y), (rect_x + rect_width, rect_y + rect_height), (0, 255, 0), -1)
    cv2.putText(img, 'total count: {}'.format(count), (text_x, text_y), cv2.FONT_HERSHEY_DUPLEX, 0.4, (255, 255, 255), 1, cv2.LINE_AA)

    # Calculate the width and height of the text
    text_width, text_height = cv2.getTextSize('correct count: {}'.format(correct), cv2.FONT_HERSHEY_DUPLEX, 0.4, 1)[0]

    # Padding for the text inside the rectangle
    padding = 10
    text_x = (640 - text_width)//2
    text_y = padding + text_height + padding
    rect_x = text_x - padding
    rect_y = padding
    rect_width = text_width + 2 * padding
    rect_height = text_height + 2 * padding

    cv2.rectangle(img, (rect_x, rect_y), (rect_x + rect_width, rect_y + rect_height), (0, 255, 0), -1)
    cv2.putText(img, 'Correct count: {}'.format(correct), (text_x, text_y), cv2.FONT_HERSHEY_DUPLEX, 0.4, (255, 255, 255), 1, cv2.LINE_AA)

     # Calculate the width and height of the text
    text_width, text_height = cv2.getTextSize('incorrect count: {}'.format(incorrect), cv2.FONT_HERSHEY_DUPLEX, 0.4, 1)[0]

    # Padding for the text inside the rectangle
    padding = 10
    text_x = (640 - text_width)//2 + 150
    text_y = padding + text_height + padding
    rect_x = text_x - padding
    rect_y = padding
    rect_width = text_width + 2 * padding
    rect_height = text_height + 2 * padding

    cv2.rectangle(img, (rect_x, rect_y), (rect_x + rect_width, rect_y + rect_height), (0, 255, 0), -1)
    cv2.putText(img, 'Incorrect count: {}'.format(incorrect), (text_x, text_y), cv2.FONT_HERSHEY_DUPLEX, 0.4, (255, 255, 255), 1, cv2.LINE_AA)
    
    return stage, count, correct, incorrect, check, warning_count

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
                stage, t_count, correct, incorrect, isCorrect, warning = strictDeadlifts(image, landmarks, stage, t_count, warning, incorrect, correct, isCorrect)
            except Exception as e:
                pass    

            # Render Detection
            mp_drawing.draw_landmarks(image, results.pose_landmarks, mp_pose.POSE_CONNECTIONS, landmark_drawing_spec=mp_drawing.DrawingSpec(color=(255,255,255)),connection_drawing_spec=mp_drawing.DrawingSpec(color= (255,153,255) ) )
            
            cv2.imshow('Mediapipe Feed', image)

        cap.release()
        cv2.destroyAllWindows()
