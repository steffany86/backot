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

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$usedFile = Join-Path $PSScriptRoot "sp_used_by_system.txt"
$backupFile = Join-Path $PSScriptRoot "backup_tarija_sp_before_sync_$timestamp.sql"
$reportFile = Join-Path $PSScriptRoot "sync_tarija_used_sp_report_$timestamp.tsv"
$applyFile = Join-Path $PSScriptRoot "_tmp_sync_tarija_used_sp_$timestamp.sql"
$okFile = Join-Path $PSScriptRoot "applied_ok_tarija_$timestamp.txt"
$failedFile = Join-Path $PSScriptRoot "applied_failed_tarija_$timestamp.txt"

function New-ConnectionString($cfg) {
  "Server=$($cfg.Server);Database=$($cfg.Database);User ID=$($cfg.User);Password=$($cfg.Password);TrustServerCertificate=True;Encrypt=False;Connection Timeout=20"
}

function Invoke-Query($cfg, [string]$sql, [object[]]$params = @()) {
  $conn = [System.Data.SqlClient.SqlConnection]::new((New-ConnectionString $cfg))
  $cmd = $conn.CreateCommand()
  $cmd.CommandTimeout = 120
  $cmd.CommandText = $sql
  for ($i = 0; $i -lt $params.Count; $i++) {
    [void]$cmd.Parameters.AddWithValue("@p$i", $params[$i])
  }
  $dt = [System.Data.DataTable]::new()
  $conn.Open()
  try {
    $da = [System.Data.SqlClient.SqlDataAdapter]::new($cmd)
    [void]$da.Fill($dt)
  } finally {
    $conn.Close()
  }
  return ,$dt
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
  if ([string]::IsNullOrWhiteSpace($name)) { return "" }
  $n = $name.Trim().ToLowerInvariant()
  $n = $n -replace '^\[?dbo\]?\.', ''
  $n = $n -replace '[\[\]]', ''
  $n
}

function Normalize-Definition([string]$definition) {
  if ([string]::IsNullOrWhiteSpace($definition)) { return "" }
  $d = $definition.ToLowerInvariant()
  $d = $d -replace '\r\n?', "`n"
  $d = $d -replace '/\*.*?\*/', ' '
  $d = $d -replace '--.*?(\n|$)', ' '
  $d = $d -replace '\[dbo\]\.', 'dbo.'
  $d = $d -replace '\[|\]', ''
  $d = $d -replace '\bcreate\s+or\s+alter\s+procedure\b', 'create proc'
  $d = $d -replace '\bcreate\s+or\s+alter\s+proc\b', 'create proc'
  $d = $d -replace '\bcreate\s+procedure\b', 'create proc'
  $d = $d -replace '\balter\s+procedure\b', 'alter proc'
  $d = $d -replace '\bprocedure\b', 'proc'
  $d = $d -replace '\bdbo\.', ''
  $d = $d -replace '\s+', ' '
  $d.Trim()
}

function Convert-ToBatch([string]$definition, [bool]$existsInTarget) {
  $d = $definition.Trim()
  $keyword = if ($existsInTarget) { "ALTER" } else { "CREATE" }
  $pattern = '(?is)(?:create\s+or\s+alter|create|alter)\s+(procedure|proc)\b'
  if ($d -match $pattern) {
    return [regex]::Replace($d, $pattern, "$keyword `$1", 1)
  }
  return $d
}

$usedNames = Get-Content $usedFile |
  ForEach-Object { Normalize-SpName $_ } |
  Where-Object { $_ -and $_ -ne "dbo" -and $_ -notlike "*-*" } |
  Sort-Object -Unique

$sourceRows = Invoke-Query $source @"
SELECT LOWER(o.name) AS sp_name, sm.definition AS sp_definition
FROM sys.objects o
JOIN sys.sql_modules sm ON sm.object_id = o.object_id
WHERE o.type = 'P'
"@

$targetRows = Invoke-Query $target @"
SELECT LOWER(o.name) AS sp_name, sm.definition AS sp_definition
FROM sys.objects o
JOIN sys.sql_modules sm ON sm.object_id = o.object_id
WHERE o.type = 'P'
"@

$sourceDefs = @{}
for ($idx = 0; $idx -lt $sourceRows.Rows.Count; $idx++) {
  $row = $sourceRows.Rows[$idx]
  $sourceDefs[(Normalize-SpName ([string]$row.Item("sp_name")))] = [string]$row.Item("sp_definition")
}

$targetDefs = @{}
for ($idx = 0; $idx -lt $targetRows.Rows.Count; $idx++) {
  $row = $targetRows.Rows[$idx]
  $targetDefs[(Normalize-SpName ([string]$row.Item("sp_name")))] = [string]$row.Item("sp_definition")
}

$candidates = @()
foreach ($sp in $usedNames) {
  if (-not $sourceDefs.ContainsKey($sp)) {
    $candidates += [pscustomobject]@{ sp = $sp; estado = "NO_EXISTE_EN_SANTACRUZ"; accion = "SKIP"; source_len = 0; target_len = if ($targetDefs.ContainsKey($sp)) { $targetDefs[$sp].Length } else { 0 } }
    continue
  }

  if (-not $targetDefs.ContainsKey($sp)) {
    $candidates += [pscustomobject]@{ sp = $sp; estado = "FALTA_EN_TARIJA"; accion = "CREATE"; source_len = $sourceDefs[$sp].Length; target_len = 0 }
    continue
  }

  $srcNorm = Normalize-Definition $sourceDefs[$sp]
  $tarNorm = Normalize-Definition $targetDefs[$sp]
  if ($srcNorm -ne $tarNorm) {
    $candidates += [pscustomobject]@{ sp = $sp; estado = "DIFERENTE_LOGICA"; accion = "ALTER"; source_len = $sourceDefs[$sp].Length; target_len = $targetDefs[$sp].Length }
  } else {
    $candidates += [pscustomobject]@{ sp = $sp; estado = "IGUAL_LOGICA"; accion = "SKIP"; source_len = $sourceDefs[$sp].Length; target_len = $targetDefs[$sp].Length }
  }
}

$candidates | Export-Csv -Path $reportFile -Delimiter "`t" -NoTypeInformation -Encoding UTF8

$toApply = $candidates | Where-Object { $_.accion -eq "CREATE" -or $_.accion -eq "ALTER" }

$backup = New-Object System.Text.StringBuilder
[void]$backup.AppendLine("-- Backup SP Tarija antes de sync $timestamp")
[void]$backup.AppendLine("USE [$($target.Database)];")
[void]$backup.AppendLine("GO")
foreach ($item in $toApply) {
  if ($targetDefs.ContainsKey($item.sp)) {
    [void]$backup.AppendLine("")
    [void]$backup.AppendLine("/* [$($item.sp)] */")
    [void]$backup.AppendLine($targetDefs[$item.sp])
    [void]$backup.AppendLine("GO")
  }
}
[System.IO.File]::WriteAllText($backupFile, $backup.ToString(), [System.Text.Encoding]::UTF8)

$apply = New-Object System.Text.StringBuilder
[void]$apply.AppendLine("-- Sync SP Tarija desde Santa Cruz $timestamp")
[void]$apply.AppendLine("USE [$($target.Database)];")
[void]$apply.AppendLine("GO")
foreach ($item in $toApply) {
  $batch = Convert-ToBatch $sourceDefs[$item.sp] ($item.accion -eq "ALTER")
  [void]$apply.AppendLine("")
  [void]$apply.AppendLine("/* $($item.accion) [$($item.sp)] */")
  [void]$apply.AppendLine($batch)
  [void]$apply.AppendLine("GO")
}
[System.IO.File]::WriteAllText($applyFile, $apply.ToString(), [System.Text.Encoding]::UTF8)

"" | Set-Content $okFile
"" | Set-Content $failedFile

foreach ($item in $toApply) {
  try {
    $batch = Convert-ToBatch $sourceDefs[$item.sp] ($item.accion -eq "ALTER")
    Execute-NonQuery $target $batch
    Add-Content $okFile "$($item.accion)`t$($item.sp)"
  } catch {
    $msg = $_.Exception.Message -replace "`r?`n", " | "
    Add-Content $failedFile "$($item.accion)`t$($item.sp)`t$msg"
  }
}

$afterRows = Invoke-Query $target @"
SELECT LOWER(o.name) AS sp_name, sm.definition AS sp_definition
FROM sys.objects o
JOIN sys.sql_modules sm ON sm.object_id = o.object_id
WHERE o.type = 'P'
"@
$afterDefs = @{}
for ($idx = 0; $idx -lt $afterRows.Rows.Count; $idx++) {
  $row = $afterRows.Rows[$idx]
  $afterDefs[(Normalize-SpName ([string]$row.Item("sp_name")))] = [string]$row.Item("sp_definition")
}

$remaining = @()
foreach ($item in $toApply) {
  if (-not $afterDefs.ContainsKey($item.sp)) {
    $remaining += [pscustomobject]@{ sp = $item.sp; estado = "SIGUE_FALTANDO" }
    continue
  }
  if ((Normalize-Definition $sourceDefs[$item.sp]) -ne (Normalize-Definition $afterDefs[$item.sp])) {
    $remaining += [pscustomobject]@{ sp = $item.sp; estado = "SIGUE_DIFERENTE" }
  }
}
$remainingFile = Join-Path $PSScriptRoot "sync_tarija_remaining_$timestamp.tsv"
$remaining | Export-Csv -Path $remainingFile -Delimiter "`t" -NoTypeInformation -Encoding UTF8

Write-Host "REPORT=$reportFile"
Write-Host "BACKUP=$backupFile"
Write-Host "APPLY_SQL=$applyFile"
Write-Host "OK=$okFile"
Write-Host "FAILED=$failedFile"
Write-Host "REMAINING=$remainingFile"
Write-Host "TO_APPLY=$($toApply.Count)"
Write-Host "OK_COUNT=$((Get-Content $okFile | Where-Object { $_.Trim() }).Count)"
Write-Host "FAILED_COUNT=$((Get-Content $failedFile | Where-Object { $_.Trim() }).Count)"
Write-Host "REMAINING_COUNT=$($remaining.Count)"
