import tensorflow as tf
import numpy as np
from matplotlib import pyplot as plt
import cv2
from landmarks import LEFT_SHOULDER, LEFT_ELBOW, LEFT_WRIST, RIGHT_SHOULDER, RIGHT_ELBOW, RIGHT_WRIST, LEFT_EDGES, RIGHT_EDGES
from rectangle import draw_count_rectangle, draw_warning_rectangle
import winsound

correct_counter = 0
incorrect_counter = 0
total_count = 0
l_stage = 0 #0 - down, 1 - up
r_stage = 0 #0 - down, 1 - up
isCorrect = True
warning_frame = 0

interpreter = tf.lite.Interpreter(model_path='Movenet.tflite')
interpreter.allocate_tensors()

#Get input details 
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()


def draw_keypoints(frame, keypoints, threshold):

    y, x, c = frame.shape
    shaped = np.multiply(keypoints, [y,x,1])

    for i in range(len(shaped)):
        if i in [LEFT_SHOULDER, LEFT_ELBOW, LEFT_WRIST, RIGHT_SHOULDER, RIGHT_ELBOW, RIGHT_WRIST]:
            ky, kx, k_conf = shaped[i]
            if k_conf > threshold:
                cv2.circle(frame, (int(kx), int(ky)), 4, (255,0,0), -1)
  
def draw_connections(frame, keypoints, edges, threshold, correct):

    y, x, c = frame.shape
    shaped = np.multiply(keypoints, [y,x,1])

    for cord, color in edges:
        
        p1, p2 = cord
        y1, x1, c1 = shaped[p1]
        y2, x2, c2 = shaped[p2]

        if (c1 >= threshold) and (c2 >= threshold):
            cv2.line(frame, (int(x1), int(y1)), (int(x2), int(y2)), (0,255,255), 3)


def calculateAngle(a,b,c):
    
    radians = np.arctan2(c[1] - b[1],c[0] - b[0]) - np.arctan2(a[1] - b[1],a[0] - b[0])
    angles = np.abs(radians*180/np.pi)
    if angles > 180:
        angles = 360-angles 

    return angles

#Cam capture
cap=cv2.VideoCapture(1)
while cap.isOpened():
    ret,frame =cap.read()
    img = frame.copy()
    img = tf.image.resize_with_pad(np.expand_dims(img, axis=0), 192, 192)
    input_image = tf.cast(img, dtype=tf.float32)

    # Make predictions
    interpreter.set_tensor(input_details[0]['index'], np.array(input_image))
    interpreter.invoke()
    keypoints_with_score = np.squeeze(interpreter.get_tensor(output_details[0]['index']))

    # Warning block
    if(warning_frame):
        draw_warning_rectangle(frame, 'Maintian tension at biceps')
        warning_frame -= 1


    if(keypoints_with_score[LEFT_SHOULDER][2] > 0.48 and keypoints_with_score[LEFT_ELBOW][2] > 0.48 and keypoints_with_score[LEFT_WRIST][2] > 0.48 and keypoints_with_score[RIGHT_SHOULDER][2] > 0.48 and keypoints_with_score[RIGHT_ELBOW][2] > 0.48 and keypoints_with_score[RIGHT_WRIST][2] > 0.48):

        left_arm_angle = calculateAngle(keypoints_with_score[LEFT_SHOULDER][:2], keypoints_with_score[LEFT_ELBOW][:2], keypoints_with_score[LEFT_WRIST][:2])
        right_arm_angle = calculateAngle(keypoints_with_score[RIGHT_SHOULDER][:2], keypoints_with_score[RIGHT_ELBOW][:2], keypoints_with_score[RIGHT_WRIST][:2])
        
        if  (left_arm_angle>15 and left_arm_angle < 60) and (not l_stage):

            l_stage = 1
            total_count += 1

            if(isCorrect):
                correct_counter += 1
            else:
                incorrect_counter += 1
            
            isCorrect = True

        if (left_arm_angle >140):

            l_stage = 0

            if(left_arm_angle > 179.5 and isCorrect and total_count > 1):
                print('Maintain tension at biceps')
                isCorrect = False
                warning_frame += 50

                # Beep sound
                frequency = 2500  # Set frequency to 2500 Hertz
                duration = 550  # Set duration to 1000 milliseconds (550 milli-second)
                winsound.Beep(frequency, duration)
                
        
        if  (right_arm_angle>15 and right_arm_angle < 60) and (not r_stage):

            r_stage = 1
            total_count += 1
            print(total_count)

            if(isCorrect):
                correct_counter += 1
            else:
                incorrect_counter += 1
            
            isCorrect = True

        if (right_arm_angle >140):

            r_stage = 0

            if(right_arm_angle > 179.5 and isCorrect and total_count > 1):
                warning_frame += 50
                isCorrect = False

                # Beep sound
                frequency = 2500  # Set frequency to 2500 Hertz
                duration = 550  # Set duration to 1000 milliseconds (550 milli-second)
                winsound.Beep(frequency, duration)
        


    # To draw the landmarks
    draw_keypoints(frame, keypoints_with_score, 0.5)
    draw_connections(frame, keypoints_with_score, LEFT_EDGES.items(), 0.5, True)
    draw_connections(frame, keypoints_with_score, RIGHT_EDGES.items(), 0.5, True)

    draw_count_rectangle(frame, 'Total count', total_count, 0)
    draw_count_rectangle(frame, 'Correct count', correct_counter, 1)
    draw_count_rectangle(frame, 'Incorrect count', incorrect_counter, 2)

    cv2.imshow('MoveNet Lightning',frame)
    if cv2.waitKey(10) & 0xFF==ord('q'):
        break

cap.release()   
cv2.destroyAllWindows()