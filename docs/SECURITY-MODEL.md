# Modelo de seguridad y límites (M6)

## Alcance autorizado

Ghost WiFi Auditor se usa para inventario de hardware, diagnóstico Wi-Fi, importación y análisis de capturas autorizadas, y conexión SSH/SFTP a equipos Linux bajo control del operador.

La captura de tramas 802.11 no revela directamente una contraseña WPA/WPA2/WPA3. La implementación M6 **no** automatiza ataques WPS, desautenticaciones, fuerza bruta, extracción de contraseñas ni promueve estas acciones en redes ajenas.

## Superficies y mitigaciones

| Superficie | Control aplicado | Pendiente |
|---|---|---|
| Privilegios Android | Comandos `su -c id` e `iw list` de solo lectura; sin escalado automático | QA tras reinicio, permiso revocado, denegaciones |
| USB OTG | VID/PID y permiso explícito; detector no equivale a controlador operativo | Driver/firmware en hardware real |
| SSH | `known_hosts` validado, sin aceptar claves automáticamente | Pruebas con SSH real y rotación de claves |
| Comandos remotos | Lista fija list/capture/cleanup y validación de interfaz, duración e ID | Test de interrupción, timeout y permisos |
| SFTP | Archivo PCAP limitado a 32 MiB, ruta esperada y limpieza remota tras intentar importar | Pruebas en Linux real |
| Archivo PCAP | Máximo 128 MiB; parser C++ valida longitudes y enlaces | Fuzzing extensivo/QA con corpus real |
| Datos del usuario | Archivo privado, `allowBackup=false`, exports con consentimiento SAF | Restauración y cifrado de backup opcional |
| Informe | Identificadores omitidos por defecto en JSON/PDF | Validación visual y privacy QA |
| Claves SSH | Contraseña/frase sin `rememberSaveable`; clave privada efímera en caché | Limpieza tras cierre inesperado; pruebas de lifecycle |
| Firma APK | R8, `apksigner`, hash SHA-256, huella visible en About y pinning opcional en CI | Configurar keystore original en Secrets |

## Riesgos residuales

- Un PCAP original puede contener tramas, direcciones e información confidencial. Exportar el archivo original no lo anonimiza.
- El usuario puede seleccionar un servidor Linux mal configurado; la verificación de huella SSH debe hacerse por un canal confiable.
- Las pruebas unitarias y un build verde **no** prueban compatibilidad de monitor mode, integridad de un driver, funcionamiento USB ni estabilidad Android.
- Las referencias de dispositivos y adaptadores no son promesas de funcionamiento en Android stock.
- WPS, cifrado anunciado, canal y RSSI siguen `NOT_MEASURED` si no hay evidencia del analizador.
- Faltan pruebas de accesibilidad/contraste, actualización de firma y auditoría de dependencias de terceros.

## Reportar vulnerabilidades

Usar un canal de reporte privado al mantenedor del repositorio (por ejemplo GitHub Security Advisories si está habilitado), evitando publicar credenciales, PCAP ajenos o instrucciones de explotación activa.
