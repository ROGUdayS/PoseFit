import cv2
import mediapipe as mp
import numpy as np
mp_drawing = mp.solutions.drawing_utils
mp_pose = mp.solutions.pose
cap = cv2.VideoCapture(1)

class PoseFit:

    def __init__(self):
        
        self.pixels = [1280,720]
        self.counter = 0
        self.correct_counter = 0
        self.incorrect_counter = 0
        self.r_stage = ''
        self.l_stage = ''
        self.correct_connector_color = (255,153,255)
        self.wrong_connector_color = (0,102,204)
        self.isCorrect = True
        self.warnings = ''
        
        self.capture('BICEPS CURLS')

        return
    
    def bicepCurls(self, landmarks, image):
    
        if(mp_pose.PoseLandmark.LEFT_ELBOW.value and mp_pose.PoseLandmark.LEFT_SHOULDER.value and mp_pose.PoseLandmark.LEFT_WRIST.value):
            
            #To get co-ordinates

            l_elbow = [landmarks[mp_pose.PoseLandmark.LEFT_ELBOW.value].x, landmarks[mp_pose.PoseLandmark.LEFT_ELBOW.value].y]
            l_shoulder = [landmarks[mp_pose.PoseLandmark.LEFT_SHOULDER.value].x, landmarks[mp_pose.PoseLandmark.LEFT_SHOULDER.value].y]
            l_wrist = [landmarks[mp_pose.PoseLandmark.LEFT_WRIST.value].x, landmarks[mp_pose.PoseLandmark.LEFT_WRIST.value].y]
            
            r_elbow = [landmarks[mp_pose.PoseLandmark.RIGHT_ELBOW.value].x, landmarks[mp_pose.PoseLandmark.RIGHT_ELBOW.value].y]
            r_shoulder = [landmarks[mp_pose.PoseLandmark.RIGHT_SHOULDER.value].x, landmarks[mp_pose.PoseLandmark.RIGHT_SHOULDER.value].y]
            r_wrist = [landmarks[mp_pose.PoseLandmark.RIGHT_WRIST.value].x, landmarks[mp_pose.PoseLandmark.RIGHT_WRIST.value].y]

            #Start calculating angles

            ang_at_l_biceps = self.calculateAngle(l_shoulder,l_elbow,l_wrist)

            ang_at_r_biceps = self.calculateAngle(r_shoulder,r_elbow,r_wrist)
            
            #Heuristics for counter

            if  (ang_at_l_biceps>15 and ang_at_l_biceps < 60) and self.l_stage == 'down':
                
                self.isCorrect = True
                self.l_stage = 'up'
                self.counter += 1
                self.correct_counter += 1

            if (ang_at_l_biceps >140):
                self.l_stage = 'down'
                self.warnings = ''
                if(ang_at_l_biceps > 176 and self.isCorrect and self.counter > 1):
                    self.incorrect_counter += 1
                    self.isCorrect = False
                    self.warnings = 'Maintain tension at biceps'
    

            if  (ang_at_r_biceps>15 and ang_at_r_biceps < 60) and self.r_stage == 'down':
                
                self.isCorrect = True
                self.r_stage = 'up'
                self.counter += 1
                self.correct_counter += 1

            if (ang_at_r_biceps >140):
                self.r_stage = 'down'
                self.warnings = ''
                if(ang_at_r_biceps > 176 and self.isCorrect and self.counter > 1):
                    self.incorrect_counter += 1
                    self.isCorrect = False
                    self.warnings = 'Maintain tension at biceps'
            
            # cv2.putText(image, str(ang_at_l_biceps),tuple(np.multiply(l_elbow, self.pixels).astype(int)),cv2.FONT_HERSHEY_SIMPLEX, 
            #             0.5, (255,255,255), 1, cv2.LINE_AA
            #             )
            
            # cv2.putText(image, self.warnings ,(300,100),cv2.FONT_HERSHEY_SIMPLEX, 
            #             0.5, (255,0,255), 1, cv2.LINE_AA
            #             )
            
            # cv2.putText(image, str(ang_at_r_biceps),tuple(np.multiply(r_elbow, self.pixels).astype(int)),cv2.FONT_HERSHEY_SIMPLEX, 
            #             0.5, (255,255,255), 1, cv2.LINE_AA
            #             )

            # Calculate the width and height of the text
            text_width, text_height = cv2.getTextSize('total count: {}'.format(self.counter), cv2.FONT_HERSHEY_DUPLEX, 1, 1)[0]

            # Padding for the text inside the rectangle
            padding = 10
            text_x = 50 + padding
            text_y = 50 + padding + text_height
            rect_x = 50
            rect_y = 50
            rect_width = text_width + 2 * padding
            rect_height = text_height + 2 * padding

            cv2.rectangle(image, (rect_x, rect_y), (rect_x + rect_width, rect_y + rect_height), (0, 255, 0), -1)
            cv2.putText(image, 'total count: {}'.format(self.counter), (text_x, text_y), cv2.FONT_HERSHEY_DUPLEX, 1, (255, 255, 255), 1, cv2.LINE_AA)

            # Calculate the width and height of the text
            text_width, text_height = cv2.getTextSize('correct count: {}'.format(self.correct_counter), cv2.FONT_HERSHEY_DUPLEX, 1, 1)[0]

            # Padding for the text inside the rectangle
            padding = 10
            text_x = 50 + padding
            text_y = 50 + padding + text_height + 50
            rect_x = 50
            rect_y = 100
            rect_width = text_width + 2 * padding
            rect_height = text_height + 2 * padding

            cv2.rectangle(image, (rect_x, rect_y), (rect_x + rect_width, rect_y + rect_height), (0, 255, 0), -1)
            cv2.putText(image, 'total count: {}'.format(self.correct_counter), (text_x, text_y), cv2.FONT_HERSHEY_DUPLEX, 1, (255, 255, 255), 1, cv2.LINE_AA)

            # Calculate the width and height of the text
            text_width, text_height = cv2.getTextSize('incorrect count: {}'.format(self.incorrect_counter), cv2.FONT_HERSHEY_DUPLEX, 1, 1)[0]

            # Padding for the text inside the rectangle
            padding = 10
            text_x = 50 + padding
            text_y = 50 + padding + text_height + 100
            rect_x = 50
            rect_y = 150
            rect_width = text_width + 2 * padding
            rect_height = text_height + 2 * padding

            cv2.rectangle(image, (rect_x, rect_y), (rect_x + rect_width, rect_y + rect_height), (0, 255, 0), -1)
            cv2.putText(image, 'incorrect count: {}'.format(self.incorrect_counter), (text_x, text_y), cv2.FONT_HERSHEY_DUPLEX, 1, (255, 255, 255), 1, cv2.LINE_AA)
            return 
        
        else: return
    
    def calculateAngle(self,a,b,c):

        
        radians = np.arctan2(c[1] - b[1],c[0] - b[0]) - np.arctan2(a[1] - b[1],a[0] - b[0])
        angles = np.abs(radians*180/np.pi)
        if angles > 180:
            angles = 360-angles 

        return angles
    
    def reset(self): 

        self.stage = ''
        self.counter = 0

    def capture(self,exercise):

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

                    #Bicep curl exercise  
                    if(exercise == 'BICEPS CURLS'):
                        self.bicepCurls(landmarks, image)
                        
                except Exception as e:
                    pass    

                # Render Detection
                mp_drawing.draw_landmarks(image, results.pose_landmarks, mp_pose.POSE_CONNECTIONS, landmark_drawing_spec=mp_drawing.DrawingSpec(color=(255,255,255)),connection_drawing_spec=mp_drawing.DrawingSpec(color= self.correct_connector_color ) )
                
                cv2.imshow('Mediapipe Feed', image)
    
            cap.release()
            cv2.destroyAllWindows()
        return


posefit = PoseFit()
