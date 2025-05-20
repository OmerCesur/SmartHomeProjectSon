import cv2
import face_recognition
import dlib
import os
import numpy as np
import requests
from pathlib import Path

# -------------------------
# MODEL YOLLARI (Yüz tanıma için gerekli eğitimli modeller)
# -------------------------
HOME = str(Path.home())
MODEL_DIR = os.path.join(HOME, ".face_recognition")
SHAPE_PREDICTOR_PATH = os.path.join(MODEL_DIR, "shape_predictor_68_face_landmarks.dat")
FACE_ENCODER_PATH = os.path.join(MODEL_DIR, "dlib_face_recognition_resnet_model_v1.dat")

pose_predictor = dlib.shape_predictor(SHAPE_PREDICTOR_PATH)
face_encoder = dlib.face_recognition_model_v1(FACE_ENCODER_PATH)

# -------------------------
# TANINAN YÜZLERİ YÜKLE
# -------------------------
known_face_encodings = []
known_face_names = []
KNOWN_FACES_DIR = "known_faces"

if not os.path.exists(KNOWN_FACES_DIR):
    print(f"Directory '{KNOWN_FACES_DIR}' not found.")
    exit()

for filename in os.listdir(KNOWN_FACES_DIR):
    if filename.endswith(".jpg") or filename.endswith(".png"):
        path = os.path.join(KNOWN_FACES_DIR, filename)
        image = face_recognition.load_image_file(path)
        locations = face_recognition.face_locations(image)

        if locations:
            top, right, bottom, left = locations[0]
            rect = dlib.rectangle(left, top, right, bottom)
            shape = pose_predictor(image, rect)
            face_encoding = np.array(
                face_encoder.compute_face_descriptor(image.astype(np.uint8), shape, 1)
            )

            known_face_encodings.append(face_encoding)
            known_face_names.append(os.path.splitext(filename)[0])
            print(f"{filename} loaded successfully.")
        else:
            print(f"No face detected in {filename}.")

# -------------------------
# KAMERA BAŞLAT
# -------------------------
video_capture = cv2.VideoCapture(0)  # 0 veya 1 kullanılabilir
if not video_capture.isOpened():
    print("Unable to access the camera.")
    exit()
print("Camera initialized...")

# -------------------------
# BACKEND URL (Flask API ile bağlantı)
# -------------------------
BACKEND_URL = "http://127.0.0.1:5001/face-unlock"  # Doğru portu kullandığından emin ol

# -------------------------
# ANA DÖNGÜ
# -------------------------
while True:
    ret, frame = video_capture.read()
    if not ret:
        print("Failed to retrieve frame from camera.")
        break

    rgb_frame = frame[:, :, ::-1]
    face_locations = face_recognition.face_locations(rgb_frame, model="hog")

    if not face_locations:
        cv2.imshow("Face Recognition", frame)
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break
        continue

    for (top, right, bottom, left) in face_locations:
        rect = dlib.rectangle(left, top, right, bottom)
        shape = pose_predictor(rgb_frame, rect)
        face_encoding = np.array(
            face_encoder.compute_face_descriptor(rgb_frame.astype(np.uint8), shape, 1)
        )

        matches = face_recognition.compare_faces(known_face_encodings, face_encoding)
        name = "Unknown"

        if True in matches:
            match_index = matches.index(True)
            name = known_face_names[match_index]
            print(f"{name} recognized.")

            # Backend'e POST isteği gönder
            try:
                response = requests.post(BACKEND_URL, json={"name": name})
                print("Backend response:", response.json())
            except Exception as e:
                print("Error contacting backend:", e)
        else:
            print("Face not recognized.")

        cv2.rectangle(frame, (left, top), (right, bottom), (0, 255, 0), 2)
        cv2.putText(frame, name, (left + 6, bottom + 20), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (255, 255, 255), 2)

    cv2.imshow("Face Recognition", frame)
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

# -------------------------
# KAYNAKLARI SERBEST BIRAK
# -------------------------
video_capture.release()
cv2.destroyAllWindows()