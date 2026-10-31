# game-leaderboard-service

Leaderboard backend for multiplayer games. Match results come in through a REST API, go through Kafka, get
stored in Postgres, and rankings are served from Redis sorted sets.

Built with Java 21, Spring Boot 4, Kafka, PostgreSQL, Redis, Flyway, Testcontainers and Docker.

## How it works

```
POST /api/v1/matches -> Kafka (match-completed) -> consumer -> Postgres (totals) -> Redis (sorted set per game)
GET  /api/v1/games/{gameId}/leaderboard          -> Redis
POST /api/v1/admin/games/{gameId}/leaderboard/rebuild -> Postgres -> Redis
```

1. The API validates the match and publishes it to Kafka, keyed by `gameId`. It returns `202` only after
   Kafka acks the message (`acks=all`).
2. The consumer saves the match id and adds the points in one transaction. If the same match id comes in
   again (Kafka can deliver twice), the insert into `processed_matches` does nothing and the points are not
   added again.
3. Points are added with `INSERT ... ON CONFLICT DO UPDATE`, so two updates at the same time can't
   overwrite each other.
4. After the commit, the player's total from Postgres is written to Redis with `ZADD GT`. Since it's the full
   total and not an increment, retrying is safe. Totals never go down, so `GT` also stops a late retry from
   overwriting a newer total. If Redis is down, the message gets retried and Redis catches up.
5. If a message keeps failing (3 retries) or can't be parsed, it goes to `match-completed.dlt` so it doesn't
   block the partition.

Postgres is the source of truth. Redis is just for fast reads (rank lookups are O(log N)). If Redis loses
data, the rebuild endpoint reloads a game from Postgres in pages of 1,000 players (keyset pagination on the
`(game_id, player_id)` primary key, so new players arriving mid-rebuild can't shift pages). It's safe to run
while matches are being processed, because of `ZADD GT`.

## API

Submit a match:

```bash
curl -X POST localhost:8080/api/v1/matches -H 'Content-Type: application/json' -d '{
  "matchId": "11111111-1111-4111-8111-111111111111",
  "gameId": "fc-arena",
  "results": [{"playerId": "alice", "points": 30}, {"playerId": "bob", "points": 50}]
}'
```

Top players (`limit` 1-100, default 10):

```bash
curl 'localhost:8080/api/v1/games/fc-arena/leaderboard?limit=10'
# {"gameId":"fc-arena","entries":[{"rank":1,"playerId":"bob","points":50},{"rank":2,"playerId":"alice","points":30}]}
```

One player:

```bash
curl localhost:8080/api/v1/games/fc-arena/leaderboard/players/bob
# {"rank":1,"playerId":"bob","points":50}
```

Errors are returned as problem details (RFC 9457):

- `400` invalid input (ids must match `[a-z0-9_-]{1,64}`, points 0 to 1,000,000, max 100 results, no
  player twice in one match)
- `404` player has no score in that game
- `503` Kafka didn't ack in time, safe to retry with the same `matchId`

Rebuild a game's leaderboard from Postgres (after a Redis flush or restart without persistence):

```bash
curl -X POST localhost:8080/api/v1/admin/games/fc-arena/leaderboard/rebuild
# {"gameId":"fc-arena","playersLoaded":2}
```

## Running it

Needs JDK 21+ and Docker.

```bash
# everything in docker
docker compose --profile app up -d --build --wait

# or just postgres/redis/kafka in docker and the app locally
docker compose up -d --wait
./mvnw spring-boot:run

# stop and remove data
docker compose --profile app down -v
```

If 5432/6379 are already taken: `POSTGRES_PORT=15432 REDIS_PORT=16379 docker compose ...`

Metrics are at `/actuator/prometheus` (`leaderboard_matches_processed_total`,
`leaderboard_matches_duplicate_total`, `leaderboard_matches_processing_seconds`), and health at
`/actuator/health`.

## Tests

```bash
./mvnw verify
```

Unit tests, `@WebMvcTest` controller tests, and integration tests that run against real Postgres, Redis and
Kafka using Testcontainers (full flow, duplicate matches, bad message going to the DLT, rebuilding after Redis data loss, stale cache
writes being ignored). CI runs the same
thing on GitHub Actions.

## TODO

- tie-breaking by time; right now equal scores are ordered by player id
- auth + rate limiting on the write and admin endpoints
- tracing with OpenTelemetry
