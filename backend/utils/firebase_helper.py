import firebase_admin
from firebase_admin import credentials, db
from datetime import datetime
import os

# 🔐 Firebase yapılandırması
def initialize_firebase():
    """
    Firebase'i yapılandırır ve başlatır.
    """
    try:
        # Firebase servis hesabı JSON dosyasını kullanarak yetkilendirme
        cred = credentials.Certificate("backend/firebase_config.json")
        firebase_admin.initialize_app(cred, {
            "databaseURL": "https://smarthome-aa9f5-default-rtdb.europe-west1.firebasedatabase.app"
        })
        print("Firebase başarıyla başlatıldı.")
    except Exception as e:
        print(f"Firebase başlatılırken hata oluştu: {str(e)}")
        raise e

def write_sensor_data(room, temperature, gas_level, severity):
    """
    Sensör verilerini Firebase'e yazar.
    
    Args:
        room (str): Oda adı (örn: "salon")
        temperature (float): Sıcaklık değeri (40-125 arası)
        gas_level (int): Gaz seviyesi
        severity (str): Gaz seviyesi kategorisi (LOW, MEDIUM, HIGH)
    """
    # 🕒 Şu anki zaman
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    # Firebase'deki oda referansına yaz
    ref = db.reference(f"sensors/{room}")
    ref.set({
        "temperature": temperature,
        "gas_level": gas_level,
        "severity": severity,
        "timestamp": timestamp
    })

    # Gaz seviyesi kritik ise bildirim verisini de kaydet
    if severity == "HIGH":
        notification_ref = db.reference("notifications")
        notification_ref.push({
            "type": "gas_alert",
            "message": f"Gaz seviyesi kritik seviyede: {gas_level}",
            "severity": severity,
            "gas_level": gas_level,
            "timestamp": timestamp
        })