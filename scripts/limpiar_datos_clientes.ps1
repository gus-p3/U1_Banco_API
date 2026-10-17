# ============================================================================
# Script PowerShell para ejecutar la limpieza de datos de clientes
# ============================================================================

$PGHOST = if ($env:PGHOST) { $env:PGHOST } else { "localhost" }
$PGPORT = if ($env:PGPORT) { $env:PGPORT } else { "5432" }
$PGDATABASE = if ($env:PGDATABASE) { $env:PGDATABASE } else { "banco" }
$PGUSER = if ($env:PGUSER) { $env:PGUSER } else { "postgres" }
$env:PGPASSWORD = if ($env:PGPASSWORD) { $env:PGPASSWORD } else { "linux" }

$scriptPath = Join-Path $PSScriptRoot "limpiar_datos_clientes.sql"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Limpiando base de datos: $PGDATABASE en ${PGHOST}:${PGPORT}" -ForegroundColor Cyan
Write-Host " Archivo: $scriptPath" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# Comprobar si psql existe localmente
if (Get-Command psql -ErrorAction SilentlyContinue) {
    Write-Host "Ejecutando script mediante psql local..." -ForegroundColor Green
    & psql -h $PGHOST -p $PGPORT -U $PGUSER -d $PGDATABASE -f $scriptPath
} else {
    Write-Host "psql no encontrado en PATH. Intentando mediante contenedor Docker 'postgres'..." -ForegroundColor Yellow
    if (Get-Command docker -ErrorAction SilentlyContinue) {
        Get-Content $scriptPath | docker exec -i postgres psql -U $PGUSER -d $PGDATABASE
    } else {
        Write-Host "AVISO: No se detectó 'psql' ni 'docker'. Puedes ejecutar directamente el archivo SQL en pgAdmin, DBeaver o IntelliJ:" -ForegroundColor Red
        Write-Host "  Ruta: $scriptPath" -ForegroundColor Yellow
    }
}
