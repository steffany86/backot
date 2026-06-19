$ErrorActionPreference = "Stop"

$source = @{
  Server = "tigo.makiro.com.bo"
  Database = "BDSistemaAntenaPM"
  User = "sistemas"
  Password = "sametsis"
}

$target = @{
  Server = "172.16.0.13"
  Database = "BDSistemaAntenaPMTarija"
  User = "sistemas"
  Password = "sametsis"
}

$pending = @(
  "sp_ObtenerListaOrdenesTrabajo",
  "sp_ObtenerListaOrdenesTrabajo_OTWEB",
  "sp_ObtenerListaOrdenesTrabajoRFechas",
  "spx_ListarOtFinalizadas",
  "spx_ObtenerAuxiliaresConformacionCuadrillaWeb",
  "spx_ObtenerConformacionCuadrillaWeb",
  "spx_ObtenerConformacionCuadrillaWebPorId",
  "spx_ObtenerTecnicosConformacionCuadrillaWeb",
  "spx_RegistrarVentaParaRegistroOTwb",
  "spx_ValidarVentaYDetallewb",
  "TraerTodosLosProductos_SinFungibleWeb"
)

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupFile = Join-Path $PSScriptRoot "backup_tarija_pending_sp_before_dynamic_$timestamp.sql"
$applyFile = Join-Path $PSScriptRoot "_tmp_force_tarija_pending_sp_dynamic_$timestamp.sql"
$okFile = Join-Path $PSScriptRoot "forced_ok_tarija_pending_sp_dynamic_$timestamp.txt"
$failedFile = Join-Path $PSScriptRoot "forced_failed_tarija_pending_sp_dynamic_$timestamp.txt"

function New-ConnectionString($cfg) {
  "Server=$($cfg.Server);Database=$($cfg.Database);User ID=$($cfg.User);Password=$($cfg.Password);TrustServerCertificate=True;Encrypt=False;Connection Timeout=20"
}

function Invoke-Scalar($cfg, [string]$sql, [object[]]$params = @()) {
  $conn = [System.Data.SqlClient.SqlConnection]::new((New-ConnectionString $cfg))
  $cmd = $conn.CreateCommand()
  $cmd.CommandTimeout = 120
  $cmd.CommandText = $sql
  for ($i = 0; $i -lt $params.Count; $i++) {
    [void]$cmd.Parameters.AddWithValue("@p$i", $params[$i])
  }
  $conn.Open()
  try {
    return $cmd.ExecuteScalar()
  } finally {
    $conn.Close()
  }
}

function Execute-NonQuery($cfg, [string]$sql) {
  $conn = [System.Data.SqlClient.SqlConnection]::new((New-ConnectionString $cfg))
  $cmd = $conn.CreateCommand()
  $cmd.CommandTimeout = 180
  $cmd.CommandText = $sql
  $conn.Open()
  try {
    [void]$cmd.ExecuteNonQuery()
  } finally {
    $conn.Close()
  }
}

function Normalize-SpName([string]$name) {
  $name.Trim().ToLowerInvariant() -replace '^\[?dbo\]?\.', '' -replace '[\[\]]', ''
}

function Convert-CreateKeyword([string]$definition, [bool]$exists) {
  $keyword = if ($exists) { "ALTER" } else { "CREATE" }
  [regex]::Replace($definition.Trim(), '(?is)(?:create\s+or\s+alter|create|alter)\s+(procedure|proc)\b', "$keyword `$1", 1)
}

function Find-AsIndex([string]$definition) {
  $match = [regex]::Match($definition, '(?is)\bAS\b')
  if (-not $match.Success) {
    throw "No se encontro AS en la definicion."
  }
  return $match.Index
}

function Split-TopLevelComma([string]$text) {
  $items = New-Object System.Collections.Generic.List[string]
  $depth = 0
  $start = 0
  for ($i = 0; $i -lt $text.Length; $i++) {
    $ch = $text[$i]
    if ($ch -eq '(') { $depth++ }
    elseif ($ch -eq ')' -and $depth -gt 0) { $depth-- }
    elseif ($ch -eq ',' -and $depth -eq 0) {
      $items.Add($text.Substring($start, $i - $start).Trim())
      $start = $i + 1
    }
  }
  $tail = $text.Substring($start).Trim()
  if ($tail) { $items.Add($tail) }
  return $items
}

function Strip-Default([string]$decl) {
  $depth = 0
  for ($i = 0; $i -lt $decl.Length; $i++) {
    $ch = $decl[$i]
    if ($ch -eq '(') { $depth++ }
    elseif ($ch -eq ')' -and $depth -gt 0) { $depth-- }
    elseif ($ch -eq '=' -and $depth -eq 0) {
      $before = $decl.Substring(0, $i).Trim()
      $after = $decl.Substring($i + 1)
      if ($after -match '(?i)\bOUTPUT\b') {
        return "$before OUTPUT"
      }
      return $before
    }
  }
  return $decl.Trim()
}

function Get-ParameterDeclarations([string]$header) {
  $withoutCreate = [regex]::Replace($header, '(?is)^\s*(?:create\s+or\s+alter|create|alter)\s+(?:procedure|proc)\s+(?:\[?dbo\]?\.)?\[?[^\]\s\(]+\]?', '', 1).Trim()
  if (-not $withoutCreate.StartsWith("@")) {
    return @()
  }
  $decls = Split-TopLevelComma $withoutCreate
  $out = New-Object System.Collections.Generic.List[object]
  foreach ($decl in $decls) {
    if ($decl -notmatch '^\s*(@[A-Za-z0-9_]+)\s+(.+)$') { continue }
    $name = $matches[1]
    $clean = Strip-Default $decl
    $out.Add([pscustomobject]@{ Name = $name; Declaration = $clean })
  }
  return $out
}

function SqlStringLiteral([string]$value) {
  "N'" + ($value -replace "'", "''") + "'"
}

function Build-DynamicWrapper([string]$definition, [bool]$exists) {
  $converted = Convert-CreateKeyword $definition $exists
  $asIndex = Find-AsIndex $converted
  $header = $converted.Substring(0, $asIndex).TrimEnd()
  $body = $converted.Substring($asIndex + 2).Trim()
  $params = Get-ParameterDeclarations $header

  $sb = New-Object System.Text.StringBuilder
  [void]$sb.AppendLine($header)
  [void]$sb.AppendLine("AS")
  [void]$sb.AppendLine("BEGIN")
  [void]$sb.AppendLine("    SET NOCOUNT ON;")
  [void]$sb.AppendLine("    DECLARE @__sql NVARCHAR(MAX);")
  [void]$sb.AppendLine("    SET @__sql = " + (SqlStringLiteral $body) + ";")

  if ($params.Count -gt 0) {
    $paramDef = ($params | ForEach-Object { $_.Declaration }) -join ", "
    $assignments = ($params | ForEach-Object { "$($_.Name) = $($_.Name)" }) -join ", "
    [void]$sb.AppendLine("    EXEC sp_executesql @__sql, " + (SqlStringLiteral $paramDef) + ", $assignments;")
  } else {
    [void]$sb.AppendLine("    EXEC sp_executesql @__sql;")
  }
  [void]$sb.AppendLine("END")
  return $sb.ToString()
}

$backup = New-Object System.Text.StringBuilder
[void]$backup.AppendLine("-- Backup Tarija antes de forzar SP dinamicos $timestamp")
[void]$backup.AppendLine("USE [$($target.Database)];")
[void]$backup.AppendLine("GO")

$apply = New-Object System.Text.StringBuilder
[void]$apply.AppendLine("-- SP pendientes Tarija como wrappers dinamicos $timestamp")
[void]$apply.AppendLine("USE [$($target.Database)];")
[void]$apply.AppendLine("GO")

"" | Set-Content $okFile
"" | Set-Content $failedFile

foreach ($name in $pending) {
  $sourceDef = [string](Invoke-Scalar $source "SELECT sm.definition FROM sys.objects o JOIN sys.sql_modules sm ON sm.object_id=o.object_id WHERE o.type='P' AND LOWER(o.name)=LOWER(@p0)" @($name))
  if ([string]::IsNullOrWhiteSpace($sourceDef)) {
    Add-Content $failedFile "MISSING_SOURCE`t$name"
    continue
  }

  $targetDef = [string](Invoke-Scalar $target "SELECT sm.definition FROM sys.objects o JOIN sys.sql_modules sm ON sm.object_id=o.object_id WHERE o.type='P' AND LOWER(o.name)=LOWER(@p0)" @($name))
  $exists = -not [string]::IsNullOrWhiteSpace($targetDef)

  if ($exists) {
    [void]$backup.AppendLine("")
    [void]$backup.AppendLine("/* [$name] */")
    [void]$backup.AppendLine($targetDef)
    [void]$backup.AppendLine("GO")
  }

  try {
    $wrapper = Build-DynamicWrapper $sourceDef $exists
    [void]$apply.AppendLine("")
    [void]$apply.AppendLine("/* DYNAMIC [$name] */")
    [void]$apply.AppendLine($wrapper)
    [void]$apply.AppendLine("GO")
    Execute-NonQuery $target $wrapper
    Add-Content $okFile "$name"
  } catch {
    $msg = $_.Exception.Message -replace "`r?`n", " | "
    Add-Content $failedFile "$name`t$msg"
  }
}

[System.IO.File]::WriteAllText($backupFile, $backup.ToString(), [System.Text.Encoding]::UTF8)
[System.IO.File]::WriteAllText($applyFile, $apply.ToString(), [System.Text.Encoding]::UTF8)

Write-Host "BACKUP=$backupFile"
Write-Host "APPLY_SQL=$applyFile"
Write-Host "OK=$okFile"
Write-Host "FAILED=$failedFile"
Write-Host "OK_COUNT=$((Get-Content $okFile | Where-Object { $_.Trim() }).Count)"
Write-Host "FAILED_COUNT=$((Get-Content $failedFile | Where-Object { $_.Trim() }).Count)"
