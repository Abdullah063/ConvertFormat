# ConvertFormat

DOCX belgelerini arka planda PDF'e dönüştüren Spring Boot uygulaması. Proje; dosya yükleme, asenkron işleme, durum sorgulama ve gizli anahtarla indirme akışını gerçek bir LibreOffice işlemi üzerinden çalıştırır.

## Özellikler

- Mobil uyumlu DOCX yükleme arayüzü
- En fazla 10 MB `.docx` doğrulaması
- Asenkron dönüşüm ve iş durumu takibi
- LibreOffice ile gerçek PDF üretimi
- SHA-256 özeti veritabanında tutulan tek kullanımlık erişim anahtarı
- İstemci başına dakikada 10 yükleme sınırı
- PostgreSQL ve Flyway şema yönetimi
- Swagger/OpenAPI dokümantasyonu
- Docker Compose ile uygulama, PostgreSQL ve kalıcı dosya alanı
- Actuator sağlık kontrolü

## Çalışma akışı

1. Tarayıcı `POST /api/v1/conversions` isteğiyle DOCX dosyasını gönderir.
2. Uygulama dosyayı saklar, veritabanında `PENDING` işi oluşturur ve indirme anahtarını yalnızca bu yanıtta döndürür.
3. Transaction tamamlandıktan sonra asenkron çalışan işlemci LibreOffice'i başlatır.
4. İş durumu `PROCESSING`, ardından `COMPLETED` veya `FAILED` olur.
5. Tarayıcı durumu sorgular ve tamamlanan PDF'i `X-Download-Token` başlığıyla indirir.

## Teknolojiler

- Java 21
- Spring Boot 4.1
- Spring Web MVC, Spring Data JPA, Spring Security
- PostgreSQL 17
- Flyway
- LibreOffice
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

Yanıttaki `id` iş durumunu sorgulamak, `downloadToken` ise PDF'i indirmek için kullanılır. Ham anahtar daha sonra sunucudan tekrar alınamaz.

```bash
curl http://127.0.0.1:8081/api/v1/conversions/IS_ID

curl \
  -H "X-Download-Token: GIZLI_ANAHTAR" \
  -o belge.pdf \
  http://127.0.0.1:8081/api/v1/conversions/IS_ID/file
```

## Yerel geliştirme

Uygulamayı Docker dışında çalıştıracaksan bilgisayarında Java 21, PostgreSQL ve `libreoffice` komutu bulunmalıdır.

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
- Kamuya açık dosya dönüştürücülerde antivirüs taraması, disk kotası ve LibreOffice işlemlerinin daha sıkı izole edilmesi sonraki güvenlik katmanlarıdır.

## Mülakat için önemli kararlar

- **Constructor injection:** Bağımlılıkları zorunlu ve değiştirilemez hâle getirir; sınıfı testlerde kolay kurmayı sağlar.
- **DTO kullanımı:** Entity'yi doğrudan API'ye açmaz; dış sözleşmeyi veritabanı modelinden ayırır.
- **Transaction sonrası event:** Veritabanına yazma başarısızsa dönüşümün boşuna başlamasını engeller.
- **Asenkron işlem:** Uzun süren LibreOffice işlemi HTTP isteğini bekletmez; istemci durumu ayrı uçtan izler.
- **Flyway:** Şema değişikliklerini sıralı, tekrar üretilebilir ve kodla birlikte sürümlenebilir tutar.
- **Token hashleme:** Veritabanı sızsa bile ham indirme anahtarı doğrudan ele geçmez.
- **Portların loopback'e bağlanması:** Uygulama ve veritabanının internete doğrudan açılmasını engeller.
