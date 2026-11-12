# Local PostgreSQL helper (portable install).
#   .\scripts\db.ps1 start   - start the server on port 5432
#   .\scripts\db.ps1 stop    - stop it
#   .\scripts\db.ps1 status
#   .\scripts\db.ps1 reset   - drop and recreate the 'bookstore' database (Flyway re-seeds it on next app start)
#   .\scripts\db.ps1 psql    - open a psql shell on the bookstore database
param([ValidateSet('start', 'stop', 'status', 'reset', 'psql')][string]$Action = 'status')

. "$PSScriptRoot\env.ps1"
$log = Join-Path (Split-Path $env:PGDATA_DIR) 'postgres.log'

switch ($Action) {
    'start'  { pg_ctl -D $env:PGDATA_DIR -l $log -o "-p 5432" start -w }
    'stop'   { pg_ctl -D $env:PGDATA_DIR stop -m fast }
    'status' { pg_ctl -D $env:PGDATA_DIR status }
    'reset'  {
        # Dev-only superuser password set at initdb time.
        $env:PGPASSWORD = 'bookstore'
        psql -U postgres -h localhost -c "DROP DATABASE IF EXISTS bookstore WITH (FORCE);" -c "CREATE DATABASE bookstore OWNER bookstore;"
    }
    'psql'   { $env:PGPASSWORD = 'bookstore'; psql -U bookstore -h localhost -d bookstore }
}
