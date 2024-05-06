import cv2
import tensorflow as tf
from matplotlib import pyplot as plt
import mediapipe as mp
mp_drawing = mp.solutions.drawing_utils
mp_pose = mp.solutions.pose
cap = cv2.VideoCapture(0)
import numpy as np

FRAME = 1000

# Mediapipe for 200 frames

mediapipe_list = []
movenet_list = []

interpreter = tf.lite.Interpreter(model_path='Movenet.tflite')
interpreter.allocate_tensors()

#Get input details 
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

x = 1

with mp_pose.Pose(min_detection_confidence=0.8,min_tracking_confidence=0.8) as pose:
    while cap.isOpened():
        FRAME -= 1

        ret, frame=cap.read()

        if cv2.waitKey(1) & 0xFF == ord('q'):
            break
        
        if(FRAME == 500):
            if (x == 1):
                x += 1
        
        if(not FRAME):
            cap.release()   
            cv2.destroyAllWindows()

        if (x == 1):
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
                sum = 0

                landmarks = results.pose_landmarks.landmark
                
                for marks in landmarks:
                    sum += marks.presence
                
                mediapipe_list.append(sum/33)
                    
            except Exception as e:
                pass    

            # Render Detection
            mp_drawing.draw_landmarks(image, results.pose_landmarks, mp_pose.POSE_CONNECTIONS, landmark_drawing_spec=mp_drawing.DrawingSpec(color=(255,255,255)),connection_drawing_spec=mp_drawing.DrawingSpec(color= (255,153,255) ) )
            
            cv2.imshow('Mediapipe Feed', image)
        
        else:

            FRAME -= 1

            img = frame.copy()
            img = tf.image.resize_with_pad(np.expand_dims(img, axis=0), 192, 192)
            input_image = tf.cast(img, dtype=tf.float32)

            # Make predictions
            interpreter.set_tensor(input_details[0]['index'], np.array(input_image))
            interpreter.invoke()
            keypoints_with_score = np.squeeze(interpreter.get_tensor(output_details[0]['index']))

            sum = 0
            count = 0
            for points in keypoints_with_score:
                if(points[2] > 0.6):
                    sum += points[2]
                    count += 1
                else:
                    FRAME += 1

            if (sum):
                movenet_list.append(sum/count)

            cv2.imshow('MoveNet Lightning',frame)



mediapipe_list_len = len(mediapipe_list)
movenet_list_len = len(movenet_list[:mediapipe_list_len])

# print(mediapipe_list_len)
print(movenet_list)

x = len(np.arange(1,mediapipe_list_len+1))
print(x)

# Plotting the data
plt.plot(np.arange(1,mediapipe_list_len+1), mediapipe_list, label='Mediapipe frames')  # Plotting y1
plt.plot(np.arange(1,mediapipe_list_len+1), movenet_list[:mediapipe_list_len], label='Movenet frames')  # Plotting y2

# Adding labels and title
plt.xlabel('Frames')
plt.ylabel('Accuracy')
plt.title('Body visibility accuracy')

# Adding legend with custom names
plt.legend()

# Display the plot
plt.show()