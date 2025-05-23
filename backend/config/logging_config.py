import os
import logging
from logging.handlers import RotatingFileHandler
from datetime import datetime

def setup_logging(app_name):
    """Logging yapılandırmasını ayarlar."""
    # Log dizinini oluştur
    log_dir = os.path.join(os.path.dirname(os.path.dirname(__file__)), "logs")
    if not os.path.exists(log_dir):
        os.makedirs(log_dir)

    # Log dosyası yolu
    log_file = os.path.join(log_dir, f"{app_name}_{datetime.now().strftime('%Y%m%d')}.log")
    
    # Dosya handler'ı
    file_handler = RotatingFileHandler(log_file, maxBytes=10240000, backupCount=10)
    file_handler.setFormatter(logging.Formatter(
        '%(asctime)s %(levelname)s: %(message)s [in %(pathname)s:%(lineno)d]'
    ))
    file_handler.setLevel(logging.INFO)

    # Konsol handler'ı
    console_handler = logging.StreamHandler()
    console_handler.setFormatter(logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s'))
    console_handler.setLevel(logging.DEBUG)

    # Logger'ı yapılandır
    logger = logging.getLogger(app_name)
    logger.setLevel(logging.DEBUG)
    logger.addHandler(file_handler)
    logger.addHandler(console_handler)

    return logger 