$ErrorActionPreference = "Stop"
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
$cs = "Server=tigo.makiro.com.bo;Database=BDSistemaAntenaPM;User ID=sistemas;Password=sametsis;TrustServerCertificate=True;Encrypt=False;Connection Timeout=20"
$conn = [System.Data.SqlClient.SqlConnection]::new($cs)
$conn.Open()
foreach ($n in $names) {
  $cmd = $conn.CreateCommand()
  $cmd.CommandText = "SELECT sm.definition FROM sys.objects o JOIN sys.sql_modules sm ON sm.object_id=o.object_id WHERE o.type='P' AND LOWER(o.name)=LOWER(@n)"
  [void]$cmd.Parameters.AddWithValue("@n", $n)
  $def = [string]$cmd.ExecuteScalar()
  Write-Output "===== $n len=$($def.Length) ====="
  if ($def.Length -gt 900) {
    Write-Output $def.Substring(0, 900)
  } else {
    Write-Output $def
  }
}
$conn.Close()
