# main.py

from flask import Flask, jsonify, request
from flask_cors import CORS
from firebase_admin import initialize_app, credentials, db
import os
import logging
from logging.handlers import RotatingFileHandler
from datetime import datetime

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
    firebase_config_path = os.path.join(os.path.dirname(__file__), 'config', 'firebase_config.json')
    firebase_env = os.environ.get("FIREBASE_CONFIG")

    if not os.path.exists(firebase_config_path):
        if firebase_env:
            try:
                import json
                # Environment variable'dan JSON'ı parse et
                config_data = json.loads(firebase_env)
                
                # Private key'i düzgün formatta hazırla
                if 'private_key' in config_data:
                    # Private key'i satır satır işle
                    private_key_lines = config_data['private_key'].split('\\n')
                    # Boş satırları temizle
                    private_key_lines = [line for line in private_key_lines if line.strip()]
                    
                    # BEGIN ve END satırlarını kontrol et ve düzelt
                    begin_line = '-----BEGIN PRIVATE KEY-----'
                    end_line = '-----END PRIVATE KEY-----'
                    
                    # BEGIN satırını ekle
                    if not any(line.strip() == begin_line for line in private_key_lines):
                        private_key_lines.insert(0, begin_line)
                    
                    # END satırını ekle (sadece bir tane olmalı)
                    if not any(line.strip() == end_line for line in private_key_lines):
                        private_key_lines.append(end_line)
                    else:
                        # Fazladan END satırlarını temizle
                        private_key_lines = [line for line in private_key_lines if line.strip() != end_line]
                        private_key_lines.append(end_line)
                    
                    # Satırları birleştir
                    private_key = '\n'.join(private_key_lines)
                    config_data['private_key'] = private_key

                # Düzgün formatlanmış JSON olarak kaydet
                with open(firebase_config_path, "w") as f:
                    json.dump(config_data, f, indent=2)
                
                logger.info("Firebase config dosyası başarıyla oluşturuldu")
                logger.debug(f"Private key formatı: {config_data['private_key']}")
            except json.JSONDecodeError as e:
                logger.error(f"Firebase config JSON parse hatası: {str(e)}")
                raise
            except Exception as e:
                logger.error(f"Firebase config dosyası oluşturulurken hata: {str(e)}")
                raise
        else:
            raise FileNotFoundError(f"Firebase config dosyası bulunamadı: {firebase_config_path}")
    
    # Config dosyasını oku ve logla
    with open(firebase_config_path, 'r') as f:
        config_content = f.read()
        logger.debug(f"Firebase config içeriği: {config_content}")
    
    # Firebase credentials'ı yükle
    firebase_cred = credentials.Certificate(firebase_config_path)
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