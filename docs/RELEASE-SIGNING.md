# Firma reproducible de Ghost WiFi Auditor (M6)

El almacén de firma de **Ghost WiFi Auditor** debe pertenecer únicamente a este proyecto; **no** usar la clave ni los secretos de SP-DECODE. No crear una nueva keystore por compilación.

## 1. Restaurar los secretos originales

En GitHub → Settings → Secrets and variables → Actions, registrar sin compartirlos en chats públicos:

- `GHOST_WIFI_KEYSTORE_B64`: contenido Base64 completo de la keystore original (P12/JKS).
- `GHOST_WIFI_STORE_PASSWORD`: contraseña del almacén.
- `GHOST_WIFI_KEY_ALIAS`: alias correcto de la firma.
- `GHOST_WIFI_KEY_PASSWORD`: contraseña de la clave.

Linux: `base64 -w0 /ruta/segura/ghost-wifi.p12`; macOS: `base64 < /ruta/segura/ghost-wifi.p12 | tr -d '\\n'`. Evitar shells con historial de comandos que registren secretos. **No subir el archivo P12, contraseñas o cadenas Base64 al repositorio.**

## 2. Fijar el certificado aprobado

Opcional, pero recomendado: consultar la huella SHA-256 del certificado público de la APK instalada desde **Ajustes → About → Identidad de la aplicación** o desde `apksigner verify --print-certs`. Fijar **solo la huella pública**, no la clave, en GitHub Actions **Variable** `GHOST_WIFI_EXPECTED_CERT_SHA256`.

El CI compara esta huella al generar una APK persistente y falla si el firmante no coincide. La huella debe corresponder a la keystore original del proyecto, no a una firma Debug efímera.

## 3. CI y versión instalada

- Sin secretos, el trabajo Debug usa la clave temporal del runner, verificará su firma y publicará `SHA256SUMS.txt`; **no constituye una release permanente**.
- Con los cuatro secretos, Debug y Release utilizan la misma keystore configurada.
- Un APK firmado con otro certificado no puede actualizar una instalación anterior de Android. Desinstalar borra los datos privados de esa aplicación, incluidas capturas importadas; exportar antes.
- El paso `signed-release` solo produce APK cuando estén todos los secretos. Que el trabajo aparezca verde con pasos omitidos **no significa** que haya una release firmada.
- Retener de forma privada una copia cifrada y una recuperación documentada de la keystore original. Perder la clave implica perder continuidad de actualización para instalaciones existentes.

## 4. Comprobaciones de salida antes de una release

1. Validar `mapping.txt` (R8), identidad de paquete y `versionCode`.
2. Verificar esquema APK v2/v3 con `apksigner verify --print-certs`.
3. Comparar SHA-256 de certificado con variable aprobada.
4. Verificar SHA-256 de APK contra `SHA256SUMS.txt`.
5. Instalar como actualización sobre la **release anterior firmada con esa misma clave**.
6. Ejecutar el plan de pruebas en dispositivos reales, asegurar retención del historial y no publicar `1.0.0` hasta cumplirlo.
