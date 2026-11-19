# Ghost WiFi Auditor — M6 QA y seguridad (v0.7.0-alpha)

M6 es la fase de **endurecimiento y preparación de QA**, no prueba certificada de hardware ni release estable.

## Automatización en CI

- Comprobación Python de configuraciones Android, SSH, almacenamiento y reportes.
- Pruebas Kotlin/JUnit de validación del host SSH, interfaz, duración, ruta SFTP y límites de captura.
- Pruebas existentes de parser root, monitor, USB y búsqueda de historial.
- Python tests del agente Linux.
- C++ PCAP/PCAPNG con casos válidos/corruptos, entradas aleatorias deterministas y `-fsanitize=undefined`.
- Android Lint y compilación de pruebas instrumentadas Compose (las pruebas instrumentadas requieren un emulador/teléfono para ejecutarse).
- `assembleDebug` con R8, verificación del APK con `apksigner`, SHA256SUMS y comprobación opcional del certificado aprobado.
- Release firmada **solo** si existen los cuatro secretos privados de Ghost WiFi Auditor.

## Plan manual final: teléfonos reales

| Equipo | Áreas críticas | Estado |
|---|---|---|
| Redmi 14C / HyperOS 3 | insets, root temporal, revocación, Wi-Fi interno sin monitor, PCAP import/export | PENDIENTE |
| Samsung Android | interfaz, estado root, USB Host, lifecycle, permisos y backup | PENDIENTE |
| Honor Android | accesibilidad, cambio de idioma, soporte USB OTG y permisos | PENDIENTE |
| Otro teléfono Android | root persistente o sin root, compatibilidad y degradación segura | PENDIENTE |
| Linux + adaptador compatible | SSH/host key, `iw`, tcpdump, SFTP, captura limitada y limpieza | PENDIENTE |

## Pruebas de aceptación funcional

1. Instalación y actualización APK sin pérdida de datos cuando el certificado es idéntico.
2. Root ausente/temporal/persistente, `su` denegado, root revocado y reinicio.
3. Escaneo USB y permisos: conectar, desconectar, fabricante desconocido, dispositivo sin controlador y hub OTG con energía.
4. Importación PCAP clásico y PCAPNG, corpus malformado, límites 128/256 MiB, borrado, exportaciones JSON/PDF originales o redactadas.
5. Sesiones SSH: host no verificado, host cambiado, contraseña errónea, clave privada y modo sin conexión activado.
6. Captura remota solo con interfaz monitor real y autorización; SFTP válido, fallo parcial, interrupciones y limpieza.
7. Pantallas: no solapar reloj/batería ni gestos, sin componentes fuera de escala, modo claro/grafito/AMOLED, español/inglés.
8. Huella SHA-256 en About coincide con `apksigner`; hash de APK coincide con sidecar de CI.
9. No enviar credenciales de SSH, claves privadas ni dumps de paquetes por telemetría.
10. Un teléfono con root o adaptador no compatible muestra explícitamente **no verificado/no disponible**.

## Resultados

**CI:** pendiente de verificarse tras integrar el código M6.
**Pruebas físicas:** aún no se ejecutaron; se harán al disponer del adaptador y de los teléfonos.
**Firma permanente:** requiere secretos originales; no se generará otra keystore.
