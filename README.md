# MyCar

MyCar este o aplicație Android pentru administrarea mașinilor personale. Utilizatorul își poate crea cont, poate adăuga mașini, poate urmări datele de expirare pentru ITP, RCA și rovinietă și poate salva istoricul intervențiilor de service.

Proiectul este realizat în Kotlin, cu Jetpack Compose pentru interfață și Firebase pentru autentificare și datele principale.

## Funcționalități

- Autentificare și înregistrare cu Firebase Authentication.
- Dashboard cu acces rapid către funcțiile principale.
- Adăugare, editare și ștergere mașini.
- Salvare date mașini în Firebase Firestore.
- Listare mașini în UI Compose folosind `LazyColumn`.
- Istoric service pentru fiecare mașină.
- Notificări periodice pentru documente care urmează să expire, folosind WorkManager.
- Salvarea ultimului email folosit la login în SharedPreferences.
- Note locale salvate în SQLite, disponibile doar pe dispozitiv.
- Catalog modele auto prin request-uri HTTP către API-ul public NHTSA vPIC.
- Resurse XML drawable pentru shape, selector, color selector și gradient.

## Tehnologii folosite

- Kotlin
- Jetpack Compose
- Firebase Authentication
- Firebase Firestore
- SQLite prin `SQLiteOpenHelper`
- SharedPreferences
- WorkManager
- HTTP requests cu `HttpURLConnection`
- JSON parsing cu `JSONObject`
- XML drawables

## Ecrane principale

- Login
- Register
- Dashboard
- Adaugă / editează mașină
- Lista mașinilor
- Istoric service
- Note locale SQLite
- Catalog modele auto

## API extern

Pentru catalogul auto este folosit API-ul public NHTSA vPIC:

https://vpic.nhtsa.dot.gov/api/

Aplicația face request-uri HTTP pentru obținerea tipurilor de vehicule și a modelelor disponibile pentru o anumită marcă auto:

- `GetVehicleTypesForMake/{make}?format=json`
- `GetModelsForMake/{make}?format=json`

API-ul este gratuit, public și nu necesită cont sau cheie API.

## Cum se rulează

1. Clonează repository-ul.
2. Deschide proiectul în Android Studio.
3. Verifică existența fișierului `app/google-services.json` pentru Firebase.
4. Rulează aplicația pe emulator sau pe telefon Android.

Comandă build:

```bash
./gradlew :app:assembleDebug
```

Pe Windows:

```bash
.\gradlew.bat :app:assembleDebug
```

## Observații

Firebase este folosit pentru datele principale sincronizate în cloud, iar SQLite este folosit separat pentru note rapide locale. SharedPreferences salvează o preferință simplă a utilizatorului, iar catalogul auto arată integrarea unui API extern real.
