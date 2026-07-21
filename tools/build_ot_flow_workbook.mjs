import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { mkdirSync } from 'node:fs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const repoRoot = path.resolve(__dirname, '..');
const exceljsPath = pathToFileURL(
  'C:/Users/JOSUE CABRERA/Downloads/frontOT-front01062026/frontOT-erm02072026/node_modules/exceljs/excel.js',
).href;
const ExcelJS = (await import(exceljsPath)).default;

const outputDir = path.join(repoRoot, 'outputs', 'ot-flujo-materiales');
mkdirSync(outputDir, { recursive: true });
const outputPath = path.join(outputDir, 'flujo_ot_finalizar_cargar_material.xlsx');

const workbook = new ExcelJS.Workbook();
workbook.creator = 'Codex';
workbook.created = new Date();
workbook.modified = new Date();
workbook.properties.date1904 = false;

const palette = {
  navy: '1F4E78',
  blue: '5B9BD5',
  green: '70AD47',
  orange: 'ED7D31',
  red: 'C00000',
  gray: 'D9EAF7',
  light: 'F7FBFF',
  yellow: 'FFF2CC',
  white: 'FFFFFF',
};

function addSheet(name, columns) {
  const ws = workbook.addWorksheet(name, {
    views: [{ state: 'frozen', ySplit: 1 }],
  });
  ws.columns = columns;
  const header = ws.getRow(1);
  header.font = { bold: true, color: { argb: palette.white } };
  header.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: palette.navy } };
  header.alignment = { vertical: 'middle', wrapText: true };
  header.height = 24;
  header.eachCell((cell) => {
    cell.border = {
      top: { style: 'thin', color: { argb: palette.white } },
      left: { style: 'thin', color: { argb: palette.white } },
      bottom: { style: 'thin', color: { argb: palette.white } },
      right: { style: 'thin', color: { argb: palette.white } },
    };
  });
  ws.autoFilter = {
    from: { row: 1, column: 1 },
    to: { row: 1, column: columns.length },
  };
  return ws;
}

function addRows(ws, rows) {
  rows.forEach((row) => ws.addRow(row));
  ws.eachRow((row, rowNumber) => {
    row.eachCell((cell) => {
      cell.alignment = { vertical: 'top', wrapText: true };
      cell.border = {
        top: { style: 'thin', color: { argb: 'D9D9D9' } },
        left: { style: 'thin', color: { argb: 'D9D9D9' } },
        bottom: { style: 'thin', color: { argb: 'D9D9D9' } },
        right: { style: 'thin', color: { argb: 'D9D9D9' } },
      };
      if (rowNumber > 1 && rowNumber % 2 === 0) {
        cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: palette.light } };
      }
    });
  });
}

function addTitle(ws, title, subtitle) {
  ws.insertRows(1, [[title], [subtitle], []]);
  ws.mergeCells('A1:H1');
  ws.mergeCells('A2:H2');
  ws.getCell('A1').font = { bold: true, size: 18, color: { argb: palette.navy } };
  ws.getCell('A2').font = { italic: true, size: 11, color: { argb: '666666' } };
  ws.getCell('A1').alignment = { vertical: 'middle' };
  ws.getCell('A2').alignment = { vertical: 'middle', wrapText: true };
  ws.getRow(4).font = { bold: true, color: { argb: palette.white } };
  ws.getRow(4).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: palette.navy } };
  ws.views = [{ state: 'frozen', ySplit: 4 }];
  ws.autoFilter = {
    from: { row: 4, column: 1 },
    to: { row: 4, column: ws.columnCount },
  };
}

const resumen = addSheet('Resumen', [
  { header: 'Area', key: 'area', width: 24 },
  { header: 'Hallazgo', key: 'hallazgo', width: 60 },
  { header: 'Detalle operativo', key: 'detalle', width: 78 },
  { header: 'Impacto', key: 'impacto', width: 38 },
]);
addRows(resumen, [
  {
    area: 'Objetivo',
    hallazgo: 'Rehacer formulario OT: finalizar y cargar material',
    detalle:
      'El flujo actual separa la finalizacion sin materiales de la carga de materiales. La pantalla decide que enviar segun si existen materiales instalados/retirados/cargo usuario y el backend valida cierre, cuadre, stock, estado de serie/chip y estado funcional de la OT.',
    impacto: 'Base para redisenar el formulario sin romper reglas existentes.',
  },
  {
    area: 'Regla central',
    hallazgo: 'Finalizada vs pendiente se deriva de venta + detalle',
    detalle:
      'Finalizada: ExisteVenta=1 y no requiere detalle, o ExisteVenta=1 con TieneDetalle=1 y TieneDetalleEnCodigoVenta=1. Pendiente material: ExisteVenta=1, TieneDetalle=1 y TieneDetalleEnCodigoVenta=0.',
    impacto: 'El front debe mostrar estado desde backend; no conviene duplicar reglas.',
  },
  {
    area: 'SP criticos',
    hallazgo: 'La operacion mezcla SP y tablas directas',
    detalle:
      'Para registrar se usan spx_RegistrarVentaParaRegistroOTwb, spx_ValidarVentaYDetallewb_clon_paginacion, spx_ObtenerSaldoRuta, spx_ValidarCuadreRuta, spx_VerificarEstadoSerie, spx_RegMod_Productos y sp_ModificarOT_OTRealizada, mas inserts/updates en tbl_CodigoVenta, tbl_Devolucion, tbl_DetalleDevolucion y tbl_Venta.',
    impacto: 'Riesgo de divergencia si el nuevo formulario no respeta todas las validaciones.',
  },
  {
    area: 'Timeouts',
    hallazgo: 'Backend tiene timeout DB/transaction; front no define timeout HTTP explicito',
    detalle:
      'Hikari connection-timeout=60000 ms, validation-timeout=5000 ms, spring.transaction.default-timeout=300 s y registrarVentaParaRegistroOtWb usa @Transactional(timeout=300). Axios no muestra timeout configurado en otApi.ts.',
    impacto: 'El usuario puede quedar esperando sin mensaje claro en llamadas largas.',
  },
  {
    area: 'Mejora prioritaria',
    hallazgo: 'Crear endpoint de estado/accion permitida para el formulario',
    detalle:
      'Unificar en backend: estado OT, boton habilitado, motivo bloqueo, materiales requeridos, stock y validaciones previas. El front consume un DTO unico y deja de inferir.',
    impacto: 'Reduce bugs de sidebar/formulario y reglas duplicadas.',
  },
]);
addTitle(resumen, 'Documento de flujo OT - Finalizar / Cargar material', 'Analisis del flujo actual, SP involucrados, reglas de estado, timeouts y mejoras sugeridas.');

const flujoFront = addSheet('Flujo Front', [
  { header: 'Paso', key: 'paso', width: 10 },
  { header: 'Momento', key: 'momento', width: 30 },
  { header: 'Que hace el front', key: 'accion', width: 70 },
  { header: 'Endpoint/API', key: 'endpoint', width: 44 },
  { header: 'Datos clave', key: 'datos', width: 60 },
  { header: 'Resultado esperado', key: 'resultado', width: 56 },
]);
addRows(flujoFront, [
  { paso: 1, momento: 'Carga/listado de OT', accion: 'Obtiene ordenes y las presenta con campos enriquecidos de venta/detalle cuando el backend los entrega.', endpoint: 'GET listado OT / supervisor / agenda segun pantalla', datos: 'OrdenTrabajo, CodigoCliente, Fecha, IdEstado, ExisteVenta, TieneDetalle, HabilitarCargarMaterial.', resultado: 'El usuario ve si la OT puede finalizarse o cargar material.' },
  { paso: 2, momento: 'Abrir formulario OT', accion: 'Lee la OT seleccionada y prepara materiales instalados, retirados y cargo usuario.', endpoint: 'GET /ot/{idVenta}, /instalados, /retirados, /cargo-usuario si aplica', datos: 'idVenta, nroOT, codigoCliente, idRuta, idSucursal, tecnico.', resultado: 'Formulario con datos existentes o vacio para capturar.' },
  { paso: 3, momento: 'Validar estado venta/detalle', accion: 'Consulta si ya existe venta, si tiene detalle y si hay registros en codigo venta.', endpoint: 'GET /ot/venta/validar-detalle; fallback /ot/spx_ValidarVentaYDetallewb', datos: 'fecha, nroOT, numeroCliente, incluirManual=true, desdeAgenda=true.', resultado: 'Determina si esta finalizada, pendiente material o si no permite cargar.' },
  { paso: 4, momento: 'Antes de guardar', accion: 'Ejecuta prevalidaciones de cierre de almacen, cuadre de ruta y saldo disponible.', endpoint: '/ot/validaciones/registro-agenda, /cuadre/validar-hoy, /ot/saldo-ruta', datos: 'fechaTrabajo, idSucursal, idRuta, productos/cantidades.', resultado: 'Bloquea si hay cierre, cuadre o falta stock.' },
  { paso: 5, momento: 'Validar series/chips', accion: 'Valida serie/chip para materiales instalados y cargo usuario antes de enviar.', endpoint: 'Validaciones que terminan en spx_VerificarEstadoSerie', datos: 'idProducto, idTipoMaterial, serie, chip, idRuta.', resultado: 'Evita enviar materiales con estado no permitido.' },
  { paso: 6, momento: 'Guardar con materiales', accion: 'Si hay materiales, envia detalle de agenda. Si hay error tolerable en retirados, reintenta limpiando serie/chip segun caso.', endpoint: 'POST /ot/detalle-materiales', datos: 'idVenta, nroOT, codigoCliente, fechaTrabajo, idRuta, idUsuario, materiales.', resultado: 'Inserta detalle, descuenta/mueve productos y marca fecha de detalle.' },
  { paso: 7, momento: 'Guardar sin materiales', accion: 'Si no hay materiales, registra OT realizada sin detalle.', endpoint: 'POST /ot/realizada', datos: 'cabecera de venta/OT y estado.', resultado: 'Finaliza la OT sin cargar material.' },
  { paso: 8, momento: 'Cargo usuario', accion: 'Si existen items de cargo usuario, los registra despues del flujo principal.', endpoint: 'POST /ot/cargo-usuario', datos: 'idVenta, productos, cantidades, serie/chip si requiere.', resultado: 'Queda registrado cargo usuario asociado a la venta.' },
]);

const flujoBack = addSheet('Flujo Backend', [
  { header: 'Paso', key: 'paso', width: 10 },
  { header: 'Metodo/Clase', key: 'metodo', width: 44 },
  { header: 'Validacion o accion', key: 'accion', width: 78 },
  { header: 'SP/Tabla', key: 'sp', width: 56 },
  { header: 'Error/bloqueo posible', key: 'error', width: 48 },
]);
addRows(flujoBack, [
  { paso: 1, metodo: 'OtController.registrarDetalleMaterialesAgenda', accion: 'Recibe el POST /ot/detalle-materiales y delega al servicio.', sp: 'N/A', error: 'Datos faltantes en request.' },
  { paso: 2, metodo: 'OtService.registrarDetalleAgenda', accion: 'Resuelve venta por idVenta o por fecha + OT + cliente; valida idVenta, idRuta e idUsuario.', sp: 'Consultas a tbl_Venta/listado segun repo', error: 'VENTA_NO_ENCONTRADA o request invalido.' },
  { paso: 3, metodo: 'resolverRutaActivaConSaldo', accion: 'Confirma ruta activa y saldo para materiales.', sp: 'spx_ObtenerSaldoRuta; fallback spb_SaldoRutasCantidad_X_Ruta', error: 'Sin ruta activa o sin saldo.' },
  { paso: 4, metodo: 'validarRegistroAgenda', accion: 'Bloquea si existe cierre de almacen/agenda para la fecha y sucursal.', sp: 'spx_ExisteCierreAlmacenHoy / sp_ExisteCierreAlmacen y variantes PR/PD', error: 'REGISTRO_BLOQUEADO.' },
  { paso: 5, metodo: 'validarCuadreRuta', accion: 'Bloquea si la ruta ya realizo cuadre para la fecha.', sp: 'spx_ValidarCuadreRuta', error: 'CUADRE_REGISTRADO.' },
  { paso: 6, metodo: 'validarVentaYDetalleWb', accion: 'Verifica si ya hay venta/detalle y si el estado permite cargar material.', sp: 'spx_ValidarVentaYDetallewb_clon_paginacion', error: 'DETALLE_YA_REGISTRADO o ESTADO_NO_PERMITE_CARGAR_MATERIAL.' },
  { paso: 7, metodo: 'validarMaterialesDetalle', accion: 'Valida producto, tipo material, cantidad, duplicados y serie/chip segun metadata.', sp: 'spx_VerificarEstadoSerie para estados de serie/chip', error: 'Material invalido, duplicado o estado no permitido.' },
  { paso: 8, metodo: 'ejecutarEnTransaccionSucursal', accion: 'Inserta cada material y registra movimiento de producto.', sp: 'tbl_CodigoVenta + spx_RegMod_Productos accion 3', error: 'Rollback si falla la transaccion.' },
  { paso: 9, metodo: 'Devoluciones para retirados', accion: 'Si idTipoMaterial es 2 o 5, crea devolucion y detalle, y registra movimiento de devolucion.', sp: 'tbl_Devolucion, tbl_DetalleDevolucion, spx_RegMod_Productos accion 35', error: 'Rollback si falla devolucion.' },
  { paso: 10, metodo: 'Actualizar venta/estado', accion: 'Si viene idEstado llama modificacion OT realizada y actualiza FechaHoraDetalle.', sp: 'sp_ModificarOT_OTRealizada + UPDATE tbl_Venta', error: 'Rollback si falla actualizacion.' },
]);

const sps = addSheet('SP y Tablas', [
  { header: 'Objeto', key: 'objeto', width: 42 },
  { header: 'Tipo', key: 'tipo', width: 16 },
  { header: 'Uso en el flujo', key: 'uso', width: 76 },
  { header: 'Entrada principal', key: 'entrada', width: 54 },
  { header: 'Salida/campo usado', key: 'salida', width: 60 },
  { header: 'Lugar observado', key: 'lugar', width: 54 },
]);
addRows(sps, [
  { objeto: 'spx_ValidarVentaYDetallewb_clon_paginacion', tipo: 'SP', uso: 'Determina si existe venta, si requiere detalle y si ya hay detalle en codigo venta.', entrada: 'fecha, nroOT, codigoCliente/numeroCliente, incluirManual, desdeAgenda.', salida: 'ExisteVenta, TieneDetalle, TieneDetalleEnCodigoVenta, CantidadVentas, CantidadDetalles, AddMaterial_o_CargoUsuario, HabilitarCargarMaterial.', lugar: 'OtRepository / OtService / otApi.ts' },
  { objeto: 'spx_RegistrarVentaParaRegistroOTwb', tipo: 'SP', uso: 'Registra cabecera/venta para OT cuando corresponde.', entrada: 'Datos de cabecera OT, usuario, ruta, sucursal.', salida: 'idVenta / resultado de registro.', lugar: 'OtRepository.registrarVentaParaRegistroOtWb' },
  { objeto: 'spx_ObtenerCaberaVentaParaRegistroOTwb', tipo: 'SP', uso: 'Obtiene cabecera para preparar registro OT.', entrada: 'OT, cliente, fecha/sucursal segun llamada.', salida: 'Cabecera de venta.', lugar: 'OtRepository / endpoints legacy' },
  { objeto: 'spx_ObtenerSaldoRuta', tipo: 'SP', uso: 'Consulta stock/saldo disponible por ruta para bloquear faltantes.', entrada: 'idRuta, fecha, sucursal/producto segun repo.', salida: 'Producto, cantidad disponible.', lugar: 'OtRepository / OtService / otApi.ts' },
  { objeto: 'spb_SaldoRutasCantidad_X_Ruta', tipo: 'SP fallback', uso: 'Fallback de saldo de ruta.', entrada: 'idRuta y filtros.', salida: 'Cantidad disponible.', lugar: 'OtRepository' },
  { objeto: 'spx_ValidarCuadreRuta', tipo: 'SP', uso: 'Valida si la ruta ya hizo cuadre para la fecha.', entrada: 'idRuta, fechaTrabajo, idSucursal.', salida: 'Booleano/flag de cuadre.', lugar: 'OtController / OtRepository' },
  { objeto: 'spx_ExisteCierreAlmacenHoy / sp_ExisteCierreAlmacen', tipo: 'SP', uso: 'Valida cierre de almacen para bloquear registro.', entrada: 'fechaTrabajo, idSucursal.', salida: 'Bloqueado=true/false.', lugar: 'OtService.validarRegistroAgenda' },
  { objeto: 'spx_ExisteCierreAlmacenHoyPR_PD / sp_ExisteCierreAlmacenPRPD', tipo: 'SP', uso: 'Variante de cierre para PR/PD.', entrada: 'fechaTrabajo, idSucursal.', salida: 'Bloqueado=true/false.', lugar: 'OtRepository' },
  { objeto: 'spx_ValidaMovimientos / sp_ValidaMovimientos', tipo: 'SP', uso: 'Valida movimientos antes de permitir acciones de inventario.', entrada: 'ruta/fecha/sucursal segun repo.', salida: 'Flag o resultado de validacion.', lugar: 'OtRepository' },
  { objeto: 'spx_VerificarEstadoSerie', tipo: 'SP', uso: 'Valida estado de serie/chip antes de instalar, retirar o cargar a usuario.', entrada: 'idProducto, serie, chip, accion=3, ruta/sucursal.', salida: 'Estado permitido o mensaje de bloqueo.', lugar: 'OtService.validarEstadoSerie / validaciones front' },
  { objeto: 'spx_RegMod_Productos', tipo: 'SP', uso: 'Registra movimiento de producto al usar material o devolucion.', entrada: 'Producto, serie/chip, cantidad, ruta, accion 3 o 35.', salida: 'Resultado del movimiento.', lugar: 'OtRepository.registrarMovimientoProducto' },
  { objeto: 'sp_ModificarOT_OTRealizada', tipo: 'SP', uso: 'Marca/modifica la OT como realizada segun estado enviado.', entrada: 'idVenta/idEstado y datos OT.', salida: 'Resultado actualizacion.', lugar: 'OtRepository' },
  { objeto: 'sp_ObtenerListaOrdenesTrabajo_OTWEB_clon_paginacion', tipo: 'SP listado', uso: 'Lista OT para pantallas OTWEB con paginacion.', entrada: 'Filtros fecha, tecnico, sucursal, estado, paginacion.', salida: 'Ordenes de trabajo.', lugar: 'ListaOtRepository' },
  { objeto: 'spx_ListarOtFinalizadas', tipo: 'SP listado', uso: 'Lista OT finalizadas segun filtros.', entrada: 'Fecha/ruta/tecnico/sucursal.', salida: 'Ordenes finalizadas.', lugar: 'OtRepository' },
  { objeto: 'tbl_CodigoVenta', tipo: 'Tabla', uso: 'Guarda detalle de materiales instalados/retirados.', entrada: 'idVenta, producto, tipo, cantidad, serie/chip.', salida: 'Detalle existente para estado finalizado.', lugar: 'OtRepository.insertarCodigoVenta' },
  { objeto: 'tbl_CodigoVentaCargoUsuario', tipo: 'Tabla', uso: 'Guarda materiales cargados al usuario.', entrada: 'idVenta, producto, cantidad, serie/chip.', salida: 'Detalle cargo usuario.', lugar: 'OtRepository' },
  { objeto: 'tbl_Devolucion / tbl_DetalleDevolucion', tipo: 'Tabla', uso: 'Registra devoluciones para materiales retirados.', entrada: 'idRuta, idUsuario, producto, serie/chip, cantidad.', salida: 'Devolucion generada.', lugar: 'OtService.registrarDetalleAgenda' },
  { objeto: 'tbl_Venta', tipo: 'Tabla', uso: 'Venta/cabecera OT; se actualiza FechaHoraDetalle y campos tecnicos.', entrada: 'idVenta y campos de OT.', salida: 'Estado/campos de venta para validaciones.', lugar: 'OtRepository updates' },
]);

const estados = addSheet('Estados OT', [
  { header: 'Estado funcional', key: 'estado', width: 28 },
  { header: 'ExisteVenta', key: 'existe', width: 16 },
  { header: 'TieneDetalle', key: 'tiene', width: 18 },
  { header: 'TieneDetalleEnCodigoVenta', key: 'codigo', width: 26 },
  { header: 'Condicion', key: 'condicion', width: 72 },
  { header: 'Que deberia mostrar el formulario', key: 'ui', width: 70 },
]);
addRows(estados, [
  { estado: 'Finalizada sin material', existe: 1, tiene: 0, codigo: 0, condicion: 'La venta existe y funcionalmente no requiere detalle material.', ui: 'Mostrar como finalizada. No pedir carga de material.' },
  { estado: 'Finalizada con material', existe: 1, tiene: 1, codigo: 1, condicion: 'La venta existe, requiere detalle y ya tiene registros en tbl_CodigoVenta.', ui: 'Mostrar como finalizada. Permitir visualizar detalle, no duplicar carga.' },
  { estado: 'Pendiente material', existe: 1, tiene: 1, codigo: 0, condicion: 'La venta existe, requiere detalle pero todavia no hay registros en codigo venta.', ui: 'Mostrar boton Cargar material y motivo pendiente.' },
  { estado: 'No registrada / revisar', existe: 0, tiene: 'N/A', codigo: 'N/A', condicion: 'No existe venta para OT/cliente/fecha o la validacion no encuentra coincidencia.', ui: 'No permitir cargar material hasta registrar/crear venta o resolver datos.' },
  { estado: 'Bloqueada por cierre', existe: 'N/A', tiene: 'N/A', codigo: 'N/A', condicion: 'Existe cierre de almacen para fecha/sucursal.', ui: 'Deshabilitar guardar y mostrar mensaje de cierre.' },
  { estado: 'Bloqueada por cuadre', existe: 'N/A', tiene: 'N/A', codigo: 'N/A', condicion: 'La ruta ya realizo cuadre para la fecha.', ui: 'Deshabilitar guardar y mostrar mensaje de cuadre.' },
  { estado: 'Bloqueada por stock', existe: 'N/A', tiene: 'N/A', codigo: 'N/A', condicion: 'Saldo de ruta insuficiente para los productos/cantidades.', ui: 'Mostrar productos faltantes y no enviar registro.' },
]);

const timeouts = addSheet('Timeouts y Riesgos', [
  { header: 'Componente', key: 'componente', width: 34 },
  { header: 'Timeout/configuracion', key: 'timeout', width: 38 },
  { header: 'Valor actual observado', key: 'valor', width: 28 },
  { header: 'Riesgo', key: 'riesgo', width: 70 },
  { header: 'Sugerencia', key: 'sugerencia', width: 70 },
  { header: 'Prioridad', key: 'prioridad', width: 16 },
]);
addRows(timeouts, [
  { componente: 'Hikari datasource', timeout: 'spring.datasource.hikari.connection-timeout', valor: '60000 ms', riesgo: 'Si SQL Server no responde, la conexion puede tardar hasta 60 s antes de fallar.', sugerencia: 'Mantener o bajar solo si hay monitoreo. Mostrar mensaje claro en front despues de 30-60 s.', prioridad: 'Media' },
  { componente: 'Hikari validation', timeout: 'spring.datasource.hikari.validation-timeout', valor: '5000 ms', riesgo: 'Validacion de conexion lenta puede degradar varias llamadas.', sugerencia: 'Loggear duracion y cantidad de conexiones invalidas.', prioridad: 'Media' },
  { componente: 'Transacciones Spring', timeout: 'spring.transaction.default-timeout', valor: '300 s', riesgo: 'Una transaccion larga puede bloquear inventario/venta por varios minutos.', sugerencia: 'Medir cada SP y separar validaciones fuera de la transaccion cuando sea posible.', prioridad: 'Alta' },
  { componente: 'Registrar venta OT', timeout: '@Transactional(timeout=300)', valor: '300 s', riesgo: 'El registro de venta puede quedar esperando mucho si SP se bloquea.', sugerencia: 'Agregar logs de inicio/fin por SP y correlacion por OT/idVenta.', prioridad: 'Alta' },
  { componente: 'Registrar detalle agenda', timeout: 'TransactionTemplate manual', valor: 'Sin timeout explicito observado', riesgo: 'Puede depender del default y no quedar claro para mantenimiento.', sugerencia: 'Definir timeout explicito alineado a 300 s o menor.', prioridad: 'Alta' },
  { componente: 'Axios/front', timeout: 'timeout HTTP', valor: 'No observado en otApi.ts', riesgo: 'El usuario queda esperando sin corte controlado si backend tarda.', sugerencia: 'Configurar timeout 60-90 s, cancelacion y mensaje de reintento seguro.', prioridad: 'Alta' },
  { componente: 'Reintentos front para retirados', timeout: 'Retry logico', valor: 'Reintenta limpiando serie/chip', riesgo: 'Puede ocultar regla de negocio y generar comportamiento distinto al backend.', sugerencia: 'Mover decision al backend y devolver codigo estructurado.', prioridad: 'Media' },
]);

const mejoras = addSheet('Mejoras', [
  { header: 'Prioridad', key: 'prioridad', width: 14 },
  { header: 'Mejora', key: 'mejora', width: 54 },
  { header: 'Descripcion', key: 'descripcion', width: 82 },
  { header: 'Impacto', key: 'impacto', width: 48 },
  { header: 'Esfuerzo', key: 'esfuerzo', width: 16 },
]);
addRows(mejoras, [
  { prioridad: 'Alta', mejora: 'Endpoint unico de estado OT para formulario', descripcion: 'Devolver estado funcional, accion permitida, motivo bloqueo, idVenta, idRuta, TieneDetalle, TieneDetalleEnCodigoVenta, HabilitarCargarMaterial y resumen de stock.', impacto: 'El front no infiere reglas y el sidebar/formulario se vuelve consistente.', esfuerzo: 'Medio' },
  { prioridad: 'Alta', mejora: 'Idempotencia al guardar materiales', descripcion: 'Enviar idempotencyKey por intento y rechazar duplicados del mismo formulario.', impacto: 'Evita doble carga por doble click, timeout o reintento.', esfuerzo: 'Medio' },
  { prioridad: 'Alta', mejora: 'Timeout HTTP explicito y UX de espera', descripcion: 'Configurar Axios con timeout y mostrar progreso por etapa: validando cierre, validando stock, guardando materiales.', impacto: 'Menos incertidumbre para el usuario y soporte.', esfuerzo: 'Bajo' },
  { prioridad: 'Alta', mejora: 'Timeout explicito en TransactionTemplate', descripcion: 'Configurar timeout para registrarDetalleAgenda y registrar duracion de la transaccion.', impacto: 'Evita bloqueos largos no controlados.', esfuerzo: 'Bajo' },
  { prioridad: 'Media', mejora: 'Batch validation para listas finalizadas/pendientes', descripcion: 'Evitar validar fila por fila con llamadas N+1; usar CTE enriquecida o SP batch.', impacto: 'Pantallas mas rapidas y menos carga SQL.', esfuerzo: 'Medio' },
  { prioridad: 'Media', mejora: 'Codigos de error estructurados', descripcion: 'Normalizar REGISTRO_BLOQUEADO, CUADRE_REGISTRADO, DETALLE_YA_REGISTRADO, STOCK_INSUFICIENTE, SERIE_INVALIDA.', impacto: 'Mensajes front claros y mantenibles.', esfuerzo: 'Bajo' },
  { prioridad: 'Media', mejora: 'Constantes para acciones de producto', descripcion: 'Reemplazar magic numbers de spx_RegMod_Productos: accion 3 y 35 por constantes documentadas.', impacto: 'Menor riesgo de errores al modificar el flujo.', esfuerzo: 'Bajo' },
  { prioridad: 'Media', mejora: 'Pruebas de reglas de estado', descripcion: 'Cubrir los 3 casos clave: finalizada sin detalle, finalizada con detalle, pendiente material.', impacto: 'Protege el nuevo formulario de regresiones.', esfuerzo: 'Bajo' },
  { prioridad: 'Baja', mejora: 'Alias o correccion de nombre de SP cabecera', descripcion: 'El nombre observado spx_ObtenerCaberaVentaParaRegistroOTwb parece tener typo Cabera.', impacto: 'Reduce confusion operativa.', esfuerzo: 'Bajo' },
]);

for (const ws of workbook.worksheets) {
  ws.eachRow((row) => {
    row.commit?.();
  });
  ws.columns.forEach((column) => {
    column.alignment = { wrapText: true, vertical: 'top' };
  });
}

resumen.getCell('A7').fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: palette.yellow } };
timeouts.eachRow((row, rowNumber) => {
  if (rowNumber <= 4) return;
  const priority = row.getCell(6).value;
  if (priority === 'Alta') {
    row.getCell(6).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'F4CCCC' } };
    row.getCell(6).font = { bold: true, color: { argb: palette.red } };
  }
});
mejoras.eachRow((row, rowNumber) => {
  if (rowNumber <= 1) return;
  const priority = row.getCell(1).value;
  const color = priority === 'Alta' ? 'F4CCCC' : priority === 'Media' ? 'FFF2CC' : 'D9EAD3';
  row.getCell(1).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: color } };
  row.getCell(1).font = { bold: true };
});

await workbook.xlsx.writeFile(outputPath);
console.log(outputPath);
