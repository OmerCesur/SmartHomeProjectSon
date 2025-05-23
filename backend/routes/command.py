# routes/command.py: (kullanıcı kontrolü için)

from flask import Blueprint, request, jsonify
from datetime import datetime
import logging
from repositories.command_repository import CommandRepository

# Blueprint tanımlanıyor
command_bp = Blueprint('command', __name__)

# Global repository instance
command_repository = None

def init_repository(database):
    """Repository'yi başlat"""
    global command_repository
    command_repository = CommandRepository(database)

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
    },
    "temperature": {
        "type": "numeric",
        "min": 16,
        "max": 30,
        "description": "Sıcaklık ayarı",
        "rooms": ["yatak_odasi", "salon"]
    }
}

@command_bp.route('/command/<room>/<command_type>', methods=['GET'])
def get_command_status(room, command_type):
    """
    Belirli bir odadaki komutun durumunu döner.
    
    Args:
        room (str): Oda adı
        command_type (str): Komut tipi (light, curtain, door, temperature)
        
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

        command_status = command_repository.get_command_status(room, command_type)

        return jsonify({
            "message": f"{room} odasındaki {command_type} durumu başarıyla alındı.",
            "room": room,
            "command_type": command_type,
            "command_info": COMMAND_TYPES[command_type],
            "status": command_status
        }), 200

    except Exception as e:
        logging.error(f"Komut durumu alınırken hata: {str(e)}")
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
        command_type (str): Komut tipi (light, curtain, door, temperature)
        
    Request Body:
        {
            "command": "on"/"off" veya sayısal değer (temperature için)
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
        if command_config["type"] == "binary":
            if command not in command_config["values"]:
                return jsonify({
                    "error": "Geçersiz komut.",
                    "details": f"{command_type} için geçerli komutlar: {', '.join(command_config['values'])}"
                }), 400
        elif command_config["type"] == "numeric":
            try:
                command = float(command)
                if not (command_config["min"] <= command <= command_config["max"]):
                    return jsonify({
                        "error": "Geçersiz sıcaklık değeri.",
                        "details": f"Sıcaklık {command_config['min']} ile {command_config['max']} arasında olmalı."
                    }), 400
            except ValueError:
                return jsonify({
                    "error": "Geçersiz sıcaklık değeri.",
                    "details": "Sıcaklık sayısal bir değer olmalı."
                }), 400

        # Komutu gönder
        command_data = command_repository.send_command(room, command_type, command)

        return jsonify({
            "message": f"{room} odasındaki {command_type} için komut gönderildi.",
            "room": room,
            "command_type": command_type,
            "command": command,
            "timestamp": command_data["timestamp"]
        }), 200

    except Exception as e:
        logging.error(f"Komut gönderilirken hata: {str(e)}")
        return jsonify({
            "error": "Komut gönderilirken hata oluştu.",
            "details": str(e)
        }), 500

@command_bp.route('/command/<room>/<command_type>/history', methods=['GET'])
def get_command_history(room, command_type):
    """
    Belirli bir odadaki komutun geçmişini döner.
    
    Args:
        room (str): Oda adı
        command_type (str): Komut tipi
        
    Query Parameters:
        limit (int): Dönecek maksimum kayıt sayısı (varsayılan: 10)
        
    Returns:
        JSON formatında komut geçmişi
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

        limit = request.args.get('limit', default=10, type=int)
        history = command_repository.get_command_history(room, command_type, limit)

        return jsonify({
            "message": f"{room} odasındaki {command_type} komut geçmişi başarıyla alındı.",
            "room": room,
            "command_type": command_type,
            "history": history
        }), 200

    except Exception as e:
        logging.error(f"Komut geçmişi alınırken hata: {str(e)}")
        return jsonify({
            "error": "Komut geçmişi alınırken hata oluştu.",
            "details": str(e)
        }), 500