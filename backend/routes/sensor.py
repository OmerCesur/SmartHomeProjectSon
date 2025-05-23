# routes/sensor.py: (sensör verileri için)

from flask import Blueprint, request, jsonify
from firebase_admin import messaging
from datetime import datetime
import logging
import os
from logging.handlers import RotatingFileHandler
from repositories.sensor_repository import SensorRepository
from repositories.notification_repository import NotificationRepository
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

# Repository'leri başlat
sensor_repository = None
notification_repository = None

def init_repositories(database):
    global sensor_repository, notification_repository
    sensor_repository = SensorRepository(database)
    notification_repository = NotificationRepository(database)

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
        "rooms": ["garaj", "giris"]
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
        logger.info(f"Bildirim başarıyla gönderildi: {response}")
        
        # Bildirimi veritabanına kaydet
        notification_repository.save_notification(
            title="Gaz Alarmı!",
            message=f"Gaz seviyesi kritik seviyede: {gas_level}\nLütfen hemen kontrol edin!",
            notification_type="gas_alert",
            severity=severity,
            sensor_value=gas_level
        )
    except Exception as e:
        logger.error(f"Bildirim gönderilirken hata oluştu: {str(e)}")

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
                sensor_status = sensor_repository.get_sensor_data(room, sensor_name)
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
        logger.error(f"Sensör listesi alınırken hata: {str(e)}")
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
            sensor_status = sensor_repository.get_sensor_data(room, sensor_name)
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
        logger.error(f"Sensör listesi alınırken hata: {str(e)}")
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

        sensor_data = sensor_repository.get_sensor_data(room, sensor_type)
        
        if sensor_data:
            return jsonify({
                "message": "Sensör verisi başarıyla alındı.",
                "room": room,
                "sensor_type": sensor_type,
                "data": sensor_data
            }), 200
        else:
            return jsonify({
                "error": "Sensör verisi bulunamadı.",
                "details": f"{room} odasındaki {sensor_type} sensörü için veri bulunamadı."
            }), 404

    except Exception as e:
        logger.error(f"Sensör verisi alınırken hata: {str(e)}")
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
        logger.info(f"Sensör verisi güncelleme isteği: {room}/{sensor_type}")
        
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

        data = request.get_json()
        if not data or 'value' not in data:
            return jsonify({
                "error": "Geçersiz istek formatı.",
                "details": "Request body'de 'value' alanı bulunmalıdır."
            }), 400

        value = data['value']
        sensor_info = SENSOR_TYPES[sensor_type]

        # Sensör tipine göre değer doğrulama
        if sensor_info["type"] == "binary":
            if value not in sensor_info["values"]:
                return jsonify({
                    "error": "Geçersiz değer.",
                    "details": f"Desteklenen değerler: {', '.join(sensor_info['values'])}"
                }), 400
        elif sensor_info["type"] == "float":
            try:
                value = float(value)
                if not (sensor_info["range"][0] <= value <= sensor_info["range"][1]):
                    return jsonify({
                        "error": "Değer aralık dışında.",
                        "details": f"Değer {sensor_info['range'][0]} ile {sensor_info['range'][1]} arasında olmalıdır."
                    }), 400
            except ValueError:
                return jsonify({
                    "error": "Geçersiz değer formatı.",
                    "details": "Değer sayısal olmalıdır."
                }), 400
        elif sensor_info["type"] == "integer":
            try:
                value = int(value)
                # Gaz sensörü için özel kontrol
                if sensor_type == "gas":
                    severity = None
                    for sev, (min_val, max_val) in sensor_info["severity"].items():
                        if min_val <= value <= max_val:
                            severity = sev
                            break
                    
                    if severity == "high":
                        send_gas_alert_notification(value, severity)
            except ValueError:
                return jsonify({
                    "error": "Geçersiz değer formatı.",
                    "details": "Değer tam sayı olmalıdır."
                }), 400

        # Sensör verisini güncelle
        updated_data = sensor_repository.update_sensor_data(room, sensor_type, value)
        
        return jsonify({
            "message": "Sensör verisi başarıyla güncellendi.",
            "room": room,
            "sensor_type": sensor_type,
            "data": updated_data
        }), 200

    except Exception as e:
        logger.error(f"Sensör verisi güncellenirken hata: {str(e)}")
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
        notifications = notification_repository.get_all_notifications()
        return jsonify({
            "message": "Bildirimler başarıyla alındı.",
            "notifications": notifications
        }), 200
    except Exception as e:
        logger.error(f"Bildirimler alınırken hata: {str(e)}")
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
        notification_repository.delete_notification(notification_id)
        return jsonify({
            "message": "Bildirim başarıyla silindi."
        }), 200
    except Exception as e:
        logger.error(f"Bildirim silinirken hata: {str(e)}")
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

@sensor_bp.route('/command/<room>/<command_type>', methods=['POST'])
def handle_command(room, command_type):
    """
    ESP32'den gelen komutları işler.
    
    Args:
        room (str): Oda adı (garaj, yatak_odasi, antre)
        command_type (str): Komut tipi (garage, curtain, door)
        
    Request Body:
        {
            "command": str  # "on" veya "off"
        }
        
    Returns:
        JSON formatında işlem sonucu
    """
    try:
        logger.info(f"Komut isteği: {room}/{command_type}")
        
        data = request.get_json()
        if not data or 'command' not in data:
            return jsonify({
                "error": "Geçersiz istek formatı.",
                "details": "Request body'de 'command' alanı bulunmalıdır."
            }), 400

        command = data['command']
        if command not in ['on', 'off']:
            return jsonify({
                "error": "Geçersiz komut.",
                "details": "Komut 'on' veya 'off' olmalıdır."
            }), 400

        # Komut tipini sensör tipine dönüştür
        sensor_type = command_type
        if command_type == 'garage':
            sensor_type = 'door'
        elif command_type == 'curtain':
            sensor_type = 'curtain'
        elif command_type == 'door':
            sensor_type = 'door'

        # Sensör verisini güncelle
        updated_data = sensor_repository.update_sensor_data(room, sensor_type, command)
        
        return jsonify({
            "message": "Komut başarıyla işlendi.",
            "room": room,
            "command_type": command_type,
            "command": command,
            "data": updated_data
        }), 200

    except Exception as e:
        logger.error(f"Komut işlenirken hata: {str(e)}")
        return jsonify({
            "error": "Komut işlenirken hata oluştu.",
            "details": str(e)
        }), 500