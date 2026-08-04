$names = @(
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
$cs = "Server=172.16.0.13;Database=BDSistemaAntenaPMTarija;User ID=sistemas;Password=sametsis;TrustServerCertificate=True;Encrypt=False;Connection Timeout=20"
$conn = [System.Data.SqlClient.SqlConnection]::new($cs)
$conn.Open()
foreach ($n in $names) {
  $cmd = $conn.CreateCommand()
  $cmd.CommandText = "SELECT o.name, LEN(sm.definition) AS def_len FROM sys.objects o JOIN sys.sql_modules sm ON sm.object_id=o.object_id WHERE o.type='P' AND LOWER(o.name)=LOWER(@n)"
  [void]$cmd.Parameters.AddWithValue("@n", $n)
  $da = [System.Data.SqlClient.SqlDataAdapter]::new($cmd)
  $dt = [System.Data.DataTable]::new()
  [void]$da.Fill($dt)
  if ($dt.Rows.Count -eq 0) {
    Write-Output "FALTA`t$n"
  } else {
    Write-Output "OK`t$($dt.Rows[0].Item("name"))`tlen=$($dt.Rows[0].Item("def_len"))"
  }
}
$conn.Close()
