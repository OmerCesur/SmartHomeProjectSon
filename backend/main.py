# main.py

from flask import Flask, jsonify, request
from flask_cors import CORS
from firebase_admin import initialize_app, credentials, db
import os
import logging
from logging.handlers import RotatingFileHandler
from datetime import datetime
from dotenv import load_dotenv
import json

# .env dosyasını yükle
load_dotenv()

# Import blueprints
from routes.status import status_bp
from routes.sensor import sensor_bp
from routes.command import command_bp

app = Flask(__name__)
CORS(app)

# Logging yapılandırması
log_dir = os.path.join(os.path.dirname(__file__), "logs")
if not os.path.exists(log_dir):
    os.makedirs(log_dir)

log_file = os.path.join(log_dir, f"app_{datetime.now().strftime('%Y%m%d')}.log")
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

# Register blueprints
app.register_blueprint(status_bp, url_prefix='/api')
app.register_blueprint(sensor_bp, url_prefix='/api')
app.register_blueprint(command_bp, url_prefix='/api')

# Firebase yapılandırması
try:
    firebase_config = {
        "type": "service_account",
        "project_id": os.environ.get('FIREBASE_PROJECT_ID', 'smarthome-aa9f5'),
        "private_key_id": os.environ.get('FIREBASE_PRIVATE_KEY_ID'),
        "private_key": os.environ.get('FIREBASE_PRIVATE_KEY', '').replace('\\n', '\n').strip('"'),
        "client_email": os.environ.get('FIREBASE_CLIENT_EMAIL'),
        "client_id": os.environ.get('FIREBASE_CLIENT_ID'),
        "auth_uri": "https://accounts.google.com/o/oauth2/auth",
        "token_uri": "https://oauth2.googleapis.com/token",
        "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
        "client_x509_cert_url": os.environ.get('FIREBASE_CLIENT_X509_CERT_URL'),
        "universe_domain": "googleapis.com"
    }

    # Debug için private key'i logla
    private_key = os.environ.get('FIREBASE_PRIVATE_KEY', '')
    logger.debug("Original private key from env: %s", private_key)
    logger.debug("Private key after replacement: %s", private_key.replace('\\n', '\n').strip('"'))
    
    config_path = os.path.join(os.path.dirname(__file__), 'firebase_config_env.json')
    with open(config_path, 'w') as f:
        json.dump(firebase_config, f, indent=2)
    
    # Debug için oluşturulan JSON'u logla
    logger.debug("Generated JSON file content: %s", json.dumps(firebase_config, indent=2))

    # Firebase credentials'ı yükle
    firebase_cred = credentials.Certificate(config_path)
    logger.info("Firebase credentials başarıyla yüklendi")
    
    firebase_app = initialize_app(firebase_cred, {
        'databaseURL': 'https://smarthome-aa9f5-default-rtdb.europe-west1.firebasedatabase.app/'
    })
    logger.info("Firebase başarıyla başlatıldı")
    
    # Test bağlantısı
    try:
        ref = db.reference('/')
        test_data = ref.get()
        logger.info("Firebase bağlantı testi başarılı")
    except Exception as e:
        logger.error(f"Firebase bağlantı testi başarısız: {str(e)}")
        raise
except Exception as e:
    logger.error(f"Firebase başlatılırken hata oluştu: {str(e)}", exc_info=True)
    raise

@app.route('/sensors/bulk-update', methods=['POST'])
def bulk_update_sensors():
    # Log if request is from another PC
    remote_addr = request.remote_addr
    if remote_addr not in ('127.0.0.1', 'localhost', '::1'):
        print(f"Received /sensors/bulk-update POST from remote address: {remote_addr}")
    try:
        data = request.get_json()
        if not data or 'updates' not in data:
            return jsonify({
                "error": "Geçersiz istek formatı.",
                "details": "Request body'de 'updates' array'i bulunmalıdır."
            }), 400

        results = []
        errors = []

        for update in data['updates']:
            room = update.get('room')
            sensor_type = update.get('sensor_type')
            value = update.get('value')

            if not all([room, sensor_type, value is not None]):
                errors.append({
                    "error": "Eksik parametre",
                    "details": f"Her güncelleme için room, sensor_type ve value gereklidir.",
                    "update": update
                })
                continue

            try:
                # Firebase'e yazma
                ref = db.reference(f"sensors/{room}/{sensor_type}")
                timestamp = "2024-03-21 12:00:00"  # Gerçek uygulamada datetime.now() kullanılmalı
                ref.set({
                    "value": value,
                    "timestamp": timestamp
                })

                results.append({
                    "room": room,
                    "sensor_type": sensor_type,
                    "status": "success",
                    "value": value,
                    "timestamp": timestamp
                })

            except Exception as e:
                errors.append({
                    "error": "Firebase güncelleme hatası",
                    "details": str(e),
                    "update": update
                })

        return jsonify({
            "message": "Toplu güncelleme tamamlandı",
            "successful_updates": results,
            "errors": errors
        }), 200 if not errors else 207

    except Exception as e:
        return jsonify({
            "error": "Toplu güncelleme sırasında hata oluştu",
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