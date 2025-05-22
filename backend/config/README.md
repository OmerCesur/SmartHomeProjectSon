# Firebase Yapılandırması

Bu klasör Firebase yapılandırma dosyalarını içerir.

## Kurulum Adımları

1. [Firebase Console](https://console.firebase.google.com/)'a gidin
2. Projenizi seçin
3. Sol menüden "Project Settings" (Proje Ayarları) seçeneğine tıklayın
4. "Service accounts" (Servis Hesapları) sekmesine gidin
5. "Generate New Private Key" (Yeni Özel Anahtar Oluştur) butonuna tıklayın
6. İndirilen JSON dosyasını `firebase_config.json` olarak bu klasöre kaydedin

## Önemli Notlar

- `firebase_config.json` dosyası hassas bilgiler içerir ve git'e eklenmemelidir
- Yeni bir geliştirici projeye katıldığında, kendi Firebase config dosyasını oluşturmalıdır
- Örnek yapılandırma için `firebase_config.example.json` dosyasını inceleyebilirsiniz

## Güvenlik

- Firebase config dosyasını asla git'e commit etmeyin
- Config dosyasını güvenli bir şekilde saklayın
- Eğer config dosyası yanlışlıkla git'e eklendiyse, hemen değiştirin ve yeni bir config oluşturun 