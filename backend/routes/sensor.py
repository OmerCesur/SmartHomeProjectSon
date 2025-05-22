# routes/sensor.py: (sensör verileri için)

from flask import Blueprint, request, jsonify
from firebase_admin import db, messaging
from datetime import datetime
import logging
import os
from logging.handlers import RotatingFileHandler
# from utils.face_recognition_module import FaceRecognitionModule  # Face ID şimdilik devre dışı

# Logging yapılandırması
log_dir = os.path.join(os.path.dirname(os.path.dirname(__file__)), "logs")
if not os.path.exists(log_dir):
    os.makedirs(log_dir)

log_file = os.path.join(log_dir, f"sensor_{datetime.now().strftime('%Y%m%d')}.log")
file_handler = RotatingFileHandler(log_file, maxBytes=10240000, backupCount=10)
file_handler.setFormatter(logging.Formatter(
    '%(asctime)s %(levelname)s: %(message)s [in %(pathname)s:%(lineno)d]'
))
file_handler.setLevel(logging.INFO)

console_handler = logging.StreamHandler()
console_handler.setFormatter(logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s'))
console_handler.setLevel(logging.DEBUG)

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
logger.addHandler(file_handler)
logger.addHandler(console_handler)

# Blueprint tanımlanıyor
sensor_bp = Blueprint('sensor', __name__)

# Face ID modülünü başlat
# face_recognition = FaceRecognitionModule()  # Face ID şimdilik devre dışı

# Desteklenen sensör tipleri
SENSOR_TYPES = {
    "light": {
        "type": "binary",
        "values": ["on", "off"],
        "description": "Aydınlatma sensörü",
        "rooms": ["yatak_odasi", "salon", "garaj", "banyo", "giris"]
    },
    "curtain": {
        "type": "binary",
        "values": ["open", "close"],
        "description": "Perde sensörü",
        "rooms": ["yatak_odasi"]
    },
    "door": {
        "type": "binary",
        "values": ["on", "off"],
        "description": "Kapı sensörü",
        "rooms": ["garaj"]
    },
    "temperature": {
        "type": "float",
        "model": "SHT35",
        "range": [0, 125],
        "description": "Sıcaklık sensörü",
        "rooms": ["salon"]
    },
    "gas": {
        "type": "integer",
        "model": "MQ-2",
        "severity": {
            "low": [0, 300],
            "medium": [301, 700],
            "high": [701, float('inf')]
        },
        "description": "Gaz sensörü",
        "rooms": ["salon"]
    },
    "face_id": {
        "type": "binary",
        "values": ["detected", "not_detected"],
        "description": "Yüz tanıma sensörü",
        "rooms": ["giris"]
    }
}

def send_gas_alert_notification(gas_level, severity):
    """
    Kritik gaz seviyesi durumunda bildirim gönderir.
    
    Args:
        gas_level (int): Gaz seviyesi
        severity (str): Gaz seviyesi kategorisi (high durumunda bildirim gönderilir)
    """
    if severity != "high":
        return

    # Bildirim mesajını hazırla
    message = messaging.Message(
        notification=messaging.Notification(
            title="Gaz Alarmı!",
            body=f"Gaz seviyesi kritik seviyede: {gas_level}\nLütfen hemen kontrol edin!"
        ),
        data={
            "severity": severity,
            "gas_level": str(gas_level),
            "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        },
        topic="gas_alert"  # Mobilde bu topic'e abone olanlar alır
    )
    
    try:
        response = messaging.send(message)
        print(f"Bildirim başarıyla gönderildi: {response}")
    except Exception as e:
        print(f"Bildirim gönderilirken hata oluştu: {str(e)}")

def save_notification_to_db(title, message, severity, gas_level, timestamp):
    ref = db.reference("notifications")
    notification_data = {
        "title": title,
        "message": message,
        "type": "gas_alert",
        "severity": severity,
        "gas_level": gas_level,
        "timestamp": timestamp,
        "read": False
    }
    ref.push(notification_data)

@sensor_bp.route('/sensors', methods=['GET'])
def get_all_sensors():
    """
    Tüm odaların sensör listesini döner.
    
    Returns:
        JSON formatında sensör listesi
    """
    try:
        # Oda bazlı sensör listesi oluştur
        room_sensors = {}
        for sensor_type, sensor_info in SENSOR_TYPES.items():
            for room in sensor_info["rooms"]:
                if room not in room_sensors:
                    room_sensors[room] = {"sensors": {}}
                room_sensors[room]["sensors"][sensor_type] = sensor_info

        # Her odanın sensörlerinin son durumlarını al
        for room in room_sensors:
            for sensor_name in room_sensors[room]["sensors"]:
                ref = db.reference(f"sensors/{room}/{sensor_name}")
                sensor_status = ref.get()
                if sensor_status:
                    room_sensors[room]["sensors"][sensor_name]["status"] = sensor_status
                else:
                    room_sensors[room]["sensors"][sensor_name]["status"] = {
                        "value": None,
                        "timestamp": None
                    }

        return jsonify({
            "message": "Tüm odaların sensör listesi başarıyla alındı.",
            "rooms": room_sensors
        }), 200

    except Exception as e:
        return jsonify({
            "error": "Sensör listesi alınırken hata oluştu.",
            "details": str(e)
        }), 500

@sensor_bp.route('/sensors/<room>', methods=['GET'])
def get_room_sensors(room):
    """
    Belirli bir odanın sensör listesini döner.
    
    Args:
        room (str): Oda adı
        
    Returns:
        JSON formatında sensör listesi
    """
    try:
        # Odadaki sensörleri bul
        room_sensors = {}
        for sensor_type, sensor_info in SENSOR_TYPES.items():
            if room in sensor_info["rooms"]:
                room_sensors[sensor_type] = sensor_info

        if not room_sensors:
            return jsonify({
                "error": "Geçersiz oda.",
                "details": f"Desteklenen odalar: {', '.join(set([room for sensor in SENSOR_TYPES.values() for room in sensor['rooms']]))}"
            }), 400

        # Sensörlerin son durumlarını al
        for sensor_name in room_sensors:
            ref = db.reference(f"sensors/{room}/{sensor_name}")
            sensor_status = ref.get()
            if sensor_status:
                room_sensors[sensor_name]["status"] = sensor_status
            else:
                room_sensors[sensor_name]["status"] = {
                    "value": None,
                    "timestamp": None
                }

        return jsonify({
            "message": f"{room} odasının sensör listesi başarıyla alındı.",
            "room": room,
            "sensors": room_sensors
        }), 200

    except Exception as e:
        return jsonify({
            "error": "Sensör listesi alınırken hata oluştu.",
            "details": str(e)
        }), 500

@sensor_bp.route('/sensor-data/<room>/<sensor_type>', methods=['GET'])
def get_sensor_data(room, sensor_type):
    """
    Belirli bir odadaki sensörün verisini döner.
    
    Args:
        room (str): Oda adı
        sensor_type (str): Sensör tipi
        
    Returns:
        JSON formatında sensör verisi
    """
    try:
        logger.info(f"Sensör verisi isteği: {room}/{sensor_type}")
        
        if sensor_type not in SENSOR_TYPES:
            logger.warning(f"Geçersiz sensör tipi: {sensor_type}")
            return jsonify({
                "error": "Geçersiz sensör tipi.",
                "details": f"Desteklenen sensörler: {', '.join(SENSOR_TYPES.keys())}"
            }), 400

        if room not in SENSOR_TYPES[sensor_type]["rooms"]:
            logger.warning(f"Geçersiz oda-sensör kombinasyonu: {room}/{sensor_type}")
            return jsonify({
                "error": "Geçersiz oda-sensör kombinasyonu.",
                "details": f"{sensor_type} sensörü {room} odasında bulunmuyor."
            }), 400

        # Firebase'den sensör verisini al
        ref = db.reference(f"sensors/{room}/{sensor_type}")
        logger.debug(f"Firebase referansı: sensors/{room}/{sensor_type}")
        
        sensor_data = ref.get()
        logger.debug(f"Firebase'den alınan veri: {sensor_data}")

        if not sensor_data:
            logger.info(f"Sensör verisi bulunamadı: {room}/{sensor_type}")
            sensor_data = {
                "value": None,
                "timestamp": None
            }

        return jsonify({
            "message": f"{room} odasındaki {sensor_type} verisi başarıyla alındı.",
            "room": room,
            "sensor_type": sensor_type,
            "sensor_info": SENSOR_TYPES[sensor_type],
            "data": sensor_data
        }), 200

    except Exception as e:
        logger.error(f"Sensör verisi alınırken hata oluştu: {str(e)}", exc_info=True)
        return jsonify({
            "error": "Sensör verisi alınırken hata oluştu.",
            "details": str(e)
        }), 500

@sensor_bp.route('/sensor-data/<room>/<sensor_type>', methods=['POST'])
def update_sensor_data(room, sensor_type):
    """
    Belirli bir odadaki sensörün verisini günceller.
    
    Args:
        room (str): Oda adı
        sensor_type (str): Sensör tipi
        
    Request Body:
        {
            "value": <sensor_value>  # Sensör tipine göre değişir
        }
        
    Returns:
        JSON formatında işlem sonucu
    """
    try:
        if sensor_type not in SENSOR_TYPES:
            return jsonify({
                "error": "Geçersiz sensör tipi.",
                "details": f"Desteklenen sensörler: {', '.join(SENSOR_TYPES.keys())}"
            }), 400

        if room not in SENSOR_TYPES[sensor_type]["rooms"]:
            return jsonify({
                "error": "Geçersiz oda-sensör kombinasyonu.",
                "details": f"{sensor_type} sensörü {room} odasında bulunmuyor."
            }), 400

        # Face ID sensörü için özel işlem
        if sensor_type == "face_id":
            face_data = face_recognition.recognize_face()
            value = "detected" if face_data["detected"] else "not_detected"
            sensor_data = {
                "value": value,
                "timestamp": face_data["timestamp"],
                "name": face_data["name"]
            }
        else:
            data = request.get_json()
            if not data or "value" not in data:
                return jsonify({
                    "error": "Sensör verisi eksik.",
                    "details": "value alanı gerekli"
                }), 400

            value = data["value"]
            sensor_config = SENSOR_TYPES[sensor_type]

            # Sensör tipine göre veri kontrolü
            if sensor_config["type"] == "binary":
                if value not in sensor_config["values"]:
                    return jsonify({
                        "error": "Geçersiz değer.",
                        "details": f"Değer {sensor_config['values']} arasından olmalı"
                    }), 400
            elif sensor_config["type"] == "float":
                if not isinstance(value, (int, float)):
                    return jsonify({
                        "error": "Geçersiz veri tipi.",
                        "details": "Float tipinde değer gerekli"
                    }), 400
                if not (sensor_config["range"][0] <= value <= sensor_config["range"][1]):
                    return jsonify({
                        "error": "Geçersiz değer.",
                        "details": f"Değer {sensor_config['range'][0]} ile {sensor_config['range'][1]} arasında olmalı"
                    }), 400
            elif sensor_config["type"] == "integer":
                if not isinstance(value, int):
                    return jsonify({
                        "error": "Geçersiz veri tipi.",
                        "details": "Integer tipinde değer gerekli"
                    }), 400
                if sensor_type == "gas":
                    # Gaz seviyesi için severity hesapla
                    severity = None
                    for level, (min_val, max_val) in sensor_config["severity"].items():
                        if min_val <= value <= max_val:
                            severity = level
                            break
                    if severity is None:
                        return jsonify({
                            "error": "Geçersiz gaz seviyesi.",
                            "details": "Gaz seviyesi 0'dan büyük olmalı"
                        }), 400

            # Sensörün güncellenme zamanı
            timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

            # Firebase'e yazılacak veri
            sensor_data = {
                "value": value,
                "timestamp": timestamp
            }

            # Gaz sensörü için severity ekle
            if sensor_type == "gas":
                sensor_data["severity"] = severity
                # Kritik seviyede ise bildirim gönder
                send_gas_alert_notification(value, severity)
                save_notification_to_db(
                    title="Gaz Alarmı!",
                    message=f"Gaz seviyesi kritik seviyede: {value}\nLütfen hemen kontrol edin!",
                    severity=severity,
                    gas_level=value,
                    timestamp=timestamp
                )

        # Sensör geçmişini kaydet
        history_ref = db.reference(f"sensor_history/{room}/{sensor_type}")
        history_ref.push(sensor_data)

        # Ana sensör verisini güncelle
        db.reference(f"sensors/{room}/{sensor_type}").set(sensor_data)

        return jsonify({
            "message": f"{room} odasındaki {sensor_type} verisi güncellendi.",
            "room": room,
            "sensor_type": sensor_type,
            "data": sensor_data
        }), 200

    except Exception as e:
        return jsonify({
            "error": "Sensör verisi güncellenirken hata oluştu.",
            "details": str(e)
        }), 500

@sensor_bp.route('/notifications', methods=['GET'])
def get_notifications():
    """
    Tüm bildirimleri listeler.
    
    Returns:
        JSON formatında bildirim listesi
    """
    try:
        # Firebase'den bildirimleri al
        ref = db.reference("notifications")
        notifications = ref.get()

        if not notifications:
            return jsonify({
                "message": "Henüz bildirim yok.",
                "notifications": []
            }), 200

        # Bildirimleri tarihe göre sırala (en yeni en üstte)
        notifications_list = []
        for notification_id, notification in notifications.items():
            notification["id"] = notification_id
            notifications_list.append(notification)

        notifications_list.sort(key=lambda x: x["timestamp"], reverse=True)

        return jsonify({
            "message": "Bildirimler başarıyla alındı.",
            "notifications": notifications_list
        }), 200

    except Exception as e:
        return jsonify({
            "error": "Bildirimler alınırken hata oluştu.",
            "details": str(e)
        }), 500

@sensor_bp.route('/notifications/<notification_id>', methods=['DELETE'])
def delete_notification(notification_id):
    """
    Belirli bir bildirimi siler.
    
    Args:
        notification_id (str): Bildirim ID'si
        
    Returns:
        JSON formatında işlem sonucu
    """
    try:
        # Firebase'den bildirimi sil
        ref = db.reference(f"notifications/{notification_id}")
        ref.delete()

        return jsonify({
            "message": "Bildirim başarıyla silindi.",
            "notification_id": notification_id
        }), 200

    except Exception as e:
        return jsonify({
            "error": "Bildirim silinirken hata oluştu.",
            "details": str(e)
        }), 500

@sensor_bp.route('/auth/login', methods=['POST'])
def login():
    """
    Kullanıcı girişi yapar.
    
    Request Body:
        {
            "username": str,
            "password": str
        }
        
    Returns:
        JSON formatında giriş sonucu
    """
    try:
        data = request.get_json()
        if not data or "username" not in data or "password" not in data:
            return jsonify({
                "error": "Eksik bilgi.",
                "details": "username ve password alanları gerekli"
            }), 400

        # Firebase'den kullanıcı bilgilerini kontrol et
        ref = db.reference(f"users/{data['username']}")
        user_data = ref.get()

        if not user_data or user_data["password"] != data["password"]:
            return jsonify({
                "error": "Geçersiz kullanıcı adı veya şifre."
            }), 401

        # Giriş zamanını kaydet
        ref.update({
            "last_login": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        })

        return jsonify({
            "message": "Giriş başarılı.",
            "user": {
                "username": data["username"],
                "name": user_data.get("name", ""),
                "role": user_data.get("role", "user")
            }
        }), 200

    except Exception as e:
        return jsonify({
            "error": "Giriş yapılırken hata oluştu.",
            "details": str(e)
        }), 500

@sensor_bp.route('/auth/logout', methods=['POST'])
def logout():
    """
    Kullanıcı çıkışı yapar.
    
    Returns:
        JSON formatında çıkış sonucu
    """
    try:
        # Çıkış zamanını kaydet
        ref = db.reference(f"users/{request.json.get('username')}")
        ref.update({
            "last_logout": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        })

        return jsonify({
            "message": "Çıkış başarılı."
        }), 200

    except Exception as e:
        return jsonify({
            "error": "Çıkış yapılırken hata oluştu.",
            "details": str(e)
        }), 500