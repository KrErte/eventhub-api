# EventHub API

EventHub on sündmuste haldamise rakendus: sündmuste nimekiri, hiljem ka sündmuste loomine ja kohtade broneerimine.
See repo sisaldab **backendi** (REST API). Kasutajaliides on eraldi repos:
[Eventhub-frontend](https://github.com/KrErte/Eventhub-frontend).

**Praegune seis:** API oskab sündmusi lugeda (`GET /api/events`, `GET /api/events/{id}`).
Loomine, broneerimine ja autentimine on veel tegemata.

## Sisukord

- [Kuidas osad kokku käivad](#kuidas-osad-kokku-käivad)
- [Tehnoloogiad](#tehnoloogiad)
- [Käivitamine](#käivitamine)
- [Arhitektuur](#arhitektuur)
- [Domeen](#domeen)
- [API](#api)
- [Vigade käsitlus](#vigade-käsitlus)
- [Andmebaas](#andmebaas)
- [Turvalisus](#turvalisus)
- [Testid](#testid)
- [Konventsioonid](#konventsioonid)

## Kuidas osad kokku käivad

```
 Brauser
   │  http://localhost:4200
   ▼
 Angular dev server (frontend, `npm start`)
   │  /api/**  → proxy (proxy.conf.json)
   ▼
 Spring Boot (see repo, `./gradlew bootRun`)    http://localhost:8080
   │  JPA / Hibernate
   ▼
 PostgreSQL 18 (Docker, compose.yaml)           juhuslik hostiport
```

Frontend kutsub API-t alati suhtelise URL-iga (`/api/events`). Arenduses suunab Angulari dev server
need päringud proxy kaudu pordile 8080, seega CORS-i seadistust pole vaja.

## Tehnoloogiad

| | |
|---|---|
| Keel | Java 25 |
| Raamistik | Spring Boot 4.2 (Spring MVC, Spring Data JPA, Spring Security, Bean Validation) |
| Andmebaas | PostgreSQL 18, skeem Flyway migratsioonidega |
| Ehitus | Gradle 9 (wrapper kaasas: `./gradlew`) |
| Lokaalne DB | Spring Boot Docker Compose tugi (`compose.yaml`) |
| Testid | JUnit 5, AssertJ, `MockMvcTester`, Testcontainers |
| Null-ohutus | JSpecify (`@NullMarked`, `@Nullable`) |

## Käivitamine

**Eeldused:** JDK 25 (või Gradle toolchain leiab/laeb selle ise) ja töötav **Docker Desktop**.

```bash
./gradlew bootRun
```

- Spring Boot käivitab `compose.yaml` põhjal Postgresi konteineri automaatselt ja seob selle juhuslikule
  hostipordile. `docker compose up` pole vaja käsitsi teha.
- `bootRun` aktiveerib profiili **`dev`**, mis laadib näidisandmed (vt [Andmebaas](#andmebaas)).
- API on aadressil `http://localhost:8080`, näiteks `curl http://localhost:8080/api/events`.

Kui käivitus ütleb `Port 8080 was already in use`, töötab mõni teine backend juba (nt IDE-st). Peata see
või käivita teises pordis: `./gradlew bootRun --args='--server.port=8081'` (siis ei jõua frontendi proxy
selleni).

## Arhitektuur

### Paketid feature'i järgi

Kood on jagatud **funktsionaalsuse** (feature) järgi, mitte tehniliste kihtide järgi
(`controller/`, `service/`, `repository/` kaustu ei ole):

```
com.example.eventhub
├── EventhubApplication.java
├── common/                     ← ühine infrastruktuur
│   ├── GlobalExceptionHandler  ← kõik vead → ProblemDetail
│   ├── NotFoundException
│   └── SecurityConfig
└── event/                      ← sündmuste feature
    ├── Event                   ← JPA entity        (package-private)
    ├── EventRepository         ← Spring Data repo  (package-private)
    ├── EventService            ← feature'i avalik sisenemispunkt
    ├── EventResponse           ← DTO (record), mida API tagastab
    └── EventController         ← REST endpointid
```

Tulevikus lisandub samamoodi `booking/`.

**Miks nii?** Kõik ühe funktsionaalsusega seotud failid on koos ja Java nähtavusreeglid hoiavad
feature'ite piire: `Event` ja `EventRepository` on package-private, nii et teised paketid
(nt tulevane `booking/`) **ei saa** neid otse kasutada, vaid peavad minema läbi `EventService`'i.

### Kihid feature'i sees

```
HTTP päring
   ↓
EventController   HTTP: URL-id, staatuskoodid, sisendi valideerimine
   ↓
EventService      äriloogika, @Transactional piirid, entity → DTO
   ↓
EventRepository   andmebaasi päringud (Spring Data JPA)
   ↓
PostgreSQL
```

Kutsed liiguvad ainult allapoole. Controller ei näe kunagi entity't.

### Põhireeglid

- **Entity't ei tagastata API-st kunagi**, ainult DTO-sid (`record`). Nii ei leki andmebaasi struktuur
  API lepingusse ja lazy-loading vead on välistatud.
- `spring.jpa.open-in-view=false`: andmebaasisessioon ei ela HTTP-vastuse renderdamiseni. Kõik vajalik
  laetakse ja teisendatakse DTO-ks service'i transaktsiooni sees.
- `spring.jpa.hibernate.ddl-auto=validate`: Hibernate **ei muuda** skeemi, vaid kontrollib käivitusel,
  et entity'd vastavad tabelitele. Skeemi muudab ainult Flyway.
- **Ainult constructor injection.** Väljadele `@Autowired`'i ei panda (v.a testides). Lombokit ei kasutata.
- **Null-ohutus:** iga paketi `package-info.java` sisaldab `@NullMarked`'i, mis tähendab, et kõik on
  vaikimisi mitte-null. Väärtused, mis võivad olla `null`, märgitakse `@Nullable`'iga
  (nt `Event.description`).

## Domeen

### Event (sündmus)

| väli | tüüp | reeglid |
|---|---|---|
| `id` | UUID | genereeritakse salvestamisel |
| `title` | tekst | kohustuslik, kuni 200 märki |
| `description` | tekst | valikuline (`null`) |
| `startsAt` | ajahetk (`Instant`) | kohustuslik, salvestatakse `timestamptz`'na (UTC) |
| `venue` | tekst | kohustuslik, kuni 200 märki |
| `capacity` | täisarv | kohustuslik, `> 0` (andmebaasi CHECK) |

Sündmused tagastatakse alati **alguse aja järgi kasvavas järjekorras**.

## API

Kõik endpointid on prefiksiga `/api`. Ajad on ISO-8601 formaadis UTC-s (`2027-01-30T09:00:00Z`).

### `GET /api/events`

Kõik sündmused, sorteeritud `startsAt` järgi.

```http
GET /api/events
```

```json
[
  {
    "id": "0193f6a0-0000-7000-8000-000000000003",
    "title": "Tartu talvine jooks",
    "description": null,
    "startsAt": "2027-01-30T09:00:00Z",
    "venue": "Raekoja plats, Tartu",
    "capacity": 1500
  }
]
```

| staatus | millal |
|---|---|
| `200 OK` | alati (tühi andmebaas → `[]`) |

### `GET /api/events/{id}`

Üks sündmus. Vastus on sama kujuga objekt nagu nimekirja element.

| staatus | millal |
|---|---|
| `200 OK` | sündmus leitud |
| `400 Bad Request` | `id` ei ole korrektne UUID |
| `404 Not Found` | sellise id-ga sündmust pole |

## Vigade käsitlus

Kõik vead tagastatakse ühtses **RFC 9457 Problem Details** formaadis
(`Content-Type: application/problem+json`). Selle eest vastutab üks klass:
`common/GlobalExceptionHandler` (`@RestControllerAdvice`, laiendab Springi `ResponseEntityExceptionHandler`'it).

| olukord | staatus | näide |
|---|---|---|
| ressurssi pole (`NotFoundException`) | 404 | `{"title":"Not Found","status":404,"detail":"Event 0000… not found"}` |
| vigane parameeter (nt mitte-UUID) | 400 | `{"title":"Bad Request","status":400,"detail":"Failed to convert 'id' with value: 'abc'"}` |
| päringu keha valideerimine (`@Valid`) | 400 | `{"title":"Bad Request","status":400,"detail":"Request validation failed","errors":{"name":"must not be blank"}}` |
| ootamatu viga | 500 | `{"title":"Internal Server Error","status":500,"detail":"Unexpected error"}` |

- Valideerimisvigade `errors` väli on objekt `väli → veateade`, et frontend saaks vead õige
  vormivälja juurde näidata.
- 500 puhul ei lähe erandi sisu kliendile, see logitakse ainult serveris.
- Spring Security erandeid advice ei neela: need visatakse edasi, et Security saaks vastata
  korrektse 401/403-ga.

Uue vea lisamiseks loo erand (nagu `NotFoundException`) ja lisa sellele `@ExceptionHandler` meetod
`GlobalExceptionHandler`'isse. Eraldi `@RestControllerAdvice` klasse ei tehta.

## Andmebaas

### Migratsioonid

Skeem on täielikult Flyway käes: `src/main/resources/db/migration/`.

| fail | sisu |
|---|---|
| `V1__create_event.sql` | tabel `event` + indeks `starts_at` peal |

**Reegel:** olemasolevaid migratsioone ei muudeta kunagi. Iga muudatus on uus fail
(`V2__...sql`, `V3__...sql`). Muidu läheb Flyway kontrollsumma olemasolevates andmebaasides katki.

### Profiilid ja näidisandmed

| profiil | kus | Flyway kaustad |
|---|---|---|
| (vaikimisi) | testid, tootmine | `db/migration` |
| `dev` | `./gradlew bootRun` | `db/migration` + `db/dev` |

`db/dev/R__dev_seed_events.sql` on *repeatable* migratsioon, mis lisab 3 näidissündmust.
See käivitub uuesti ainult siis, kui faili sisu muutub, ja `ON CONFLICT DO NOTHING` hoiab ära
duplikaadid. Testidesse ja tootmisse näidisandmed ei jõua.

Postgresi andmed säilivad konteineris ka pärast rakenduse peatamist. Täiesti puhta andmebaasi saamiseks
käivita `docker compose down -v` (kustutab andmed).

## Turvalisus

Projektis on `spring-boot-starter-security-oauth2-resource-server` (JWT), aga **autentimine pole veel
sisse lülitatud**. Ajutine `common/SecurityConfig`:

- `/api/**`, `/error`, `/actuator/health` on avatud,
- kõik muu on keelatud (`denyAll`),
- CSRF on välja lülitatud ja sessioone ei looda (stateless REST API).

**Plaan:** lisada OAuth2 JWT valideerimine (`spring.security.oauth2.resourceserver.jwt.issuer-uri`) ja
kaitsta muutvad endpointid (loomine, broneerimine).

## Testid

```bash
./gradlew test
```

Testid vajavad töötavat Dockerit: integratsioonitestid käivitavad **päris PostgreSQL 18** konteineri
(Testcontainers + `@ServiceConnection`). H2 või muid mälupõhiseid andmebaase ei kasutata, et testid
käituksid täpselt nagu päris andmebaas (samad migratsioonid, samad piirangud).

| test | tüüp | mida kontrollib |
|---|---|---|
| `EventhubApplicationTests` | Spring kontekst + Postgres | rakendus käivitub, migratsioonid ja entity'd klapivad |
| `event/EventControllerTest` | `@SpringBootTest` + `MockMvcTester` + Postgres | nimekiri ja sorteering, üks sündmus, 404, 400, avatud ligipääs |
| `common/GlobalExceptionHandlerTest` | standalone `MockMvcTester` (ilma Springi kontekstita) | 404, valideerimise 400 koos `errors`'iga, 500 ei leki detaile |

Testides kasutatakse AssertJ-d ja `MockMvcTester`'it.
`TestEventhubApplication` käivitab rakenduse lokaalselt Testcontainersi andmebaasiga (alternatiiv
`bootRun`'ile).

## Konventsioonid

Täielikud reeglid on failis [`AGENTS.md`](AGENTS.md). Lühidalt:

- paketid feature'i järgi, DTO-d on `record`'id, Lombokit ei kasutata,
- ainult constructor injection,
- entity't API-st ei tagastata,
- vead ainult `GlobalExceptionHandler`'i kaudu `ProblemDetail`'ina,
- igas paketis `package-info.java` koos `@NullMarked`'iga,
- skeemi muudatused ainult uue Flyway migratsiooniga,
- commitid [Conventional Commits](https://www.conventionalcommits.org/) formaadis
  (`feat(event): ...`, `fix: ...`, `chore: ...`).
