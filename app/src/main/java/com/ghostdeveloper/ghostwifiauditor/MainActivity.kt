package com.ghostdeveloper.ghostwifiauditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val Bg = Color(0xFF080A0D)
private val CardBg = Color(0xFF232527)
private val PanelBg = Color(0xFF14171B)
private val Green = Color(0xFF54D98A)
private val Blue = Color(0xFF66B9F1)
private val Muted = Color(0xFFA9ACB3)
private val Amber = Color(0xFFFFC87D)

private data class DeviceItem(val name: String, val details: String, val support: String, val url: String)
private val supported = listOf(
    DeviceItem("Google Nexus 5", "Nexmon y kernel/firmware específicamente modificados", "Interno · Condicionado", "https://www.kali.org/docs/nethunter/wireless-cards/"),
    DeviceItem("Samsung Galaxy S10 / S10e / S10+ / S10 5G", "Variantes compatibles con NetHunter + LineageOS 23 (no firmware stock)", "Interno · Condicionado", "https://www.kali.org/blog/kali-linux-2025-4-release/"),
    DeviceItem("OnePlus 7 / 7 Pro / 7T / 7T Pro", "Kernel NetHunter para LineageOS 23.2; verificar modelo exacto", "Interno · Condicionado", "https://nethunter.kali.org/kernels")
)
private data class AdapterItem(
    val name: String, val chipset: String, val note: String,
    val bands: String, val photoUrl: String, val photoCredit: String, val officialUrl: String
)
// Reference photographs load from their credited sources on demand. The app does not
// bundle or redistribute manufacturer/retailer imagery in the source archive.
private val adapters = listOf(
    AdapterItem("ALFA AWUS036ACM", "MediaTek MT7612U", "Buena opción de doble banda en Linux. En Android requiere módulo MT76 y alimentación OTG compatibles.",
        "2.4 / 5 GHz", "https://justcallmekokollc.com/cdn/shop/files/DSC07509-Edit.jpg?v=1761143131", "Foto: JustCallMeKoko", "https://www.alfa.com.tw/products/awus036acm"),
    AdapterItem("ALFA AWUS036NHA", "Atheros AR9271", "Opción conocida de 2.4 GHz con ath9k_htc en Linux. En Android necesita soporte del kernel.",
        "2.4 GHz", "https://zsecurity.org/wp-content/uploads/2020/11/alfa-awus036nha-wireless-adapter-7.jpg", "Foto: zSecurity", "https://www.alfa.com.tw/products/awus036nha"),
    AdapterItem("TP-Link TL-WN722N v1", "Atheros AR9271", "Comprobar físicamente que sea revisión V1. Revisiones V2/V3 usan hardware diferente.",
        "2.4 GHz", "https://cdn.sotel.de/images/1000024296/tp-link-tl-wn722n-n150-wlan-high-gain-usb-stick-150-mbits-002.jpg", "Foto: Sotel", "https://www.tp-link.com/mx/home-networking/adapter/tl-wn722n/v1/"),
    AdapterItem("ALFA AWUS036ACH", "Realtek RTL8812AU", "Doble banda. Puede requerir controlador adicional; no asumir que funcionará en Android stock.",
        "2.4 / 5 GHz", "https://www.alfa.com.tw/cdn/shop/files/AWUS036ACH_05_800x800.jpg?v=1763618631", "Foto: ALFA Network", "https://www.alfa.com.tw/products/awus036ach_1"),
    AdapterItem("Otros adaptadores", "Chipset y VID/PID", "Registra la referencia exacta para comprobar si existe un controlador compatible.",
        "Por identificar", "", "", "https://www.kali.org/docs/nethunter/wireless-cards/")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GhostTheme { MainShell() } }
    }
}

@Composable
private fun GhostTheme(content: @Composable () -> Unit) {
    val palette = darkColorScheme(
        primary = Green, secondary = Blue, background = Bg, surface = CardBg,
        onPrimary = Color.Black, onSecondary = Color.Black, onBackground = Color.White,
        onSurface = Color.White
    )
    MaterialTheme(colorScheme = palette, typography = Typography(), content = content)
}

@Composable
private fun MainShell() {
    var section by rememberSaveable { mutableStateOf("Inicio") }
    val screens = listOf("Inicio", "Auditoría", "Equipos", "Ajustes")
    val symbols = listOf(Icons.Filled.Home, Icons.Filled.Radar, Icons.Filled.Devices,
        Icons.Filled.Settings)
    Scaffold(
        // Android 15+ draws behind status/navigation bars; inset the full scaffold.
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Bg,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF101317)) {
                screens.forEachIndexed { idx, name ->
                    NavigationBarItem(
                        selected = section == name || (section == "About" && name == "Ajustes"), onClick = { section = name },
                        icon = { Icon(symbols[idx], contentDescription = name, modifier=Modifier.size(21.dp)) },
                        label = { Text(name, fontSize=10.sp, maxLines=1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor=Green, selectedTextColor=Green,
                            indicatorColor=Color(0xFF1C3428),
                            unselectedIconColor=Muted, unselectedTextColor=Muted
                        )
                    )
                }
            }
        },
        topBar = {
            Row(
                Modifier.fillMaxWidth().background(Bg).padding(horizontal=18.dp, vertical=14.dp),
                verticalAlignment=Alignment.CenterVertically
            ) {
                Image(painterResource(R.drawable.ic_dragon), "Dragón original Ghost WiFi Auditor",
                    Modifier.size(48.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Ghost WiFi Auditor", fontWeight=FontWeight.Bold, fontSize=19.sp)
                    Text("WIRELESS SECURITY DASHBOARD", color=Muted, fontSize=10.sp)
                }
                IconButton(onClick={ section="Ajustes" }) { Icon(Icons.Filled.Settings,"Ajustes", tint=Muted) }
            }
        }
    ) { inset ->
        LazyColumn(Modifier.fillMaxSize().padding(inset).padding(horizontal=16.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp),
            contentPadding=PaddingValues(bottom=22.dp)) {
            item {
                when(section) {
                    "Inicio" -> HomeScreen({ section="Auditoría" }, { section="Ajustes" })
                    "Auditoría" -> AuditScreen()
                    "Equipos" -> DevicesScreen()
                    "Ajustes" -> SettingsScreen(openAbout = { section="About" })
                    else -> AboutScreen(onBack = { section="Ajustes" })
                }
            }
        }
    }
}

@Composable
private fun Panel(modifier: Modifier=Modifier, content: @Composable ColumnScope.()->Unit) {
    Column(modifier.fillMaxWidth().background(CardBg, RoundedCornerShape(15.dp))
        .padding(15.dp), verticalArrangement=Arrangement.spacedBy(9.dp), content=content)
}
@Composable private fun H1(text:String) { Text(text,fontSize=20.sp,fontWeight=FontWeight.Bold) }
@Composable private fun H2(text:String) { Text(text,fontSize=16.sp,fontWeight=FontWeight.SemiBold) }
@Composable private fun Small(text:String, color:Color=Muted) { Text(text,color=color,fontSize=12.sp,lineHeight=17.sp) }
@Composable private fun StatusRow(icon:ImageVector,label:String,value:String,color:Color=Muted) {
    Row(verticalAlignment=Alignment.CenterVertically, modifier=Modifier.fillMaxWidth()) {
        Icon(icon,null,tint=color,modifier=Modifier.size(19.dp))
        Spacer(Modifier.width(9.dp))
        Text(label,Modifier.weight(1f),fontSize=13.sp)
        Text(value,fontSize=12.sp,color=color)
    }
}
@Composable private fun PrimaryAction(text:String, onClick:()->Unit) {
    Button(onClick=onClick,modifier=Modifier.fillMaxWidth(),
        colors=ButtonDefaults.buttonColors(containerColor=Green, contentColor=Color.Black)) {
        Text(text,fontWeight=FontWeight.SemiBold)
    }
}

@Composable private fun HomeScreen(goAudit:()->Unit,goSettings:()->Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var root by remember { mutableStateOf<DiagnosticFinding?>(null) }
    var monitor by remember { mutableStateOf<DiagnosticFinding?>(null) }
    var details by remember { mutableStateOf<DeviceFingerprint?>(null) }
    var busy by remember { mutableStateOf(false) }
    var usb by remember { mutableIntStateOf(HardwareInspector.usbDevices(ctx).size) }
    Column(verticalArrangement=Arrangement.spacedBy(13.dp)) {
        H1("Panel de control")
        Small("Diagnóstico actual, bajo demanda. Root y monitor no se conservan entre reinicios.")
        Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            Panel(Modifier.weight(1f)) {
                Icon(Icons.Filled.VerifiedUser,null,tint=Green)
                Text("Root",fontWeight=FontWeight.SemiBold)
                Small(root?.summary ?: "Sin comprobar",
                    if(root?.status==CapabilityStatus.SUPPORTED) Green else Muted)
            }
            Panel(Modifier.weight(1f)) {
                Icon(Icons.Filled.WifiTethering,null,tint=Blue)
                Text("Modo monitor",fontWeight=FontWeight.SemiBold)
                Small(monitor?.summary ?: "Sin comprobar",
                    if(monitor?.status==CapabilityStatus.SUPPORTED) Green else Muted)
            }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            Panel(Modifier.weight(1f)) {
                Icon(Icons.Filled.Usb,null,tint=Blue)
                Text("USB OTG",fontWeight=FontWeight.SemiBold)
                Small("$usb dispositivo(s) detectado(s)")
            }
            Panel(Modifier.weight(1f)) {
                Icon(Icons.Filled.Laptop,null,tint=Blue)
                Text("Linux remoto",fontWeight=FontWeight.SemiBold)
                Small("Próximo hito M4")
            }
        }
        PrimaryAction(if(busy) "Diagnosticando…" else "Comprobar dispositivo") {
            if(!busy) scope.launch {
                busy = true
                try {
                    // No cached root result; each check verifies the current session.
                    root = HardwareInspector.rootDiagnostic(ctx)
                    monitor = HardwareInspector.monitorDiagnostic(
                        ctx, root?.status == CapabilityStatus.SUPPORTED
                    )
                    details = HardwareInspector.deviceInfo(ctx)
                    usb = HardwareInspector.usbDevices(ctx).size
                } finally {
                    busy = false
                }
            }
        }
        if(root!=null || monitor!=null) Panel {
            H2("Evidencia técnica")
            root?.let {
                Small("Root: ${it.evidence}")
                Small("Verificado: "+java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it.observedAtEpochMs)))
            }
            monitor?.let {
                Small("Monitor: ${it.evidence}")
                Small("Verificado: "+java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it.observedAtEpochMs)))
            }
            details?.let {
                Small("Android ${it.android} · Kernel ${it.kernel}")
                Small("Wi-Fi: ${it.wlanDriver} · Firmware: ${it.wlanFirmware}")
            }
            Small("‘Monitor anunciado’ no demuestra que el controlador permita capturar tramas. La app no altera interfaces.")
        }
        H2("Seleccionar método de conexión")
        ModeTile(Icons.Filled.PhoneAndroid,"Wi-Fi interno",
            "Verificar capacidades del controlador en este modelo",goAudit)
        ModeTile(Icons.Filled.Usb,"Adaptador Wi-Fi USB (OTG)",
            "Chipsets configurables; comprobar controlador y alimentación",goAudit)
        ModeTile(Icons.Filled.Laptop,"Conexión remota Linux",
            "Panel Android para un equipo Linux compatible (pendiente)",goAudit)
        Panel {
            StatusRow(Icons.Filled.Security,"Auditoría autorizada","Diagnóstico de solo lectura",Green)
            Small("No se ejecutan exploits, comandos propietarios ni modificaciones del kernel.")
            TextButton(onClick=goSettings) { Text("Configuración de compatibilidad",color=Blue) }
        }
    }
}

@Composable private fun ModeTile(icon:ImageVector,title:String,desc:String,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().border(1.dp,Color(0xFF303439),RoundedCornerShape(14.dp))
        .clickable(onClick=onClick).padding(15.dp),verticalAlignment=Alignment.CenterVertically) {
        Icon(icon,null,tint=Blue,modifier=Modifier.size(24.dp))
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) { Text(title,fontWeight=FontWeight.SemiBold); Small(desc) }
        Icon(Icons.Filled.ChevronRight,null,tint=Muted,modifier=Modifier.size(13.dp))
    }
}

@Composable private fun AuditScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by rememberSaveable { mutableStateOf("Interno") }
    var captures by remember { mutableStateOf(CaptureRepository.list(context)) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Importa un archivo PCAP/PCAPNG para analizarlo sin conexión.") }
    var selected by remember { mutableStateOf<CaptureEntry?>(null) }
    var confirmDelete by remember { mutableStateOf<CaptureEntry?>(null) }
    var exportEntry by remember { mutableStateOf<CaptureEntry?>(null) }
    val pickPcap=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null && !busy) {
            scope.launch {
                busy = true
                try {
                    val entry = CaptureRepository.importCapture(context,uri)
                    captures = CaptureRepository.list(context)
                    selected = entry
                    message = "Importación completa: ${entry.packets} registros examinados"
                } catch (ex: Exception) {
                    message = "Error al importar: ${ex.message ?: ex.javaClass.simpleName}"
                } finally {
                    busy = false
                }
            }
        }
    }
    val reportSaver=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val item=exportEntry
        if(uri!=null && item!=null) scope.launch {
            try {
                CaptureRepository.exportReport(context,item,uri)
                message="Informe JSON guardado correctamente."
            } catch(ex: Exception) { message="Error al exportar informe: ${ex.message}" }
        }
    }
    val captureSaver=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val item=exportEntry
        if(uri!=null && item!=null) scope.launch {
            try {
                CaptureRepository.exportCapture(context,item,uri)
                message="Captura original exportada."
            } catch(ex: Exception) { message="Error al exportar captura: ${ex.message}" }
        }
    }
    if(confirmDelete!=null) {
        val item=confirmDelete!!
        AlertDialog(
            onDismissRequest={confirmDelete=null},
            title={Text("Eliminar captura")},
            text={Text("Se eliminará el archivo importado y su historial local: ${item.displayName}.")},
            confirmButton={
                TextButton(onClick={
                    val removed=CaptureRepository.delete(context,item.id)
                    captures=CaptureRepository.list(context)
                    if(selected?.id==item.id) selected=null
                    message=if(removed) "Captura eliminada." else "No se pudo eliminar el archivo."
                    confirmDelete=null
                }) { Text("Eliminar",color=Amber) }
            },
            dismissButton={TextButton(onClick={confirmDelete=null}) { Text("Cancelar") }}
        )
    }
    Column(verticalArrangement=Arrangement.spacedBy(13.dp)) {
        H1("Centro de auditoría")
        Small("M3 · Análisis offline de tramas y gestión de capturas autorizadas.")
        Panel {
            H2("Motor de captura")
            listOf("Interno","USB OTG","Linux remoto").forEach { option ->
                Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth()
                    .clickable { mode=option }) {
                    RadioButton(selected=mode==option,onClick={mode=option})
                    Text(option)
                }
            }
            HorizontalDivider(color=Color.DarkGray)
            Small(when(mode) {
                "Interno" -> "Wi-Fi integrado: exige una interfaz monitor operativa y validada. Root por sí solo no basta."
                "USB OTG" -> "Un dispositivo USB detectado no implica que el kernel tenga un driver de captura compatible."
                else -> "SSH y agente Linux no están implementados todavía (plan M4)."
            })
            StatusRow(Icons.Filled.RadioButtonUnchecked,"Captura en vivo","Sin backend validado",Amber)
            Small("El inicio de captura permanecerá deshabilitado hasta implementar y probar un motor real para el hardware. No se simulan resultados.")
        }
        Panel {
            H2("Importación y análisis C++")
            Small("PCAP y PCAPNG: valida bloques, límites y registros; clasifica tramas 802.11 en gestión, control y datos. No extrae contraseñas.")
            OutlinedButton(
                onClick={pickPcap.launch(arrayOf("*/*"))},
                enabled=!busy,modifier=Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.FolderOpen,null)
                Spacer(Modifier.width(8.dp))
                Text(if(busy) "Analizando…" else "Importar PCAP / PCAPNG")
            }
            Small("Tamaño máximo: 128 MiB por captura y 256 MiB de historial local. Los archivos quedan privados.")
            Small(message,if(message.startsWith("Error")) Amber else Green)
        }
        Panel {
            H2("Historial de capturas")
            if(captures.isEmpty()) Small("Sin capturas importadas.")
            captures.forEach { entry ->
                val active=selected?.id==entry.id
                Column(Modifier.fillMaxWidth().border(1.dp,
                    if(active) Green else Color(0xFF34383A),RoundedCornerShape(12.dp))
                    .clickable { selected=entry }.padding(11.dp),
                    verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    Text(entry.displayName,fontWeight=FontWeight.Medium,fontSize=13.sp,
                        maxLines=2,overflow=TextOverflow.Ellipsis)
                    Small("${entry.format} · ${entry.packets} paquetes · ${entry.bytes} bytes",
                        if(active) Green else Muted)
                    Small(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(entry.importedAt)))
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick={selected=entry}) { Text("Detalles",color=Blue) }
                        TextButton(onClick={confirmDelete=entry}) { Text("Eliminar",color=Amber) }
                    }
                }
            }
        }
        selected?.let { entry ->
            Panel {
                H2("Resultados de análisis")
                StatusRow(Icons.Filled.InsertDriveFile,"Formato",entry.format,Green)
                StatusRow(Icons.Filled.List,"Paquetes",entry.packets.toString(),Green)
                StatusRow(Icons.Filled.Settings,"Tramas de gestión",entry.summary.optLong("management").toString())
                StatusRow(Icons.Filled.Settings,"Tramas de control",entry.summary.optLong("control").toString())
                StatusRow(Icons.Filled.Settings,"Tramas de datos",entry.summary.optLong("data").toString())
                StatusRow(Icons.Filled.Settings,"Ethernet",entry.summary.optLong("ethernet").toString())
                StatusRow(Icons.Filled.Settings,"Otros enlaces",entry.summary.optLong("other").toString())
                val links=entry.summary.optJSONArray("linkTypes")
                val linkLabels=links?.let { l -> (0 until l.length()).joinToString(", ") { l.optInt(it).toString() } }
                Small("Tipos de enlace: ${linkLabels?.ifBlank { "no presentes" } ?: "desconocidos"}")
                Small("Los resultados representan categorías de trama, no vulnerabilidades ni credenciales.")
                OutlinedButton(modifier=Modifier.fillMaxWidth(),onClick={
                    exportEntry=entry
                    reportSaver.launch("ghost-wifi-report-${entry.id.take(8)}.json")
                }) { Text("Exportar informe JSON") }
                OutlinedButton(modifier=Modifier.fillMaxWidth(),onClick={
                    exportEntry=entry
                    val suffix=if(entry.format=="PCAPNG") "pcapng" else "pcap"
                    captureSaver.launch("ghost-wifi-${entry.id.take(8)}.$suffix")
                }) { Text("Exportar captura original") }
            }
        }
    }
}

@Composable private fun DevicesScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val catalog = remember { DeviceCatalog.load(ctx) }
    var search by rememberSaveable { mutableStateOf("") }
    var profile by remember { mutableStateOf<DeviceFingerprint?>(null) }
    var scanning by remember { mutableStateOf(false) }
    val model = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
    val relevant = remember(catalog,model) { DeviceCatalog.matching(catalog) }
    Column(verticalArrangement=Arrangement.spacedBy(13.dp)) {
        H1("Equipos y compatibilidad")
        Panel {
            H2("Este teléfono · $model")
            Small("Android ${android.os.Build.VERSION.RELEASE} · ${android.os.Build.DEVICE}")
            Small("Ninguna compatibilidad se concede solo por coincidir el nombre del equipo.")
            OutlinedButton(enabled=!scanning, onClick={
                scope.launch {
                    scanning = true
                    try { profile = HardwareInspector.deviceInfo(ctx) }
                    finally { scanning = false }
                }
            }) { Text(if(scanning) "Leyendo…" else "Actualizar información del dispositivo") }
            profile?.let {
                Small("Build: ${it.build}")
                Small("Kernel: ${it.kernel}")
                Small("Driver: ${it.wlanDriver}")
                Small("Firmware WLAN: ${it.wlanFirmware}")
                Small("No se recopilan MAC, IMEI, seriales ni contraseñas Wi-Fi.")
            }
            if(relevant.isEmpty()) {
                Small("No hay entrada específica para este modelo: NO VERIFICADO.", Amber)
            } else {
                relevant.forEach { Small("Catálogo: ${it.name} · ${it.level}",Amber) }
            }
        }
        Small("Catálogo local versionado. El soporte publicado suele exigir firmware, ROM y kernel específicos.")
        OutlinedTextField(value=search,onValueChange={search=it.take(80)},
            label={Text("Buscar modelos documentados")},
            leadingIcon={Icon(Icons.Filled.Search,null)},singleLine=true,
            modifier=Modifier.fillMaxWidth())
        catalog.filter { it.name.contains(search,true) || it.requirements.contains(search,true) }
            .forEach { entry ->
                Panel {
                    Text(entry.name,fontWeight=FontWeight.SemiBold)
                    Small(when(entry.level) {
                        "DOCUMENTADO_CONDICIONAL" -> "Documentado con requisitos específicos"
                        "OBSERVACION_NO_REPLICADA" -> "Observación de usuario, sin replicación independiente"
                        else -> "Sin verificar"
                    },Amber)
                    Small(entry.support)
                    Small(entry.requirements)
                    Small("Evidencia: ${entry.evidenceDate} · Puede necesitar USB: ${if(entry.usbMayBeNeeded) "Sí" else "Depende"}")
                    if(entry.sourceUrl.startsWith("https://")) {
                        TextButton(onClick={openExternalLink(ctx,entry.sourceUrl)}) {
                            Text("Ver fuente técnica",color=Blue)
                        }
                    }
                }
            }
        UsbAdapterPanel()
    }
}

/**
 * M2: USB Host observations only. The panel cannot install drivers or force monitor mode.
 * Android's attach/detach broadcasts refresh the UI while the screen is visible.
 */
@Composable private fun UsbAdapterPanel() {
    val ctx = LocalContext.current
    val profiles = remember { UsbProfiles.load(ctx) }
    var usbDevices by remember { mutableStateOf(UsbAdapterInspector.enumerate(ctx, profiles)) }
    var usbMessage by remember { mutableStateOf("Conecta un adaptador por OTG o actualiza la lista.") }
    val hostSupported = remember {
        ctx.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_USB_HOST)
    }
    DisposableEffect(ctx) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
                when (intent.action) {
                    android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED,
                    android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED,
                    ACTION_GHOST_USB_CHANGED -> {
                        usbDevices = UsbAdapterInspector.enumerate(ctx, profiles)
                        usbMessage = "Estado USB actualizado"
                    }
                }
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(ACTION_GHOST_USB_CHANGED)
        }
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            ctx.registerReceiver(receiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            ctx.registerReceiver(receiver, filter)
        }
        onDispose { runCatching { ctx.unregisterReceiver(receiver) } }
    }
    Panel {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Filled.Usb, null, tint=Green)
            Spacer(Modifier.width(8.dp))
            H2("USB OTG · Compatibilidad M2")
        }
        Small(if(hostSupported) "Android declara USB Host disponible" else
            "Este equipo no anuncia USB Host; comprueba soporte OTG")
        Small("La detección y el permiso USB NO instalan un controlador Wi-Fi.")
        if(usbDevices.isEmpty()) {
            Small("No se detectaron dispositivos USB conectados.")
        }
        usbDevices.forEach { entry ->
            Column(
                Modifier.fillMaxWidth().border(1.dp,Color(0xFF353B3D),RoundedCornerShape(12.dp))
                    .padding(11.dp), verticalArrangement=Arrangement.spacedBy(6.dp)
            ) {
                Text(entry.description, fontWeight=FontWeight.SemiBold,fontSize=14.sp)
                Small("USB ${entry.usbId} · Clase: ${entry.deviceClass} · Interfaces: ${entry.interfaces}",Blue)
                if(entry.profile == null) {
                    Small("Chipset desconocido. Se puede registrar y comprobar manualmente.",Amber)
                } else {
                    Small("Chipset candidato: ${entry.profile.candidateChipset}",Green)
                    Small("Driver Linux de referencia: ${entry.profile.linuxDriver} · ${entry.profile.bands}")
                    Small(entry.driverEvidence)
                    Small(entry.profile.caveat)
                    TextButton(onClick={openExternalLink(ctx,entry.profile.sourceUrl)}) {
                        Text("Documentación del driver",color=Blue)
                    }
                }
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Small(if(entry.permissionGranted) "Permiso USB concedido" else "Permiso USB no concedido",
                        if(entry.permissionGranted) Green else Muted)
                    Spacer(Modifier.weight(1f))
                    if(!entry.permissionGranted) {
                        OutlinedButton(onClick={
                            usbMessage = UsbAdapterInspector.requestAccess(ctx,entry.deviceName)
                        }) { Text("Solicitar permiso",fontSize=12.sp) }
                    }
                }
                Small("Captura y modo monitor: NO VERIFICADOS",Amber)
            }
        }
        OutlinedButton(onClick={
            usbDevices = UsbAdapterInspector.enumerate(ctx, profiles)
            usbMessage = "Exploración manual terminada"
        }) {
            Icon(Icons.Filled.Refresh,null)
            Spacer(Modifier.width(6.dp))
            Text("Actualizar USB")
        }
        Small(usbMessage)
        Small("Si el dongle consume más energía que la que entrega el teléfono, usa un hub OTG con alimentación externa. No cargues módulos de otros kernels.")
    }
}

@Composable private fun SettingsScreen(openAbout:()->Unit) {
    val ctx=LocalContext.current
    val prefs=remember { ctx.getSharedPreferences("ghost_wifi_settings",android.content.Context.MODE_PRIVATE) }
    var rootMethod by remember { mutableStateOf(prefs.getString("root_method","Temporal") ?: "Temporal") }
    var selection by remember { mutableStateOf(prefs.getString("adapter","ALFA AWUS036ACM")?:"ALFA AWUS036ACM") }
    var customAdapter by remember { mutableStateOf(prefs.getString("custom_adapter", "") ?: "") }
    var permitted by remember { mutableStateOf(prefs.getBoolean("authorization",true)) }
    Column(verticalArrangement=Arrangement.spacedBy(13.dp)) {
        H1("Ajustes")
        Panel {
            H2("Root y compatibilidad")
            Small("Preferencia de guía: no certifica el estado root. Verifica root desde Inicio en cada sesión.")
            listOf("Temporal","Persistente","Sin Root").forEach { item ->
                Row(verticalAlignment=Alignment.CenterVertically) {
                    RadioButton(selected=rootMethod==item,onClick={rootMethod=item;prefs.edit().putString("root_method",item).apply()})
                    Text(item,modifier=Modifier.clickable {rootMethod=item;prefs.edit().putString("root_method",item).apply()})
                }
            }
            Small(when(rootMethod) {
                "Temporal" -> "DFRoot puede requerir un kernel vulnerable y compatible. Reiniciar suele retirar el root temporal. Nunca se activará automáticamente desde esta app."
                "Persistente" -> "Requiere evaluar bootloader, firmware, soporte KernelSU/Magisk y riesgos específicos del teléfono. La app no flashea imágenes."
                else -> "Puedes revisar USB e importar archivos PCAP. Las funciones privilegiadas permanecerán deshabilitadas."
            })
        }
        Panel {
            H2("Adaptadores recomendados")
            Small("Fotos de referencia para reconocer cada modelo. Verifica el chipset, revisión y controlador antes de comprar.")
            adapters.forEach { a ->
                Column(Modifier.fillMaxWidth().border(1.dp,Color(0xFF34383A),RoundedCornerShape(12.dp)).padding(10.dp)) {
                    Row(verticalAlignment=Alignment.Top, horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        if (a.photoUrl.isNotBlank()) {
                            AdapterPhoto(a.photoUrl, a.name)
                        } else {
                            Box(Modifier.size(85.dp).background(PanelBg,RoundedCornerShape(9.dp)),contentAlignment=Alignment.Center) {
                                Icon(Icons.Filled.Usb,null,tint=Blue)
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(a.name,fontSize=13.sp,fontWeight=FontWeight.Bold)
                            Small(a.chipset,Green)
                            Small(a.bands)
                            Small(a.photoCredit)
                        }
                    }
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        RadioButton(selected=selection==a.name,onClick={selection=a.name;prefs.edit().putString("adapter",a.name).apply()})
                        Text("Seleccionar perfil",fontSize=12.sp,modifier=Modifier.clickable{selection=a.name;prefs.edit().putString("adapter",a.name).apply()})
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick={openExternalLink(ctx,a.officialUrl)}) { Text("Ver modelo",color=Blue,fontSize=12.sp) }
                    }
                    if(selection==a.name) Small(a.note,Blue)
                }
                Spacer(Modifier.height(6.dp))
            }
            if (selection=="Otros adaptadores") {
                OutlinedTextField(
                    value=customAdapter,
                    onValueChange={customAdapter=it.take(150);prefs.edit().putString("custom_adapter",customAdapter).apply()},
                    modifier=Modifier.fillMaxWidth(), singleLine=false,
                    label={Text("Marca, chipset o VID:PID")},
                    placeholder={Text("Ej.: Adaptador USB 0E8D:7612")}
                )
            }
            Small("El soporte anunciado por Kali NetHunter depende del kernel Android. Una imagen no confirma el chipset de una revisión concreta.")
        }
        Panel {
            H2("Seguridad")
            Row(verticalAlignment=Alignment.CenterVertically) {
                Checkbox(checked=permitted,onCheckedChange={permitted=it;prefs.edit().putBoolean("authorization",it).apply()})
                Text("Pedir confirmación de autorización",fontSize=13.sp)
            }
            Small("Los cambios en esta pantalla son preferencias; no modifican el Wi-Fi ni el kernel.")
        }
        Panel {
            H2("Información")
            Row(Modifier.fillMaxWidth().clickable(onClick=openAbout).padding(vertical=7.dp),
                verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Filled.Info,null,tint=Green)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("About · Acerca de",fontWeight=FontWeight.SemiBold)
                    Small("Créditos, canal, comunidad y código fuente")
                }
                Icon(Icons.Filled.ChevronRight,null,tint=Muted)
            }
        }
    }
}

@Composable private fun AdapterPhoto(url:String, name:String) {
    SubcomposeAsyncImage(
        model=url, contentDescription="Fotografía de referencia: $name",
        modifier=Modifier.size(85.dp).clip(RoundedCornerShape(9.dp)),
        contentScale=ContentScale.Crop,
        loading={ Box(Modifier.fillMaxSize().background(PanelBg), contentAlignment=Alignment.Center) {
            Icon(Icons.Filled.Image,null,tint=Blue)
        } },
        error={ Box(Modifier.fillMaxSize().background(PanelBg), contentAlignment=Alignment.Center) {
            Icon(Icons.Filled.Usb,null,tint=Muted)
        } }
    )
}

private fun openExternalLink(ctx: android.content.Context, url: String) {
    if (url.isBlank() || !url.startsWith("https://")) return
    runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(url))) }
}

@Composable private fun AboutScreen(onBack:()->Unit) {
    val ctx=LocalContext.current
    Column(verticalArrangement=Arrangement.spacedBy(13.dp)) {
        TextButton(onClick=onBack) { Icon(Icons.Filled.ArrowBack,null); Spacer(Modifier.width(6.dp)); Text("Volver a Ajustes") }
        Row(verticalAlignment=Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_dragon),null,Modifier.size(76.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                H1("Ghost WiFi Auditor")
                Small("Versión 0.4.0-alpha · Hito M3")
                Small("Dragón original · AMOLED grafito",Green)
            }
        }
        Panel {
            H2("Créditos")
            Text("Ghost Developer",fontSize=18.sp,fontWeight=FontWeight.Bold,color=Green)
            Small("Dirección del proyecto, diseño, desarrollo e integración.")
            HorizontalDivider(color=Color.DarkGray)
            Small("Kotlin · Jetpack Compose · Android USB Host · C++/JNI")
            Small("Las herramientas de terceros conservan sus licencias y atribuciones.")
        }
        Panel {
            H2("Canales oficiales")
            OutlinedButton(onClick={openExternalLink(ctx,"https://t.me/GhostDeve")},modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Campaign,null,tint=Blue)
                Spacer(Modifier.width(8.dp)); Text("Únete a nuestro canal · @GhostDeve")
            }
            OutlinedButton(onClick={openExternalLink(ctx,"https://t.me/CodeBreakersHub")},modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Groups,null,tint=Blue)
                Spacer(Modifier.width(8.dp)); Text("Comunidad · @CodeBreakersHub")
            }
        }
        Panel {
            H2("Código fuente")
            Small("Código fuente público, seguimiento de cambios y documentación del proyecto.")
            OutlinedButton(onClick={openExternalLink(ctx,"https://github.com/Gh0stDeveloper/Ghost-Wifi-Auditor")}, modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Code,null)
                Spacer(Modifier.width(8.dp)); Text("Abrir repositorio GitHub")
            }
        }
        Panel {
            H2("Aviso")
            Small("Aplicación para auditorías autorizadas. La compatibilidad con modo monitor depende del teléfono, kernel, driver, firmware y adaptador.")
        }
    }
}
