import cv2
import numpy as np
import face_recognition
import os
import logging
from datetime import datetime

class FaceRecognitionModule:
    def __init__(self):
        self.known_face_encodings = []
        self.known_face_names = []
        self.face_locations = []
        self.face_encodings = []
        self.face_names = []
        self.process_this_frame = True
        self.logger = logging.getLogger(__name__)
        
        # Yüz veritabanı dizini
        self.faces_dir = os.path.join(os.path.dirname(os.path.dirname(__file__)), "data", "faces")
        if not os.path.exists(self.faces_dir):
            os.makedirs(self.faces_dir)
            
        # Kayıtlı yüzleri yükle
        self.load_known_faces()
        
    def load_known_faces(self):
        """Kayıtlı yüzleri yükler."""
        try:
            for filename in os.listdir(self.faces_dir):
                if filename.endswith(".jpg") or filename.endswith(".png"):
                    # Dosya adından kişi adını al (uzantıyı çıkar)
                    name = os.path.splitext(filename)[0]
                    
                    # Yüz resmini yükle
                    image_path = os.path.join(self.faces_dir, filename)
                    image = face_recognition.load_image_file(image_path)
                    
                    # Yüz kodlamasını al
                    face_encoding = face_recognition.face_encodings(image)[0]
                    
                    # Listelere ekle
                    self.known_face_encodings.append(face_encoding)
                    self.known_face_names.append(name)
                    
            self.logger.info(f"{len(self.known_face_names)} adet yüz yüklendi.")
        except Exception as e:
            self.logger.error(f"Yüzler yüklenirken hata: {str(e)}")
            
    def add_face(self, image, name):
        """
        Yeni bir yüz ekler.
        
        Args:
            image: Yüz resmi (numpy array)
            name: Kişi adı
            
        Returns:
            bool: İşlem başarılı ise True
        """
        try:
            # Yüz kodlamasını al
            face_encoding = face_recognition.face_encodings(image)[0]
            
            # Yüzü kaydet
            filename = f"{name}_{datetime.now().strftime('%Y%m%d_%H%M%S')}.jpg"
            filepath = os.path.join(self.faces_dir, filename)
            cv2.imwrite(filepath, cv2.cvtColor(image, cv2.COLOR_RGB2BGR))
            
            # Listelere ekle
            self.known_face_encodings.append(face_encoding)
            self.known_face_names.append(name)
            
            self.logger.info(f"Yeni yüz eklendi: {name}")
            return True
        except Exception as e:
            self.logger.error(f"Yüz eklenirken hata: {str(e)}")
            return False
            
    def recognize_face(self, image):
        """
        Görüntüdeki yüzü tanır.
        
        Args:
            image: Görüntü (numpy array)
            
        Returns:
            str: Tanınan kişinin adı, tanınmazsa "unknown"
        """
        try:
            # Yüz konumlarını bul
            face_locations = face_recognition.face_locations(image)
            
            if not face_locations:
                return "not_detected"
                
            # Yüz kodlamalarını al
            face_encodings = face_recognition.face_encodings(image, face_locations)
            
            # Her yüz için en yakın eşleşmeyi bul
            for face_encoding in face_encodings:
                matches = face_recognition.compare_faces(self.known_face_encodings, face_encoding)
                name = "unknown"
                
                if True in matches:
                    first_match_index = matches.index(True)
                    name = self.known_face_names[first_match_index]
                    
                return name
                
        except Exception as e:
            self.logger.error(f"Yüz tanıma hatası: {str(e)}")
            return "error"
            
    def delete_face(self, name):
        """
        Kayıtlı bir yüzü siler.
        
        Args:
            name: Silinecek kişinin adı
            
        Returns:
            bool: İşlem başarılı ise True
        """
        try:
            # Kişinin tüm yüz dosyalarını bul
            files_to_delete = [f for f in os.listdir(self.faces_dir) if f.startswith(name + "_")]
            
            # Dosyaları sil
            for filename in files_to_delete:
                filepath = os.path.join(self.faces_dir, filename)
                os.remove(filepath)
                
            # Listelerden kaldır
            indices = [i for i, x in enumerate(self.known_face_names) if x == name]
            for index in sorted(indices, reverse=True):
                del self.known_face_names[index]
                del self.known_face_encodings[index]
                
            self.logger.info(f"Yüz silindi: {name}")
            return True
        except Exception as e:
            self.logger.error(f"Yüz silinirken hata: {str(e)}")
            return False 