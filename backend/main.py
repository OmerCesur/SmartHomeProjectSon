# main.py

from flask import Flask, jsonify, request
from flask_cors import CORS
from firebase_admin import db, credentials, initialize_app
import logging
from datetime import datetime
import os

# Import blueprints
from routes.status import status_bp
from routes.sensor import sensor_bp, init_repositories
from routes.command import command_bp, init_repository as init_command_repository
from routes.face_id import face_id_bp

# Import configs
from config.logging_config import setup_logging

# Flask uygulamasını oluştur
app = Flask(__name__)
CORS(app)

# Logging'i yapılandır
logger = setup_logging('smart_home')

# Blueprint'leri kaydet
app.register_blueprint(status_bp, url_prefix='/api')
app.register_blueprint(sensor_bp, url_prefix='/api')
app.register_blueprint(command_bp, url_prefix='/api')
app.register_blueprint(face_id_bp, url_prefix='/api')

# Firebase configuration
firebase_config = {
    "type": os.getenv('FIREBASE_TYPE'),
    "project_id": os.getenv('FIREBASE_PROJECT_ID'),
    "private_key_id": os.getenv('FIREBASE_PRIVATE_KEY_ID'),
    "private_key": os.getenv('FIREBASE_PRIVATE_KEY').replace('\\n', '\n'),
    "client_email": os.getenv('FIREBASE_CLIENT_EMAIL'),
    "client_id": os.getenv('FIREBASE_CLIENT_ID'),
    "auth_uri": os.getenv('FIREBASE_AUTH_URI'),
    "token_uri": os.getenv('FIREBASE_TOKEN_URI'),
    "auth_provider_x509_cert_url": os.getenv('FIREBASE_AUTH_PROVIDER_X509_CERT_URL'),
    "client_x509_cert_url": os.getenv('FIREBASE_CLIENT_X509_CERT_URL')
}

try:
    cred = credentials.Certificate(firebase_config)
    firebase_app = initialize_app(cred, {
        'databaseURL': f"https://{firebase_config['project_id']}-default-rtdb.europe-west1.firebasedatabase.app"
    })
    db_ref = db.reference('/')
    init_repositories(db_ref)
    init_command_repository(db_ref)
    logger.info("Firebase başarıyla başlatıldı")
    
    # Test bağlantısı
    try:
        test_data = db_ref.get()
        logger.info("Firebase bağlantı testi başarılı")
    except Exception as e:
        logger.error(f"Firebase bağlantı testi başarısız: {str(e)}")
        raise
except Exception as e:
    logger.error(f"Firebase başlatılırken hata oluştu: {str(e)}", exc_info=True)
    raise

@app.route('/api/sensors/bulk-update', methods=['POST'])
def bulk_update_sensors():
    """
    Toplu sensör güncellemesi yapar.
    
    Request Body:
        {
            "sensors": [
                {
                    "room": "yatak_odasi",
                    "type": "temperature",
                    "value": 25.5
                },
                {
                    "room": "salon",
                    "type": "light",
                    "value": "on"
                }
            ]
        }
    """
    try:
        data = request.get_json()
        if not data or "sensors" not in data:
            return jsonify({
                "error": "Geçersiz veri formatı.",
                "details": "sensors alanı gerekli"
            }), 400

        sensors = data["sensors"]
        if not isinstance(sensors, list):
            return jsonify({
                "error": "Geçersiz veri formatı.",
                "details": "sensors bir liste olmalı"
            }), 400

        # Her sensör için güncelleme yap
        for sensor in sensors:
            if not all(key in sensor for key in ["room", "type", "value"]):
                return jsonify({
                    "error": "Geçersiz sensör verisi.",
                    "details": "Her sensör room, type ve value alanlarını içermeli"
                }), 400

            # Sensör verisini güncelle
            ref = db.reference(f"sensors/{sensor['room']}/{sensor['type']}")
            ref.set({
                "value": sensor["value"],
                "timestamp": datetime.now().isoformat()
            })

        return jsonify({
            "message": "Sensörler başarıyla güncellendi.",
            "updated_sensors": len(sensors)
        }), 200

    except Exception as e:
        logger.error(f"Toplu sensör güncellemesi sırasında hata: {str(e)}")
        return jsonify({
            "error": "Sensörler güncellenirken hata oluştu.",
            "details": str(e)
        }), 500

@app.route('/sensors/<room>/temperature', methods=['GET'])
def get_temperature(room):
    try:
        ref = db.reference(f"sensors/{room}/temperature")
        data = ref.get()
        if data:
            return jsonify({
                "room": room,
                "temperature": data.get("value"),
                "timestamp": data.get("timestamp")
            }), 200
        else:
            return jsonify({"error": "No data found"}), 404
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(debug=True, port=5001, host='0.0.0.0')