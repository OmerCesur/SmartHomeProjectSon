from flask import Blueprint, request, jsonify
from firebase_admin import db
import logging
from datetime import datetime
import json

face_id_bp = Blueprint('face_id', __name__)
logger = logging.getLogger('smart_home')

@face_id_bp.route('/face-recognition', methods=['POST'])
def handle_face_recognition():
    """
    Face ID sunucusundan gelen tanıma sonuçlarını işler.
    
    Request Body:
    {
        "device_id": "camera_1",
        "recognized": true,
        "timestamp": "2024-05-23T01:46:30.870Z"
    }
    """
    try:
        # Gelen veriyi logla
        logger.info(f"Gelen istek: {request.get_data()}")
        
        # JSON parse etmeyi dene
        try:
            data = request.get_json()
            logger.info(f"Parse edilen JSON: {data}")
        except json.JSONDecodeError as e:
            logger.error(f"JSON parse hatası: {str(e)}")
            return jsonify({
                "error": "Geçersiz JSON formatı",
                "details": str(e)
            }), 400
        
        # Gerekli alanları kontrol et
        if not data:
            logger.error("Boş veri gönderildi")
            return jsonify({
                "error": "Geçersiz veri formatı",
                "details": "Veri boş olamaz"
            }), 400
            
        if "device_id" not in data:
            logger.error("device_id eksik")
            return jsonify({
                "error": "Geçersiz veri formatı",
                "details": "device_id alanı gerekli"
            }), 400
            
        if "recognized" not in data:
            logger.error("recognized eksik")
            return jsonify({
                "error": "Geçersiz veri formatı",
                "details": "recognized alanı gerekli"
            }), 400
            
        device_id = data["device_id"]
        recognized = data["recognized"]
        timestamp = data.get("timestamp", datetime.now().isoformat())
        
        # Firebase'e kaydet
        ref = db.reference(f"sensors/{device_id}/face_id")
        face_data = {
            "recognized": recognized,
            "timestamp": timestamp
        }
        
        # Sensör verisini güncelle
        ref.set(face_data)
        
        # Bildirimi veritabanına kaydet
        notification_ref = db.reference("notifications")
        notification_ref.push({
            "title": "Yüz Tanıma",
            "message": f"{device_id} cihazında yüz {'tanındı' if recognized else 'tanınmadı'}",
            "type": "face_recognition",
            "severity": "info",
            "timestamp": timestamp
        })
        
        logger.info(f"Face recognition verisi başarıyla kaydedildi: {face_data}")
        return jsonify({
            "message": "Face recognition verisi başarıyla kaydedildi",
            "data": face_data
        }), 200
        
    except Exception as e:
        logger.error(f"Face recognition verisi işlenirken hata: {str(e)}", exc_info=True)
        return jsonify({
            "error": "Veri işlenirken hata oluştu",
            "details": str(e)
        }), 500 