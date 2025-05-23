from datetime import datetime
from firebase_admin import db
import logging

logger = logging.getLogger(__name__)

class SensorRepository:
    def __init__(self, database):
        self.db = database

    def get_sensor_data(self, room, sensor_type):
        """Belirli bir odadaki sensörün verisini getirir."""
        try:
            ref = self.db.child(f"sensors/{room}/{sensor_type}")
            return ref.get()
        except Exception as e:
            logger.error(f"Sensör verisi alınırken hata: {str(e)}")
            raise

    def update_sensor_data(self, room, sensor_type, value):
        """Belirli bir odadaki sensörün verisini günceller."""
        try:
            ref = self.db.child(f"sensors/{room}/{sensor_type}")
            data = {
                "value": value,
                "timestamp": datetime.now().isoformat()
            }
            ref.set(data)
            return data
        except Exception as e:
            logger.error(f"Sensör verisi güncellenirken hata: {str(e)}")
            raise

    def get_all_sensors(self):
        """Tüm sensörlerin verilerini getirir."""
        try:
            ref = self.db.child("sensors")
            return ref.get()
        except Exception as e:
            logger.error(f"Tüm sensör verileri alınırken hata: {str(e)}")
            raise

    def get_room_sensors(self, room):
        """Belirli bir odadaki tüm sensörlerin verilerini getirir."""
        try:
            ref = self.db.child(f"sensors/{room}")
            return ref.get()
        except Exception as e:
            logger.error(f"Oda sensör verileri alınırken hata: {str(e)}")
            raise 