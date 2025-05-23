from datetime import datetime
from firebase_admin import db
import logging

logger = logging.getLogger(__name__)

class CommandRepository:
    def __init__(self, database):
        self.db = database

    def get_command_status(self, room, command_type):
        """Belirli bir odadaki komutun durumunu getirir."""
        try:
            ref = self.db.reference(f"commands/{room}/{command_type}")
            command_status = ref.get()
            
            if not command_status:
                command_status = {
                    "value": "off",
                    "timestamp": None
                }
            
            return command_status
        except Exception as e:
            logger.error(f"Komut durumu alınırken hata: {str(e)}")
            raise

    def send_command(self, room, command_type, command):
        """Belirli bir odadaki komutu gönderir."""
        try:
            timestamp = datetime.now().isoformat()
            command_data = {
                "value": command,
                "timestamp": timestamp
            }

            # Komut geçmişini kaydet
            history_ref = self.db.reference(f"command_history/{room}/{command_type}")
            history_ref.push(command_data)

            # Ana komut verisini güncelle
            ref = self.db.reference(f"commands/{room}/{command_type}")
            ref.set(command_data)

            return command_data
        except Exception as e:
            logger.error(f"Komut gönderilirken hata: {str(e)}")
            raise

    def get_command_history(self, room, command_type, limit=10):
        """Belirli bir odadaki komutun geçmişini getirir."""
        try:
            ref = self.db.reference(f"command_history/{room}/{command_type}")
            history = ref.order_by_child("timestamp").limit_to_last(limit).get()
            return history
        except Exception as e:
            logger.error(f"Komut geçmişi alınırken hata: {str(e)}")
            raise 