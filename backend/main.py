# main.py

from flask import Flask, jsonify, request
from flask_cors import CORS
from firebase_admin import initialize_app, credentials, db

# Import blueprints
from routes.status import status_bp
from routes.sensor import sensor_bp
from routes.command import command_bp

app = Flask(__name__)
CORS(app)

# Register blueprints
app.register_blueprint(status_bp, url_prefix='/api')
app.register_blueprint(sensor_bp, url_prefix='/api')
app.register_blueprint(command_bp, url_prefix='/api')

# Firebase yapılandırması
import os
firebase_config_path = os.path.join(os.path.dirname(__file__), "config/firebase_config.json")
firebase_cred = credentials.Certificate(firebase_config_path)
firebase_app = initialize_app(firebase_cred, {
    'databaseURL': 'https://smarthome-aa9f5-default-rtdb.europe-west1.firebasedatabase.app/'
})

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