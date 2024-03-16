import cv2
import mediapipe as mp
import numpy as np
mp_drawing = mp.solutions.drawing_utils
mp_pose = mp.solutions.pose
cap = cv2.VideoCapture(0)
import winsound

class PoseFit:

    def __init__(self):
        
        self.pixels = [640,480]
        self.counter = 0
        self.correct_counter = 0
        self.incorrect_counter = 0
        self.r_stage = ''
        self.l_stage = ''
        self.correct_connector_color = (255,153,255)
        self.wrong_connector_color = (0,102,204)
        self.isCorrect = True
        self.warnings = 0
        
        self.capture('BICEPS CURLS')

        return
    
    def bicepCurls(self, landmarks, image):

        print(self.warnings)
        
        if(self.warnings):
            self.warnings -= 1
            # Calculate the width and height of the text
            text_width, text_height = cv2.getTextSize('Maintain tension at the biceps', cv2.FONT_HERSHEY_DUPLEX, 0.4, 1)[0]

            # Padding for the text inside the rectangle
            padding = 8
            text_x = (640 - text_width)//2
            text_y = 480 - 2*padding
            rect_x = text_x - padding
            rect_y = rect_y = 480 - 3 * padding - text_height
            rect_width = text_width + 2 * padding
            rect_height = text_height + 2 * padding

            cv2.rectangle(image, (rect_x, rect_y), (rect_x + rect_width, rect_y + rect_height), (255, 255, 255), -1)
            cv2.putText(image, 'Maintain tension at the biceps', (text_x, text_y), cv2.FONT_HERSHEY_DUPLEX, 0.4, (0, 0, 255), 1, cv2.LINE_AA)


    
        
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

            self.l_stage = 'up'
            self.counter += 1

            if(self.isCorrect):
                self.correct_counter += 1
            else:
                self.incorrect_counter += 1
            
            self.isCorrect = True

        if (ang_at_l_biceps >140):

            self.l_stage = 'down'

            if(ang_at_l_biceps > 179 and self.isCorrect and self.counter > 1):
                self.warnings += 50
                self.isCorrect = False

                #Beep sound 
                frequency = 2500  # Set frequency to 2500 Hertz
                duration = 550  # Set duration to 1000 milliseconds (550 milli-second)
                winsound.Beep(frequency, duration)
            


        if  (ang_at_r_biceps>15 and ang_at_r_biceps < 60) and self.r_stage == 'down':
            
            self.r_stage = 'up'
            self.counter += 1
            
            if(self.isCorrect):
                self.correct_counter += 1
            else:
                self.incorrect_counter += 1
            
            self.isCorrect = True
            

        if (ang_at_r_biceps >140):

            self.r_stage = 'down'

            if(ang_at_r_biceps > 179 and self.isCorrect and self.counter > 1):
                self.warnings += 50
                self.isCorrect = False

                #Beep sound 
                frequency = 2500  # Set frequency to 2500 Hertz
                duration = 550  # Set duration to 1000 milliseconds (550 milli-second)
                winsound.Beep(frequency, duration)
            


        # Calculate the width and height of the text
        text_width, text_height = cv2.getTextSize('total count: {}'.format(self.counter), cv2.FONT_HERSHEY_DUPLEX, 0.4, 1)[0]

        # Padding for the text inside the rectangle
        padding = 10
        text_x = (640 - text_width)//2 - 150
        text_y = padding + text_height + padding
        rect_x = text_x - padding
        rect_y = padding
        rect_width = text_width + 2 * padding
        rect_height = text_height + 2 * padding

        cv2.rectangle(image, (rect_x, rect_y), (rect_x + rect_width, rect_y + rect_height), (0, 255, 0), -1)
        cv2.putText(image, 'total count: {}'.format(self.counter), (text_x, text_y), cv2.FONT_HERSHEY_DUPLEX, 0.4, (255, 255, 255), 1, cv2.LINE_AA)

        # Calculate the width and height of the text
        text_width, text_height = cv2.getTextSize('correct count: {}'.format(self.correct_counter), cv2.FONT_HERSHEY_DUPLEX, 0.4, 1)[0]

        # Padding for the text inside the rectangle
        padding = 10
        text_x = (640 - text_width)//2
        text_y = padding + text_height + padding
        rect_x = text_x - padding
        rect_y = padding
        rect_width = text_width + 2 * padding
        rect_height = text_height + 2 * padding

        cv2.rectangle(image, (rect_x, rect_y), (rect_x + rect_width, rect_y + rect_height), (0, 255, 0), -1)
        cv2.putText(image, 'correct count: {}'.format(self.correct_counter), (text_x, text_y), cv2.FONT_HERSHEY_DUPLEX, 0.4, (255, 255, 255), 1, cv2.LINE_AA)

        # Calculate the width and height of the text
        text_width, text_height = cv2.getTextSize('incorrect count: {}'.format(self.incorrect_counter), cv2.FONT_HERSHEY_DUPLEX, 0.4, 1)[0]

        # Padding for the text inside the rectangle
        padding = 10
        text_x = (640 - text_width)//2 + 150
        text_y = padding + text_height + padding
        rect_x = text_x - padding
        rect_y = padding
        rect_width = text_width + 2 * padding
        rect_height = text_height + 2 * padding

        cv2.rectangle(image, (rect_x, rect_y), (rect_x + rect_width, rect_y + rect_height), (0, 255, 0), -1)
        cv2.putText(image, 'incorrect count: {}'.format(self.incorrect_counter), (text_x, text_y), cv2.FONT_HERSHEY_DUPLEX, 0.4, (255, 255, 255), 1, cv2.LINE_AA)
        return 
    
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
