import os
import json
import base64
import logging
from firebase_admin import initialize_app, credentials

logger = logging.getLogger(__name__)

def init_firebase():
    """Firebase'i başlatır ve yapılandırır."""
    try:
        firebase_config_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'config', 'firebase_config.json')
        firebase_env = os.environ.get("FIREBASE_CONFIG")

        if not os.path.exists(firebase_config_path):
            if firebase_env:
                try:
                    # Environment variable'dan JSON'ı parse et
                    config_data = json.loads(firebase_env)
                    
                    # Private key'i düzgün formatta hazırla
                    if 'private_key' in config_data:
                        config_data['private_key'] = format_private_key(config_data['private_key'])
                    
                    # Düzgün formatlanmış JSON olarak kaydet
                    with open(firebase_config_path, "w") as f:
                        json.dump(config_data, f, indent=2)
                    
                    logger.info("Firebase config dosyası başarıyla oluşturuldu")
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
        
        return firebase_app
        
    except Exception as e:
        logger.error(f"Firebase başlatılırken hata oluştu: {str(e)}", exc_info=True)
        raise

def format_private_key(private_key):
    """Private key'i düzgün formatta hazırlar."""
    # Private key'i satır satır işle
    private_key_lines = private_key.split('\\n')
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
    
    # Base64 kodlamasını düzelt
    try:
        # Private key'in içeriğini al (BEGIN ve END satırları hariç)
        key_content = '\n'.join(line for line in private_key_lines[1:-1])
        # Base64 decode et
        decoded_key = base64.b64decode(key_content)
        # Tekrar encode et
        encoded_key = base64.b64encode(decoded_key).decode('utf-8')
        # Satırları 64 karakterlik parçalara böl
        wrapped_key = '\n'.join(encoded_key[i:i+64] for i in range(0, len(encoded_key), 64))
        # BEGIN ve END satırlarını ekle
        return f"{begin_line}\n{wrapped_key}\n{end_line}"
    except Exception as e:
        logger.warning(f"Base64 düzeltme başarısız oldu, orijinal private key kullanılıyor: {str(e)}")
        return private_key 