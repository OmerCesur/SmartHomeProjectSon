# routes/command.py: (kullanıcı kontrolü için)

from flask import Blueprint, request, jsonify
from firebase_admin import db
from datetime import datetime

# Blueprint tanımlanıyor
command_bp = Blueprint('command', __name__)

# Desteklenen komut tipleri
COMMAND_TYPES = {
    "light": {
        "type": "binary",
        "values": ["on", "off"],
        "description": "Aydınlatma kontrolü",
        "rooms": ["yatak_odasi", "salon", "garaj", "banyo", "giris"]
    },
    "curtain": {
        "type": "binary",
        "values": ["on", "off"],
        "description": "Perde kontrolü (aç/kapa)",
        "rooms": ["yatak_odasi"]
    },
    "door": {
        "type": "binary",
        "values": ["on", "off"],
        "description": "Kapı kontrolü (aç/kapa)",
        "rooms": ["garaj"]
    }
}

@command_bp.route('/command/<room>/<command_type>', methods=['GET'])
def get_command_status(room, command_type):
    """
    Belirli bir odadaki komutun durumunu döner.
    
    Args:
        room (str): Oda adı
        command_type (str): Komut tipi (light, curtain, door)
        
    Returns:
        JSON formatında komut durumu
    """
    try:
        if command_type not in COMMAND_TYPES:
            return jsonify({
                "error": "Geçersiz komut tipi.",
                "details": f"Desteklenen komutlar: {', '.join(COMMAND_TYPES.keys())}"
            }), 400

        if room not in COMMAND_TYPES[command_type]["rooms"]:
            return jsonify({
                "error": "Geçersiz oda-komut kombinasyonu.",
                "details": f"{command_type} komutu {room} odasında bulunmuyor."
            }), 400

        # Firebase'den komut durumunu al
        ref = db.reference(f"commands/{room}/{command_type}")
        command_status = ref.get()

        if not command_status:
            command_status = {
                "value": "off",
                "timestamp": None
            }

        return jsonify({
            "message": f"{room} odasındaki {command_type} durumu başarıyla alındı.",
            "room": room,
            "command_type": command_type,
            "command_info": COMMAND_TYPES[command_type],
            "status": command_status
        }), 200

    except Exception as e:
        return jsonify({
            "error": "Komut durumu alınırken hata oluştu.",
            "details": str(e)
        }), 500

@command_bp.route('/command/<room>/<command_type>', methods=['POST'])
def send_command(room, command_type):
    """
    Belirli bir odadaki komutu gönderir.
    
    Args:
        room (str): Oda adı
        command_type (str): Komut tipi (light, curtain, door)
        
    Request Body:
        {
            "command": "on" veya "off"
        }
        
    Returns:
        JSON formatında işlem sonucu
    """
    try:
        if command_type not in COMMAND_TYPES:
            return jsonify({
                "error": "Geçersiz komut tipi.",
                "details": f"Desteklenen komutlar: {', '.join(COMMAND_TYPES.keys())}"
            }), 400

        if room not in COMMAND_TYPES[command_type]["rooms"]:
            return jsonify({
                "error": "Geçersiz oda-komut kombinasyonu.",
                "details": f"{command_type} komutu {room} odasında bulunmuyor."
            }), 400

        data = request.get_json()
        if not data or "command" not in data:
            return jsonify({
                "error": "Komut bilgisi eksik.",
                "details": "command alanı gerekli"
            }), 400

        command = data["command"]
        command_config = COMMAND_TYPES[command_type]

        # Komut validasyonu
        if command not in command_config["values"]:
            return jsonify({
                "error": "Geçersiz komut.",
                "details": f"{command_type} için geçerli komutlar: {', '.join(command_config['values'])}"
            }), 400

        # 🔹 Komutun gönderilme zamanı
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

        # 🔹 Firebase'e veri yazılacak yol
        ref_path = f"commands/{room}/{command_type}"

        # 🔹 Komut ve zaman bilgisi birlikte yazılır
        command_data = {
            "value": command,
            "timestamp": timestamp
        }

        # Komut geçmişini de kaydet
        history_ref = db.reference(f"command_history/{room}/{command_type}")
        history_ref.push(command_data)

        # Ana komut verisini güncelle
        db.reference(ref_path).set(command_data)

        return jsonify({
            "message": f"{room} odasındaki {command_type} için komut gönderildi.",
            "room": room,
            "command_type": command_type,
            "command": command,
            "timestamp": timestamp
        }), 200

    except Exception as e:
        return jsonify({
            "error": "Komut gönderilirken hata oluştu.",
            "details": str(e)
        }), 500