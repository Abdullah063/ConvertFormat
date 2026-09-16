# ConvertFormat

DOCX belgelerini ve görselleri farklı formatlara dönüştüren Spring Boot uygulaması. Proje; dosya yükleme, asenkron işleme, durum sorgulama ve gizli anahtarla indirme akışını gerçek LibreOffice, cwebp ve ImageMagick işlemleri üzerinden çalıştırır.

## Özellikler

- Mobil uyumlu belge ve görsel yükleme arayüzü
- DOCX → PDF
- PPTX → PDF
- XLSX → PDF
- JPEG/PNG → WebP veya PDF
- WebP → JPEG veya PNG
- PNG → JPEG
- JPEG → PNG
- WebP ve JPEG çıktıları için 1–100 arası kalite seçimi
- Dosya uzantısı yanında Office, JPEG, PNG ve WebP imza doğrulaması
- En fazla 10 MB dosya doğrulaması
- Asenkron dönüşüm ve iş durumu takibi
- LibreOffice, cwebp ve ImageMagick ile gerçek format dönüşümü
- SHA-256 özeti veritabanında tutulan tek kullanımlık erişim anahtarı
- İstemci başına dakikada 10 yükleme sınırı
- PostgreSQL ve Flyway şema yönetimi
- Swagger/OpenAPI dokümantasyonu
- Docker Compose ile uygulama, PostgreSQL ve kalıcı dosya alanı
- Actuator sağlık kontrolü

## Çalışma akışı

1. Tarayıcı `POST /api/v1/conversions` isteğiyle dosyayı ve hedef dönüşüm türünü gönderir.
2. Uygulama dosyayı saklar, veritabanında `PENDING` işi oluşturur ve indirme anahtarını yalnızca bu yanıtta döndürür.
3. Transaction tamamlandıktan sonra asenkron çalışan işlemci dönüşüm tipine uygun LibreOffice, cwebp veya ImageMagick motorunu seçer.
4. İş durumu `PROCESSING`, ardından `COMPLETED` veya `FAILED` olur.
5. Tarayıcı durumu sorgular ve tamamlanan dosyayı `X-Download-Token` başlığıyla indirir.

## Teknolojiler

- Java 21
- Spring Boot 4.1
- Spring Web MVC, Spring Data JPA, Spring Security
- PostgreSQL 17
- Flyway
- LibreOffice, libwebp/cwebp ve ImageMagick
- Springdoc OpenAPI
- Docker ve Docker Compose

## Docker ile çalıştırma

Gerekenler: Docker ve Docker Compose.

```bash
cp .env.example .env
```

`.env` içindeki veritabanı parolasını değiştir, ardından:

```bash
docker compose up -d --build
docker compose ps
```

Adresler:

- Uygulama: http://127.0.0.1:8081
- Swagger UI: http://127.0.0.1:8081/swagger-ui.html
- Sağlık kontrolü: http://127.0.0.1:8081/actuator/health
- PostgreSQL: `127.0.0.1:5434`

Logları izlemek ve sistemi durdurmak için:

```bash
docker compose logs -f app
docker compose down
```

`docker compose down` verileri silmez. `-v` seçeneği PostgreSQL ve dosya volume'larını sileceği için dikkatli kullanılmalıdır.

## API örnekleri

Yeni dönüşüm başlatma:

```bash
curl -F file=@belge.docx http://127.0.0.1:8081/api/v1/conversions
```

Görseli kalite değeriyle WebP'ye dönüştürme:

```bash
curl \
  -F file=@gorsel.png \
  -F conversionType=IMAGE_TO_WEBP \
  -F quality=82 \
  http://127.0.0.1:8081/api/v1/conversions
```

Diğer görsel dönüşümleri:

```bash
# WebP → JPEG
curl -F file=@gorsel.webp -F conversionType=WEBP_TO_JPEG -F quality=90 \
  http://127.0.0.1:8081/api/v1/conversions

# WebP → PNG
curl -F file=@gorsel.webp -F conversionType=WEBP_TO_PNG \
  http://127.0.0.1:8081/api/v1/conversions

# JPEG veya PNG → PDF
curl -F file=@gorsel.jpg -F conversionType=IMAGE_TO_PDF \
  http://127.0.0.1:8081/api/v1/conversions
```

Desteklenen `conversionType` değerleri: `DOCX_TO_PDF`, `PPTX_TO_PDF`, `XLSX_TO_PDF`, `IMAGE_TO_WEBP`, `WEBP_TO_JPEG`, `WEBP_TO_PNG`, `PNG_TO_JPEG`, `JPEG_TO_PNG` ve `IMAGE_TO_PDF`.

`quality` değeri `1–100` arasındadır. `IMAGE_TO_WEBP` için varsayılan `82`; `WEBP_TO_JPEG` ve `PNG_TO_JPEG` için varsayılan `90` kullanılır. Arayüz dönüşüm türünü her zaman açıkça gönderir. Geriye dönük uyumluluk amacıyla tür gönderilmezse DOCX dosyası PDF'e, JPEG/PNG dosyası WebP'ye dönüştürülür; diğer dosyalarda hedef tür zorunludur.

Yanıttaki `id` iş durumunu sorgulamak, `downloadToken` ise çıktıyı indirmek için kullanılır. Ham anahtar daha sonra sunucudan tekrar alınamaz.

```bash
curl http://127.0.0.1:8081/api/v1/conversions/IS_ID

curl \
  -H "X-Download-Token: GIZLI_ANAHTAR" \
  -o belge.pdf \
  http://127.0.0.1:8081/api/v1/conversions/IS_ID/file
```

## Yerel geliştirme

Uygulamayı Docker dışında çalıştıracaksan bilgisayarında Java 21, PostgreSQL, `libreoffice`, `cwebp` ve WebP/PDF desteği bulunan `magick` komutları bulunmalıdır.

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

Testler:

```bash
./mvnw test
```

Veritabanı tabloları Hibernate tarafından otomatik değiştirilmez. Şema `src/main/resources/db/migration` altındaki Flyway migration'larıyla sürümlenir; Hibernate yalnızca şemanın entity'lerle uyumlu olduğunu doğrular.

## Üretim notları

- Compose portları yalnızca `127.0.0.1` üzerinde açıktır. Dış trafik Nginx veya başka bir reverse proxy üzerinden uygulamanın `8081` portuna iletilmelidir.
- `server.forward-headers-strategy=framework`, reverse proxy arkasında istemci adresinin doğru okunmasını sağlar.
- Yükleme limiti şu anda uygulama belleğindedir. Tek sunucuda uygundur; birden fazla uygulama kopyasında ortak limit için Redis gibi merkezi bir sistem kullanılmalıdır.
- Yüklenen ve üretilen dosyalar Docker volume'unda kalıcıdır. Otomatik silme bilinçli olarak etkin değildir; üretimde saklama süresi belirlendikten sonra zamanlanmış temizlik eklenmelidir.
- Kamuya açık dosya dönüştürücülerde antivirüs taraması, disk kotası ve dönüşüm işlemlerinin daha sıkı izole edilmesi sonraki güvenlik katmanlarıdır.

## Mülakat için önemli kararlar

- **Constructor injection:** Bağımlılıkları zorunlu ve değiştirilemez hâle getirir; sınıfı testlerde kolay kurmayı sağlar.
- **DTO kullanımı:** Entity'yi doğrudan API'ye açmaz; dış sözleşmeyi veritabanı modelinden ayırır.
- **Transaction sonrası event:** Veritabanına yazma başarısızsa dönüşümün boşuna başlamasını engeller.
- **Asenkron işlem:** Dönüşüm motorunun çalışması HTTP isteğini bekletmez; istemci durumu ayrı uçtan izler.
- **Strategy yaklaşımı:** İşlemci dönüşüm tipine göre uygun motoru seçer; yeni formatlar ana akışı büyütmeden eklenebilir.
- **Flyway:** Şema değişikliklerini sıralı, tekrar üretilebilir ve kodla birlikte sürümlenebilir tutar.
- **Token hashleme:** Veritabanı sızsa bile ham indirme anahtarı doğrudan ele geçmez.
- **Portların loopback'e bağlanması:** Uygulama ve veritabanının internete doğrudan açılmasını engeller.
