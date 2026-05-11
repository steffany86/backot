-- SP nuevo para registro de OT (cabecera)
-- Ejecutar en la BD de la sucursal donde se registran las OT.

IF OBJECT_ID('dbo.spx_RegistrarOrdenTrabajo', 'P') IS NULL
BEGIN
    EXEC('CREATE PROC dbo.spx_RegistrarOrdenTrabajo AS SELECT 1 AS placeholder;');
END
GO

-- SP para listar OT finalizadas por fecha y vendedores (versionable por BD)
IF OBJECT_ID('dbo.spx_ListarOtFinalizadas', 'P') IS NULL
BEGIN
    EXEC('CREATE PROC dbo.spx_ListarOtFinalizadas AS SELECT 1 AS placeholder;');
END
GO

ALTER PROC dbo.spx_ListarOtFinalizadas
    @Fecha DATETIME,
    @IdUsuario INT = NULL,
    @IdsVendedorCsv NVARCHAR(MAX) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    IF @Fecha IS NULL
    BEGIN
        RAISERROR('Fecha es requerida.', 16, 1);
        RETURN;
    END

    DECLARE @Vendedores TABLE (Id_Vendedor INT PRIMARY KEY);

    IF @IdsVendedorCsv IS NOT NULL AND LTRIM(RTRIM(@IdsVendedorCsv)) <> ''
    BEGIN
        DECLARE @xml XML;
        DECLARE @csv NVARCHAR(MAX);
        SET @csv = REPLACE(LTRIM(RTRIM(@IdsVendedorCsv)), ' ', '');
        SET @xml = CAST('<x><i>' + REPLACE(@csv, ',', '</i><i>') + '</i></x>' AS XML);

        INSERT INTO @Vendedores (Id_Vendedor)
        SELECT DISTINCT CAST(T.N.value('.', 'NVARCHAR(50)') AS INT)
        FROM @xml.nodes('/x/i') AS T(N)
        WHERE ISNUMERIC(T.N.value('.', 'NVARCHAR(50)')) = 1
          AND CAST(T.N.value('.', 'NVARCHAR(50)') AS INT) > 0;
    END

    IF @IdUsuario IS NOT NULL AND @IdUsuario > 0
       AND NOT EXISTS (SELECT 1 FROM @Vendedores WHERE Id_Vendedor = @IdUsuario)
    BEGIN
        INSERT INTO @Vendedores (Id_Vendedor) VALUES (@IdUsuario);
    END

    SELECT
        v.Id_Venta AS idVenta,
        v.OrdenTrabajo AS ordenTrabajo,
        v.CodigoCliente AS codigoCliente,
        v.Fecha_Ejecucion AS fechaEjecucion,
        v.Origen AS origen,
        v.Id_Vendedor AS idVendedor,
        v.Id_TipoServicio AS idTipoServicio,
        ts.Nombre AS tipoServicio,
        v.Id_Estado AS idEstado,
        e.Nombre AS estado
    FROM dbo.tbl_Venta v
    LEFT JOIN dbo.tbl_tiposervicio ts ON ts.Id_TipoServicio = v.Id_TipoServicio
    LEFT JOIN dbo.tbl_estado e ON e.Id_Estado = v.Id_Estado
    WHERE ISNULL(v.E_Eliminado, 0) = 0
      AND CONVERT(DATE, v.Fecha_Ejecucion) = @Fecha
      AND EXISTS (
            SELECT 1
            FROM @Vendedores x
            WHERE x.Id_Vendedor = v.Id_Vendedor
      )
    ORDER BY v.Id_Venta DESC;
END
GO

ALTER PROC dbo.spx_RegistrarOrdenTrabajo
    @Id_Usuario INT,
    @Id_Ruta INT,
    @Id_TipoServicio INT,
    @CodigoCliente INT = NULL,
    @Id_Estado INT = NULL,
    @Observacion NVARCHAR(255) = NULL,
    @TieneObservacion BIT = 0,
    @Id_Sucursal INT = NULL,
    @NombreCliente NVARCHAR(200) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @Id_Usuario IS NULL OR @Id_Ruta IS NULL OR @Id_TipoServicio IS NULL
    BEGIN
        RAISERROR('Parametros requeridos faltantes.', 16, 1);
        RETURN;
    END

    IF @TieneObservacion = 1 AND ( @Observacion IS NULL OR LTRIM(RTRIM(@Observacion)) = '' )
    BEGIN
        RAISERROR('Observacion requerida cuando TieneObservacion=1.', 16, 1);
        RETURN;
    END

    DECLARE @Id_Vendedor INT;
    SELECT @Id_Vendedor = Id_Vendedor
    FROM dbo.tbl_Ruta
    WHERE Id_Ruta = @Id_Ruta AND E_Eliminado = 0;

    IF @Id_Vendedor IS NULL
    BEGIN
        RAISERROR('Ruta no valida.', 16, 1);
        RETURN;
    END

    DECLARE @OrdenTrabajo INT;

    BEGIN TRAN;
        -- Bloqueo pesimista: evita que dos sesiones tomen el mismo OrdenTrabajo
        SELECT @OrdenTrabajo = ISNULL(MAX(OrdenTrabajo), 0) + 1
        FROM dbo.tbl_Venta WITH (TABLOCKX, HOLDLOCK);

        INSERT INTO dbo.tbl_Venta (
            Id_Usuario,
            Id_Vendedor,
            Id_Ruta,
            Id_TipoServicio,
            Fecha_Ejecucion,
            Fecha_Registro,
            OrdenTrabajo,
            Observacion,
            Total,
            Id_UsuarioE,
            E_Eliminado,
            Nombre,
            Id_Estado,
            Id_Sucursal,
            CodigoCliente,
            TieneObservacion
        )
        VALUES (
            @Id_Usuario,
            @Id_Vendedor,
            @Id_Ruta,
            @Id_TipoServicio,
            GETDATE(),
            GETDATE(),
            @OrdenTrabajo,
            @Observacion,
            NULL,
            NULL,
            0,
            @NombreCliente,
            @Id_Estado,
            @Id_Sucursal,
            @CodigoCliente,
            @TieneObservacion
        );
    COMMIT TRAN;

    SELECT CAST(SCOPE_IDENTITY() AS INT) AS Id_Venta, @OrdenTrabajo AS OrdenTrabajo;
END
GO
