from datetime import datetime
from firebase_admin import db
import logging

logger = logging.getLogger(__name__)

class NotificationRepository:
    def __init__(self, database):
        self.db = database

    def save_notification(self, title, message, notification_type, severity=None, sensor_value=None):
        """Yeni bir bildirim kaydeder."""
        try:
            ref = self.db.child("notifications")
            notification_data = {
                "title": title,
                "message": message,
                "type": notification_type,
                "severity": severity,
                "sensor_value": sensor_value,
                "timestamp": datetime.now().isoformat(),
                "read": False
            }
            return ref.push(notification_data)
        except Exception as e:
            logger.error(f"Bildirim kaydedilirken hata: {str(e)}")
            raise

    def get_all_notifications(self):
        """Tüm bildirimleri getirir."""
        try:
            ref = self.db.child("notifications")
            return ref.get()
        except Exception as e:
            logger.error(f"Bildirimler alınırken hata: {str(e)}")
            raise

    def delete_notification(self, notification_id):
        """Belirli bir bildirimi siler."""
        try:
            ref = self.db.child(f"notifications/{notification_id}")
            ref.delete()
            return True
        except Exception as e:
            logger.error(f"Bildirim silinirken hata: {str(e)}")
            raise

    def mark_as_read(self, notification_id):
        """Belirli bir bildirimi okundu olarak işaretler."""
        try:
            ref = self.db.child(f"notifications/{notification_id}")
            ref.update({"read": True})
            return True
        except Exception as e:
            logger.error(f"Bildirim okundu olarak işaretlenirken hata: {str(e)}")
            raise 