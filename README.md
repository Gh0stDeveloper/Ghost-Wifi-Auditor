<p align="center">
  <img src="assets/ghost-dragon.svg" alt="Ghost WiFi Auditor — dragón original" width="148" />
</p>

<h1 align="center">Ghost WiFi Auditor</h1>

<p align="center"><b>Android Wireless Security & Hardware Diagnostics</b><br/>
Diagnóstico inalámbrico, compatibilidad de adaptadores y análisis de capturas para auditorías autorizadas.</p>

<p align="center">
  <a href="https://github.com/Gh0stDeveloper/Ghost-Wifi-Auditor/actions"><img alt="Android CI" src="https://img.shields.io/github/actions/workflow/status/Gh0stDeveloper/Ghost-Wifi-Auditor/android.yml?label=Android%20CI&logo=githubactions"/></a>
  <img alt="Development stage" src="https://img.shields.io/badge/estado-alpha%200.5.0-F0AD4E"/>
  <img alt="Android" src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white"/>
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white"/>
  <a href="https://t.me/GhostDeve"><img alt="Telegram" src="https://img.shields.io/badge/Telegram-Canal%20oficial-26A5E4?logo=telegram&logoColor=white"/></a>
</p>

> [!IMPORTANT]
> **Estado:** proyecto Android en fase **alpha / diagnóstico (M1)**. El código de la interfaz y las comprobaciones de hardware son una base de desarrollo. No se debe afirmar que la app ya permite capturar paquetes, activar modo monitor en cualquier teléfono, obtener contraseñas o instalar controladores automáticamente. Las funciones dependen del modelo, *kernel*, *firmware*, chipset y permisos.

## Hito M4 — Linux remoto con SSH y SFTP (0.5.0-alpha)

La pantalla **Auditoría → Linux remoto** incluye integración real de red usando [SSHJ](https://github.com/hierynomus/sshj) 0.41.2:

- SSH cifrado con **host key pinning obligatorio** mediante una entrada OpenSSH `known_hosts` que debe obtenerse por un canal confiable. No se permite aceptar claves automáticamente.
- Autenticación por contraseña o archivo de clave privada, con frase de cifrado opcional. Las credenciales no se guardan en preferencias y los archivos temporales se eliminan después de cada operación.
- Diagnóstico real con el agente Linux `ghost-wifi-agent`: versión kernel, hostname, interfaces `iw`, tipo de interfaz y disponibilidad de `tcpdump`.
- Captura pasiva limitada a interfaces **ya configuradas en modo monitor** en el servidor; duración elegida de 5, 10, 15 o 30 segundos, máximo 500 paquetes y 32 MiB. El usuario debe confirmar explícitamente la autorización.
- Descarga cifrada por **SFTP**, inspección y almacenamiento local en el historial PCAP del hito M3, y limpieza del fichero remoto después de una importación exitosa.
- El motor Linux no interpreta comandos arbitrarios de la aplicación. El agente Linux (Python estándar) valida nombres de interfaces, IDs y parámetros y ejecuta `tcpdump` sin usar `shell=True`.
- Las capturas Wi-Fi del propio Android o de un adaptador OTG siguen sin implementarse: no se confunde detección de hardware con una interfaz monitor operativa.

**Instalación y configuración Linux:** [linux/README.md](linux/README.md).  
**Pruebas de aceptación pendientes:** SSH real, fingerprints, login por clave, diagnóstico, captura de una interfaz monitor y transferencia SFTP. Solo se podrá verificar cuando exista un equipo Linux compatible; las pruebas automáticas no reemplazan hardware real.

## Hito M3 — Capturas offline y análisis nativo (0.4.0-alpha)

- **Android 15/16 — barras del sistema:** se aplica `safeDrawingPadding()` al `Scaffold` para evitar que encabezado y barra inferior queden debajo de la hora, batería o navegación del sistema.
- **Equipos:** eliminado el formulario “Otro dispositivo no listado” que guardaba modelos manualmente. Se conserva la búsqueda de equipos documentados y el diagnóstico automático del dispositivo.
- **Auditoría:** importación mediante Android Storage Access Framework, sin permisos de almacenamiento generales. PCAP y PCAPNG se copian a un área privada antes de analizarlos.
- **Motor C++17/JNI:** valida cabeceras y registros PCAP, bloques SHB/IDB/EPB/PB/SPB de PCAPNG, tamaños, límites y enlaces. Cuenta paquetes Ethernet y categorías 802.11 de gestión, control y datos. No extrae claves ni publica identificadores MAC.
- **Sesiones offline:** historial privado de hasta 20 archivos (máximo 128 MiB por archivo, 256 MiB en total), detalles, eliminación y exportación explícita de JSON de resumen y captura original mediante SAF.
- **QA:** tests nativos con PCAP/PCAPNG sintéticos válidos y corruptos; se ejecutan antes de la compilación Android. Debug y release mantienen R8.
- **Seguridad de hardware:** la pantalla deja explícito que la **captura en vivo no está implementada ni validada**. Aún no se inicia ninguna captura en interfaz interna, USB OTG o SSH sin un backend real y hardware compatible. Detectar USB/root no desbloquea falsamente el botón.

**Prueba física pendiente:** revisar visualmente la corrección de insets en Xiaomi/Samsung/Honor y probar con capturas exportadas de un entorno autorizado. El modo monitor real y las sesiones en vivo siguen condicionados a hardware/controladores y a la fase de integración correspondiente.


### Estado comprobado de CI de M3

- **Pruebas nativas C++:** aprobadas, incluyendo PCAP/PCAPNG válido y truncado.
- **Pruebas unitarias Android:** aprobadas.
- **Build Debug R8:** aprobado; archivo `mapping.txt` verificado.
- **Artefacto APK Debug:** `ghost-wifi-debug` (GitHub Actions [GitHub Actions #44567816041](https://github.com/Gh0stDeveloper/Ghost-Wifi-Auditor/actions/runs/44567816041), commit `cda2761`).
- **Firma persistente:** no disponible en GitHub Actions; la release con keystore original no se generó. El Debug del runner puede no actualizar versiones firmadas con otro certificado.
- **Validación Android 16 en dispositivo:** pendiente; la corrección de insets está implementada, no aún verificada visualmente en el teléfono.

## Hito M2 — USB OTG y hardware externo (0.3.0-alpha)

- Escaneo de dispositivos USB conectados y actualizaciones al conectar/desconectar, desde la pantalla **Equipos**.
- Solicitud **explícita** de permisos USB mediante PendingIntent y un receiver privado no exportado.
- Catálogo local de pares **VID:PID** (AR9271, MT7612U y referencias adicionales) en `app/src/main/assets/usb_adapters.v1.json`.
- Separación visible entre dispositivo detectado, permiso de Android, módulo Linux potencialmente cargado y compatibilidad real de captura; nunca se confunden esos estados.
- Guía de energía OTG, hub alimentado, firmware y controlador, incluyendo USB desconocidos.
- Pruebas unitarias de emparejamiento exacto y estado conservador de módulos; el Debug ahora también usa **R8**.
- La firma persistente puede utilizarse en CI si están configurados los secretos privados `GHOST_WIFI_*`. Sin esos secretos, el Debug del runner se firma temporalmente y **no garantiza actualizar una APK anterior**.

**Pendiente:** pruebas con adaptadores físicos, creación de una interfaz monitor válida, capturas y motor SSH. USB Host no instala drivers por sí mismo.

## Hito M1 — Diagnóstico estructurado (0.2.0-alpha)

- Estados definidos: **SUPPORTED / UNSUPPORTED / UNKNOWN / ERROR**, siempre con evidencia y hora de observación.
- Consulta de superusuario bajo demanda; el estado nunca se reutiliza después de reiniciar o cerrar la aplicación.
- El modo monitor se evalúa **solo** desde la sección `Supported interface modes` de `iw list`. «Anunciado» **no** significa «captura operativa».
- Detalles locales: versión Android, build, kernel, controlador WLAN y firmware, sin recolectar MAC/IMEI/serial.
- Catálogo de compatibilidad versionado en `app/src/main/assets/device_compatibility.v1.json`, con requisitos y fuentes. Los equipos no listados se consideran **no verificados**.
- Perfil de «otro dispositivo» guardado únicamente en preferencias locales.
- Pruebas unitarias del parser de root/monitor con JUnit, integradas en GitHub Actions.

**CI verificada:** las pruebas unitarias y `assembleDebug` finalizaron correctamente en [GitHub Actions #44558588666](https://github.com/Gh0stDeveloper/Ghost-Wifi-Auditor/actions/runs/44558588666) (commit `539ffe2`). El artefacto `ghost-wifi-debug` es Debug, **no** una release con firma permanente. 

**Pendiente de validación:** instalación de la APK en teléfonos reales y pruebas de las diferencias entre controladores/firmware. El soporte funcional de modo monitor, USB Wi-Fi y Linux remoto sigue pendiente.


### Resultado de CI M2 (19 de noviembre de 2026)

- [GitHub Actions #44563903907](https://github.com/Gh0stDeveloper/Ghost-Wifi-Auditor/actions/runs/44563903907): pruebas unitarias, compilación **R8-minified Debug** y comprobación del archivo `mapping.txt`: **correctas**.
- APK Debug de M2: artefacto `ghost-wifi-debug`, commit `5406aaa`.
- **Firma:** no se configuraron aún los secretos privados de firma de Ghost WiFi Auditor. Por lo tanto, el trabajo de release firmada se omitió; el estado verde del job no significa que se produjera una release.
- **Actualizaciones:** comprobación de certificados APK v2 mostró **certificados Debug distintos** para M1 y M2; Android rechazará instalar M2 sobre M1 como actualización. Desinstalar M1 permitiría probar el Debug M2, pero elimina los datos locales. Preferir en el futuro una compilación persistente firmada con la keystore original.
- No se ha realizado todavía una prueba física con un adaptador USB: la enumeración, el permiso y la detección de chipsets **deben validarse con hardware real**.

#### Configuración privada de firma (sin subir claves al repositorio)

En **Settings → Secrets and variables → Actions** configura los cuatro secretos, con la keystore **original** de Ghost WiFi Auditor: `GHOST_WIFI_KEYSTORE_B64`, `GHOST_WIFI_STORE_PASSWORD`, `GHOST_WIFI_KEY_ALIAS` y `GHOST_WIFI_KEY_PASSWORD`. Una vez configurados, CI podrá compilar Debug y release con la **misma** identidad de firma. Los valores secretos y el archivo `.p12` no pertenecen al repositorio público.

## Vista general

**Ghost WiFi Auditor** es una aplicación Android con diseño AMOLED grafito, acentos verdes y azules e identidad de dragón geométrico original. Está diseñada para funcionar como centro de diagnóstico y, progresivamente, como interfaz para herramientas de auditoría inalámbrica **en redes propias o con autorización expresa**.

- **Wi-Fi interno:** identifica las capacidades que anuncia el controlador; no fuerza funciones incompatibles.
- **USB OTG:** enumera adaptadores, identifica **VID/PID** y permite planificar soporte de múltiples chipsets.
- **Linux remoto:** cliente SSH/SFTP y agente controlado para diagnóstico y captura pasiva con interfaces monitor reales (pendiente de pruebas físicas).
- **PCAP/PCAPNG:** analizador nativo de bloques y categorías de trama, con historial y exportación de informes; no captura en vivo.
- **Root:** verificación bajo demanda de superusuario, compatible con enfoques de root temporal o persistente sin conceder permisos automáticamente.

## Herramientas y tecnologías

<p>
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose"/>
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android"/>
  <img src="https://img.shields.io/badge/C%2B%2B-00599C?style=for-the-badge&logo=cplusplus&logoColor=white" alt="C++"/>
  <img src="https://img.shields.io/badge/CMake-064F8C?style=for-the-badge&logo=cmake&logoColor=white" alt="CMake"/>
  <img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white" alt="Gradle"/>
  <img src="https://img.shields.io/badge/GitHub%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white" alt="GitHub Actions"/>
  <img src="https://img.shields.io/badge/Linux-FCC624?style=for-the-badge&logo=linux&logoColor=black" alt="Linux"/>
</p>

| Capa | Tecnología | Uso |
|---|---|---|
| Interfaz | Kotlin + Jetpack Compose / Material 3 | Pantallas, navegación y preferencias |
| Plataforma | Android SDK, USB Host API | Identificación y enumeración de dispositivos |
| Diagnóstico local | Kotlin + comandos de solo lectura | Root y capacidades Wi-Fi anunciadas |
| Motor nativo | C++17, JNI y CMake | Reconocimiento preliminar de PCAP/PCAPNG |
| Imágenes de referencia | Coil | Fichas visuales de adaptadores, descargadas bajo demanda |
| Compilación | Gradle, Android Gradle Plugin, R8 | Debug y release |
| Automatización | GitHub Actions | Compilaciones y artefactos CI |
| Integración futura | SSH + agente Linux | Control remoto autenticado (pendiente) |

## Pantallas

| Pantalla | Propósito |
|---|---|
| **Inicio** | Estado general del dispositivo, root, monitor y USB |
| **Auditoría** | Elegir entre Wi-Fi interno, adaptador USB OTG y Linux remoto |
| **Equipos** | Catálogo de dispositivos y soporte condicionado por firmware/kernel |
| **Ajustes** | Método de root, adaptadores recomendados y preferencias de seguridad |
| **Ajustes → About** | Créditos de Ghost Developer, comunidad y enlace al código fuente |

## Compatibilidad de dispositivos

La compatibilidad **no es una propiedad fija del nombre comercial del teléfono**; depende de variante/SKU, versión Android, kernel, firmware WLAN, controlador y configuración. Algunas soluciones usan firmware modificado y pueden dejar de funcionar tras una actualización.

| Dispositivo / familia | Modo monitor interno | Condición |
|---|---|---|
| Google Nexus 5 | Documentado con modificaciones | Firmware/implementación Nexmon específica |
| Samsung Galaxy S10 (variantes compatibles) | Documentado en entornos específicos | Kernel/ROM NetHunter compatibles; **no** asumir soporte stock |
| OnePlus 7 / 7 Pro / 7T / 7T Pro | Documentado en determinadas configuraciones | NetHunter y ROM/kernel correspondientes |
| Xiaomi Redmi 14C, MediaTek Gen4m | No disponible mediante el controlador estándar **probado** | No anuncia `monitor`; creación `mon0` devuelve `EOPNOTSUPP (-95)` |
| Otros Android (Samsung, Xiaomi, Honor, etc.) | **Sin verificar** | Ejecutar diagnóstico por modelo; valorar USB OTG o Linux |

Fuentes y matrices: [Kali NetHunter — Wireless Cards](https://www.kali.org/docs/nethunter/wireless-cards/) · [NetHunter kernels](https://nethunter.kali.org/kernels) · [Kali Linux](https://www.kali.org/).

## Adaptadores Wi-Fi recomendados para investigar

> [!CAUTION]
> **Recomendado para evaluación no significa compatible con todos los Android.** Para usar un adaptador externo se necesitan soporte USB Host/OTG, energía suficiente, firmware y controlador del chipset compilado para el kernel. Algunos adaptadores cambian de chipset entre revisiones.

| Modelo | Chipset | Bandas | Nota |
|---|---|---|---|
| [ALFA AWUS036ACM](https://www.alfa.com.tw/products/awus036acm) | MediaTek MT7612U | 2.4 / 5 GHz | Buena referencia para Linux; confirmar `mt76` en el kernel del dispositivo |
| [ALFA AWUS036NHA](https://www.alfa.com.tw/products/awus036nha) | Atheros AR9271 | 2.4 GHz | Requiere `ath9k_htc` y firmware |
| [TP-Link TL-WN722N **v1**](https://www.tp-link.com/mx/home-networking/adapter/tl-wn722n/v1/) | Atheros AR9271 | 2.4 GHz | Verificar físicamente la revisión **v1** |
| [ALFA AWUS036ACH](https://www.alfa.com.tw/products/awus036ach_1) | Realtek RTL8812AU | 2.4 / 5 GHz | Puede requerir controlador adicional |
| Otros | Por VID:PID/chipset | Variable | Verificación individual, sin lista cerrada |

La app presenta fotografías de referencia con crédito al proveedor de la imagen y enlace al producto; las imágenes no certifican chipsets de unidades individuales.

## Estructura del proyecto

```text
Ghost-Wifi-Auditor/
├── app/src/main/assets/device_compatibility.v1.json
├── app/src/main/java/com/ghostdeveloper/ghostwifiauditor/DiagnosticModels.kt
├── app/src/main/java/com/ghostdeveloper/ghostwifiauditor/DeviceCatalog.kt
├── app/src/test/java/com/ghostdeveloper/ghostwifiauditor/DiagnosticParsersTest.kt
├── assets/ghost-dragon.svg
├── app/
│   ├── src/main/java/com/ghostdeveloper/ghostwifiauditor/
│   │   ├── MainActivity.kt
│   │   ├── HardwareInspector.kt
│   │   └── NativePcap.kt
│   ├── src/main/cpp/
│   │   ├── pcap_inspector.cpp
│   │   └── CMakeLists.txt
│   └── src/main/res/drawable/ic_dragon.xml
├── docs/ROADMAP.md
├── .github/workflows/android.yml
└── README.md
```

## Compilar desde Android Studio

**Requisitos previstos:** Android Studio compatible con AGP **8.7.3**, JDK **17**, Kotlin **2.0.21**, Android SDK **35**, NDK y CMake.

1. Clona el repositorio: `git clone https://github.com/Gh0stDeveloper/Ghost-Wifi-Auditor.git`.
2. Abre la carpeta raíz en Android Studio y sincroniza Gradle.
3. Instala los componentes SDK/NDK/CMake que solicite Android Studio.
4. Compila `assembleDebug` mediante Gradle/Android Studio. La compatibilidad de compilación y ejecución se validará en CI y en dispositivos físicos.

### Firma persistente y seguridad de secretos

Las builds release deben reutilizar **una única keystore privada y respaldada**. No se publica la clave ni sus contraseñas. La configuración usa las variables:

`GHOST_WIFI_STORE_FILE`, `GHOST_WIFI_STORE_PASSWORD`, `GHOST_WIFI_KEY_ALIAS`, `GHOST_WIFI_KEY_PASSWORD`.

GitHub Actions contempla secretos separados, incluido `GHOST_WIFI_KEYSTORE_B64`. No subas archivos `.p12`, `.jks`, claves, contraseñas, tokens o credenciales al repositorio.

## Roadmap

- **M0 — Base Android:** interfaz Compose, icono, diagnóstico root/monitor, USB VID/PID, About y detección preliminar PCAP.
- **M1 — Matriz de dispositivos:** evidencia versionada de compatibilidad por kernel y firmware.
- **M2 — Adaptadores:** permisos USB, identificación de chipsets y comprobaciones de drivers/potencia.
- **M3 — Capturas autorizadas:** soporte específico por hardware, sesiones PCAP y revisión de datos.
- **M4 — Linux remoto:** SSH autenticado, validación de huella del host y agente de alcance reducido.
- **M5 — Informes y QA:** JSON/PDF, historial local, accesibilidad, traducciones y pruebas en diferentes marcas.

Más detalles: [docs/ROADMAP.md](docs/ROADMAP.md).

## Uso responsable

Proyecto orientado a **aprendizaje, diagnóstico y auditorías con autorización**. No se incluyen sistemas de ataque automático, recuperación de claves, explotación de routers, inyección de paquetes ni herramientas para acceder a redes ajenas. La app no modifica el kernel ni intenta activar a la fuerza funcionalidades no anunciadas por un controlador.

## Autor, enlaces y créditos

**Desarrollado por Ghost Developer** — diseño, desarrollo y mantenimiento.

- [Canal oficial de Telegram](https://t.me/GhostDeve)
- [Comunidad y soporte](https://t.me/CodeBreakersHub)
- [Código fuente](https://github.com/Gh0stDeveloper/Ghost-Wifi-Auditor)
- Referencias técnicas: [Kali Linux](https://www.kali.org/) / [Kali NetHunter](https://www.kali.org/docs/nethunter/).

El dragón es una **ilustración geométrica original inspirada en la estética de ciberseguridad**; no es el logotipo oficial de Kali Linux. Las marcas, herramientas y proyectos de terceros conservan sus respectivos derechos, licencias y atribuciones.

**Licencia:** pendiente de elección por el titular. La disponibilidad pública del código no implica por sí misma permiso de redistribución o modificación.
