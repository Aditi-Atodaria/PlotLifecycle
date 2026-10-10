# Deployment guide

The app is a single Spring Boot service with a SQLite database file. Anything that can run a Docker
container and keep a persistent disk can host it.

## Before you go live

1. **Set strong passwords** for the three seeded accounts with `SEED_ADMIN_PASSWORD`, `SEED_AGENT_PASSWORD`
   and `SEED_ACCOUNTANT_PASSWORD`. They are used only when the database is first created; changing them later
   does not alter existing accounts. If you already started once with the defaults, delete the database file so it
   is recreated.
2. **Serve over HTTPS** and set `COOKIE_SECURE=true`.
3. **Decide who may sign up.** Visitors can register as agents or accountants. Set `ALLOW_REGISTRATION=false` to
   disable that. Admin accounts can never be created from the sign-up page.
4. **Attach a persistent disk** mounted at `/data`. Without one, all data is lost on every redeploy.
5. **Run a single instance.** SQLite is one file and does not support several app servers sharing it.

If the site is a public demo, you can set `SHOW_DEMO_CREDENTIALS=true` to show the demo logins on the sign-in
page, but only with the default passwords and no real data.

## Docker

```bash
docker build -t plot-lifecycle .
docker run -p 8080:8080 -v plotdata:/data \
  -e SEED_ADMIN_PASSWORD=change-me -e SEED_AGENT_PASSWORD=change-me -e SEED_ACCOUNTANT_PASSWORD=change-me \
  plot-lifecycle
```

## Docker Compose

```bash
cp .env.example .env     # edit the values
docker compose up --build -d
```

Data is kept in the `plotdata` volume, and the container restarts automatically.

## Hosted platforms (Render, Railway, Fly.io)

1. Create a new service from your GitHub repository and choose the **Dockerfile** build.
2. **Attach a persistent disk or volume mounted at `/data`.** On Render, persistent disks need a paid instance
   type; on a free plan the data is lost on redeploy.
3. Add the environment variables from the table in the README. Do not set `PORT` unless the platform asks for it;
   the app reads it automatically.
4. Set the health check path to **`/actuator/health/liveness`**.
5. Turn on HTTPS (the platforms do this by default) and set `COOKIE_SECURE=true`.

## Without Docker

```bash
mvn clean package
DB_PATH=/var/lib/plotdata/realestate.db java -jar target/*.jar
```

Put a reverse proxy such as nginx or Caddy in front for HTTPS. The app already trusts the `X-Forwarded-*` headers.

## Backups

The whole database is one file. Stop the app or use SQLite's online backup, then copy it:

```bash
sqlite3 /data/realestate.db ".backup '/backups/realestate-$(date +%F).db'"
```

Restore by stopping the app and replacing the file at `DB_PATH`.

## Upgrading

Tables are created and extended automatically on start (`ddl-auto=update`). Back up the database first. If a release
makes an incompatible change, the symptom is a Hibernate error at start-up; restore from backup or recreate the
database.

## Troubleshooting

| Symptom | What to check |
|---|---|
| Build fails | Find the first `[ERROR]` line in the Maven output. It is usually a compilation error with a file and line |
| Data disappears after a deploy | No persistent disk mounted at `/data` |
| `unable to open database file` | The directory in `DB_PATH` does not exist or is not writable |
| Login works locally but not online | Check `COOKIE_SECURE`: it must be `true` only when the site is served over HTTPS |
| Default passwords still work | The database already existed when you set new ones. Delete it, or change the passwords in the app |
| Pages look unstyled | The browser cannot reach `cdn.jsdelivr.net` (Bootstrap and Chart.js load from there) |
| Platform health check fails | Use `/actuator/health/liveness`, and check the service is listening on the platform's `PORT` |
