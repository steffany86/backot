using System;
using System.Collections.Generic;
using System.Data;
using System.Data.SqlClient;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Text;

namespace MigradorFotosSupervision
{
    /// <summary>
    /// Migra las fotos (base64 en columnas nvarchar(max)) de tbl_Supervision a archivos en disco
    /// y deja en la columna solo la ruta relativa:  yyyy/yyyyMMdd_NombreColumna_Id.jpg
    ///
    /// Por defecto SIMULA (no escribe nada). Usar --aplicar para escribir archivos y actualizar la BD.
    /// </summary>
    internal static class Program
    {
        private sealed class Opciones
        {
            public string Servidor = ".";
            public string Base = "bdControlOrdenes";
            public string Tabla = "tbl_Supervision_bak";
            public string Salida = @"D:\BDControlOrdenesImagenes\Supervision";
            public string Usuario;
            public string Clave;
            public string ColumnaClave;       // si no se indica, se autodetecta (Id / identity / Codigo)
            public int Limite = 0;            // 0 = todas las filas
            public bool Aplicar;
            public int MaxLado = 1280;
            public int Calidad = 75;
            public int MaxLadoAlto = 1600;    // niveles DOCSIS / medicion de ruido (hay que leer numeros)
            public int CalidadAlta = 85;
        }

        private sealed class Perfil
        {
            public int MaxLado;
            public int Calidad;
        }

        private static int Main(string[] args)
        {
            Opciones o;
            try { o = Parsear(args); }
            catch (ArgumentException ex)
            {
                Console.WriteLine(ex.Message);
                MostrarAyuda();
                return 1;
            }
            if (o == null) { MostrarAyuda(); return 0; }

            string cs = ConstruirCadena(o);
            Console.WriteLine("Modo        : " + (o.Aplicar ? "APLICAR (escribe archivos y actualiza la BD)" : "SIMULACION (no escribe nada)"));
            Console.WriteLine("Servidor/BD : " + o.Servidor + " / " + o.Base);
            Console.WriteLine("Tabla       : " + o.Tabla);
            Console.WriteLine("Salida      : " + o.Salida);

            try
            {
                using (var cn = new SqlConnection(cs))
                {
                    cn.Open();
                    return Ejecutar(cn, o);
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine("ERROR: " + ex.Message);
                return 2;
            }
        }

        private static int Ejecutar(SqlConnection cn, Opciones o)
        {
            string tabla = Ident(o.Tabla);
            List<string> columnasFoto = DescubrirColumnasFoto(cn, o.Tabla);
            if (columnasFoto.Count == 0)
            {
                Console.WriteLine("No se encontraron columnas Foto% nvarchar(max) en " + o.Tabla);
                return 3;
            }
            string claveCol = o.ColumnaClave ?? DetectarClave(cn, o.Tabla);
            bool tieneFecha = ExisteColumna(cn, o.Tabla, "FechaRegistro");
            Console.WriteLine("Columna clave: " + claveCol + (tieneFecha ? "  | fecha: FechaRegistro" : "  | sin FechaRegistro"));
            Console.WriteLine("Columnas foto: " + string.Join(", ", columnasFoto));
            Console.WriteLine();

            // Lista de filas (solo clave y fecha; las fotos se leen una por una)
            var filas = new List<KeyValuePair<string, DateTime?>>();
            string sqlFilas = "SELECT " + (o.Limite > 0 ? "TOP (" + o.Limite + ") " : "") +
                              Ident(claveCol) + ", " + (tieneFecha ? "FechaRegistro" : "CAST(NULL AS datetime)") +
                              " FROM " + tabla + " ORDER BY " + Ident(claveCol);
            using (var cmd = new SqlCommand(sqlFilas, cn))
            using (var rd = cmd.ExecuteReader())
            {
                while (rd.Read())
                {
                    string k = Convert.ToString(rd.GetValue(0), CultureInfo.InvariantCulture);
                    DateTime? f = rd.IsDBNull(1) ? (DateTime?)null : rd.GetDateTime(1);
                    filas.Add(new KeyValuePair<string, DateTime?>(k, f));
                }
            }
            Console.WriteLine("Filas a revisar: " + filas.Count);

            Directory.CreateDirectory(o.Salida);
            string logPath = Path.Combine(o.Salida, "migracion_log.csv");
            bool logNuevo = !File.Exists(logPath);

            long bytesOriginal = 0, bytesFinal = 0;
            int migradas = 0, omitidas = 0, errores = 0;

            using (var log = new StreamWriter(logPath, true, new UTF8Encoding(true)))
            {
                if (logNuevo) log.WriteLine("fecha_proceso;clave;columna;ruta;bytes_base64_utf16;bytes_archivo;estado");

                int n = 0;
                foreach (var fila in filas)
                {
                    n++;
                    foreach (string col in columnasFoto)
                    {
                        string valor;
                        using (var cmd = new SqlCommand(
                            "SELECT " + Ident(col) + " FROM " + tabla + " WHERE " + Ident(claveCol) + " = @k", cn))
                        {
                            cmd.CommandTimeout = 300;
                            cmd.Parameters.AddWithValue("@k", fila.Key);
                            object r = cmd.ExecuteScalar();
                            valor = (r == null || r is DBNull) ? null : (string)r;
                        }
                        if (string.IsNullOrWhiteSpace(valor)) continue;
                        if (!PareceBase64(valor)) { omitidas++; continue; } // vacia o ya migrada (ruta)

                        long origBytes = (long)valor.Length * 2;
                        try
                        {
                            byte[] raw = DecodificarBase64(valor);
                            Perfil p = PerfilDe(col, o);
                            byte[] jpg = ComprimirJpeg(raw, p);

                            string anio = fila.Value.HasValue ? fila.Value.Value.Year.ToString() : "SinFecha";
                            string fecha = fila.Value.HasValue ? fila.Value.Value.ToString("yyyyMMdd") : "SinFecha";
                            string nombre = fecha + "_" + col + "_" + Limpiar(fila.Key) + ".jpg";
                            string mes = fila.Value.HasValue ? fila.Value.Value.ToString("yyyyMM") : null;
                            string rel = mes == null ? anio + "/" + nombre : anio + "/" + mes + "/" + nombre;
                            string abs = mes == null ? Path.Combine(o.Salida, anio, nombre) : Path.Combine(o.Salida, anio, mes, nombre);

                            if (o.Aplicar)
                            {
                                Directory.CreateDirectory(Path.GetDirectoryName(abs));
                                File.WriteAllBytes(abs, jpg);
                                if (new FileInfo(abs).Length != jpg.Length)
                                    throw new IOException("El archivo escrito no coincide en tamano.");

                                using (var up = new SqlCommand(
                                    "UPDATE " + tabla + " SET " + Ident(col) + " = @p WHERE " + Ident(claveCol) + " = @k", cn))
                                {
                                    up.Parameters.Add("@p", SqlDbType.NVarChar, 300).Value = rel;
                                    up.Parameters.AddWithValue("@k", fila.Key);
                                    up.ExecuteNonQuery();
                                }
                            }

                            migradas++;
                            bytesOriginal += origBytes;
                            bytesFinal += jpg.Length;
                            log.WriteLine(string.Join(";", new[] {
                                DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"), fila.Key, col, rel,
                                origBytes.ToString(), jpg.Length.ToString(), o.Aplicar ? "OK" : "SIMULADO" }));
                        }
                        catch (Exception ex)
                        {
                            errores++;
                            Console.WriteLine("  ! " + fila.Key + " / " + col + ": " + ex.Message);
                            log.WriteLine(string.Join(";", new[] {
                                DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"), fila.Key, col, "",
                                origBytes.ToString(), "0", "ERROR: " + ex.Message.Replace(';', ',').Replace('\n', ' ') }));
                        }
                    }
                    if (n % 25 == 0 || n == filas.Count)
                    {
                        log.Flush();
                        Console.WriteLine("  fila " + n + "/" + filas.Count + " | fotos: " + migradas +
                                          " | " + Mb(bytesOriginal) + " MB -> " + Mb(bytesFinal) + " MB");
                    }
                }
            }

            Console.WriteLine();
            Console.WriteLine("===== RESUMEN (" + (o.Aplicar ? "APLICADO" : "SIMULACION") + ") =====");
            Console.WriteLine("Fotos procesadas : " + migradas);
            Console.WriteLine("Omitidas/ya ruta : " + omitidas);
            Console.WriteLine("Errores          : " + errores);
            Console.WriteLine("Tamano en BD     : " + Mb(bytesOriginal) + " MB");
            Console.WriteLine("Tamano archivos  : " + Mb(bytesFinal) + " MB");
            if (bytesOriginal > 0)
                Console.WriteLine("Reduccion        : " + (100.0 - 100.0 * bytesFinal / bytesOriginal).ToString("0.0", CultureInfo.InvariantCulture) + " %");
            Console.WriteLine("Log              : " + logPath);
            if (o.Aplicar)
                Console.WriteLine("\nSiguiente: ALTER TABLE ... REBUILD + DBCC SHRINKFILE para recuperar el espacio (ver SQL_POST_MIGRACION_FOTOS.sql).");
            return errores > 0 ? 4 : 0;
        }

        // ---------- Imagen ----------

        private static Perfil PerfilDe(string col, Opciones o)
        {
            bool alta = col.IndexOf("NivelesDocsis", StringComparison.OrdinalIgnoreCase) >= 0 ||
                        col.IndexOf("MedicionRuido", StringComparison.OrdinalIgnoreCase) >= 0;
            return alta
                ? new Perfil { MaxLado = o.MaxLadoAlto, Calidad = o.CalidadAlta }
                : new Perfil { MaxLado = o.MaxLado, Calidad = o.Calidad };
        }

        private static byte[] ComprimirJpeg(byte[] raw, Perfil p)
        {
            using (var ms = new MemoryStream(raw))
            using (var src = Image.FromStream(ms, false, false))
            {
                // Aplicar orientacion EXIF (fotos de celular) porque al recomprimir se pierde el EXIF
                try
                {
                    if (Array.IndexOf(src.PropertyIdList, 0x0112) >= 0)
                    {
                        int ori = src.GetPropertyItem(0x0112).Value[0];
                        switch (ori)
                        {
                            case 3: src.RotateFlip(RotateFlipType.Rotate180FlipNone); break;
                            case 6: src.RotateFlip(RotateFlipType.Rotate90FlipNone); break;
                            case 8: src.RotateFlip(RotateFlipType.Rotate270FlipNone); break;
                        }
                    }
                }
                catch { /* sin EXIF legible: se deja como esta */ }

                int w = src.Width, h = src.Height;
                double escala = Math.Min(1.0, (double)p.MaxLado / Math.Max(w, h));
                int nw = Math.Max(1, (int)Math.Round(w * escala));
                int nh = Math.Max(1, (int)Math.Round(h * escala));

                using (var bmp = new Bitmap(nw, nh, PixelFormat.Format24bppRgb))
                {
                    using (var g = Graphics.FromImage(bmp))
                    {
                        g.Clear(Color.White); // PNG con transparencia -> fondo blanco
                        g.InterpolationMode = InterpolationMode.HighQualityBicubic;
                        g.SmoothingMode = SmoothingMode.HighQuality;
                        g.PixelOffsetMode = PixelOffsetMode.HighQuality;
                        g.DrawImage(src, 0, 0, nw, nh);
                    }
                    ImageCodecInfo jpgCodec = ImageCodecInfo.GetImageEncoders().First(c => c.FormatID == ImageFormat.Jpeg.Guid);
                    using (var prms = new EncoderParameters(1))
                    using (var outMs = new MemoryStream())
                    {
                        prms.Param[0] = new EncoderParameter(System.Drawing.Imaging.Encoder.Quality, (long)p.Calidad);
                        bmp.Save(outMs, jpgCodec, prms);
                        byte[] resultado = outMs.ToArray();
                        // Si ya era un JPEG pequeno y la recompresion lo agranda, conservar el original
                        if (resultado.Length >= raw.Length && EsJpeg(raw) && escala >= 1.0) return raw;
                        return resultado;
                    }
                }
            }
        }

        private static bool EsJpeg(byte[] b)
        {
            return b.Length > 3 && b[0] == 0xFF && b[1] == 0xD8;
        }

        // ---------- Base64 ----------

        private static bool PareceBase64(string v)
        {
            string t = v.TrimStart();
            if (t.StartsWith("data:", StringComparison.OrdinalIgnoreCase)) return true;
            return t.Length > 1000 && t.IndexOf('/') != 0 && t.IndexOf('\\') < 0 && t.IndexOf(' ') < 0;
        }

        private static byte[] DecodificarBase64(string v)
        {
            string t = v.Trim();
            int i = t.IndexOf(";base64,", StringComparison.OrdinalIgnoreCase);
            if (t.StartsWith("data:", StringComparison.OrdinalIgnoreCase) && i > 0)
                t = t.Substring(i + 8);
            t = t.Replace("\r", "").Replace("\n", "").Replace(" ", "");
            return Convert.FromBase64String(t);
        }

        // ---------- Esquema ----------

        private static List<string> DescubrirColumnasFoto(SqlConnection cn, string tabla)
        {
            var res = new List<string>();
            const string sql = @"SELECT c.name FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
                                 WHERE c.object_id = OBJECT_ID(@t) AND c.name LIKE 'Foto%'
                                   AND t.name IN ('nvarchar','varchar') AND c.max_length = -1
                                 ORDER BY c.column_id";
            using (var cmd = new SqlCommand(sql, cn))
            {
                cmd.Parameters.AddWithValue("@t", "dbo." + tabla);
                using (var rd = cmd.ExecuteReader())
                    while (rd.Read()) res.Add(rd.GetString(0));
            }
            return res;
        }

        private static string DetectarClave(SqlConnection cn, string tabla)
        {
            const string sql = @"SELECT TOP 1 c.name FROM sys.columns c
                                 WHERE c.object_id = OBJECT_ID(@t)
                                   AND (c.is_identity = 1 OR c.name IN ('Id','ID','id','IdSupervision'))
                                 ORDER BY c.is_identity DESC, c.column_id";
            using (var cmd = new SqlCommand(sql, cn))
            {
                cmd.Parameters.AddWithValue("@t", "dbo." + tabla);
                object r = cmd.ExecuteScalar();
                if (r != null && !(r is DBNull)) return (string)r;
            }
            return "Codigo";
        }

        private static bool ExisteColumna(SqlConnection cn, string tabla, string col)
        {
            using (var cmd = new SqlCommand("SELECT COL_LENGTH(@t, @c)", cn))
            {
                cmd.Parameters.AddWithValue("@t", "dbo." + tabla);
                cmd.Parameters.AddWithValue("@c", col);
                object r = cmd.ExecuteScalar();
                return r != null && !(r is DBNull);
            }
        }

        // ---------- Utilidades ----------

        private static string Ident(string name)
        {
            return "[" + name.Replace("]", "]]") + "]";
        }

        private static string Limpiar(string s)
        {
            var sb = new StringBuilder();
            foreach (char c in s.Trim())
                sb.Append(char.IsLetterOrDigit(c) || c == '-' ? c : '_');
            return sb.ToString();
        }

        private static string Mb(long bytes)
        {
            return (bytes / 1048576.0).ToString("0.0", CultureInfo.InvariantCulture);
        }

        private static string ConstruirCadena(Opciones o)
        {
            var b = new SqlConnectionStringBuilder
            {
                DataSource = o.Servidor,
                InitialCatalog = o.Base,
                ConnectTimeout = 30
            };
            if (string.IsNullOrEmpty(o.Usuario)) b.IntegratedSecurity = true;
            else { b.UserID = o.Usuario; b.Password = o.Clave; }
            return b.ConnectionString;
        }

        private static Opciones Parsear(string[] args)
        {
            var o = new Opciones();
            for (int i = 0; i < args.Length; i++)
            {
                string a = args[i].ToLowerInvariant();
                Func<string> val = () =>
                {
                    if (i + 1 >= args.Length) throw new ArgumentException("Falta el valor de " + args[i]);
                    return args[++i];
                };
                switch (a)
                {
                    case "--ayuda": case "-h": case "/?": return null;
                    case "--servidor": o.Servidor = val(); break;
                    case "--base": o.Base = val(); break;
                    case "--tabla": o.Tabla = val(); break;
                    case "--salida": o.Salida = val(); break;
                    case "--usuario": o.Usuario = val(); break;
                    case "--clave": o.Clave = val(); break;
                    case "--columna-clave": o.ColumnaClave = val(); break;
                    case "--limite": o.Limite = int.Parse(val()); break;
                    case "--max-lado": o.MaxLado = int.Parse(val()); break;
                    case "--calidad": o.Calidad = int.Parse(val()); break;
                    case "--aplicar": o.Aplicar = true; break;
                    default: throw new ArgumentException("Argumento desconocido: " + args[i]);
                }
            }
            return o;
        }

        private static void MostrarAyuda()
        {
            Console.WriteLine(@"
MigradorFotosSupervision - pasa fotos base64 de tbl_Supervision a archivos JPG

  --servidor   (def: .)                    --base      (def: bdControlOrdenes)
  --tabla      (def: tbl_Supervision_bak)  --salida    (def: D:\BDControlOrdenesImagenes\Supervision)
  --usuario / --clave   (sin esto usa autenticacion de Windows)
  --columna-clave       (def: autodetecta Id / identity / Codigo)
  --limite N            solo las primeras N filas (para probar)
  --max-lado 1280  --calidad 75
  --aplicar             ESCRIBE archivos y actualiza la BD (sin esto solo simula)
");
        }
    }
}
