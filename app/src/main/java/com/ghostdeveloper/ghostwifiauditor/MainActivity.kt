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
    val ctx=LocalContext.current
    val scope=rememberCoroutineScope()
    var root by remember { mutableStateOf("Sin comprobar") }
    var monitor by remember { mutableStateOf("Sin comprobar") }
    var usb by remember { mutableStateOf(HardwareInspector.usbDevices(ctx).size) }
    Column(verticalArrangement=Arrangement.spacedBy(13.dp)) {
        H1("Panel de control")
        Small("Diagnóstico del teléfono actual; los estados no se simulan.")
        Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            Panel(Modifier.weight(1f)) {
                Icon(Icons.Filled.VerifiedUser,null,tint=Green)
                Text("Root",fontWeight=FontWeight.SemiBold)
                Small(root,if(root.startsWith("Root confirmado")) Green else Muted)
            }
            Panel(Modifier.weight(1f)) {
                Icon(Icons.Filled.WifiTethering,null,tint=Blue)
                Text("Monitor",fontWeight=FontWeight.SemiBold)
                Small(monitor)
            }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            Panel(Modifier.weight(1f)) {
                Icon(Icons.Filled.Usb,null,tint=Blue)
                Text("USB OTG",fontWeight=FontWeight.SemiBold)
                Small("$usb dispositivo(s) enumerados")
            }
            Panel(Modifier.weight(1f)) {
                Icon(Icons.Filled.Laptop,null,tint=Blue)
                Text("Linux remoto",fontWeight=FontWeight.SemiBold)
                Small("No configurado")
            }
        }
        PrimaryAction("Comprobar Root y modo monitor") {
            scope.launch {
                root="Comprobando…"; monitor="Comprobando…"
                root=HardwareInspector.rootState()
                monitor=HardwareInspector.monitorState()
                usb=HardwareInspector.usbDevices(ctx).size
            }
        }
        H2("Seleccionar método de conexión")
        ModeTile(Icons.Filled.PhoneAndroid,"Wi-Fi interno", "Usar el hardware integrado cuando admita monitor",goAudit)
        ModeTile(Icons.Filled.Usb,"Adaptador Wi-Fi USB (OTG)","Chipsets configurables y verificación de drivers",goAudit)
        ModeTile(Icons.Filled.Laptop,"Conexión remota Linux", "Panel Android para un equipo Linux compatible",goAudit)
        Panel {
            StatusRow(Icons.Filled.Security,"Auditoría autorizada","Solo lectura",Green)
            Small("No se modifica el kernel, no se fuerza la interfaz Wi-Fi ni se ejecutan ataques automáticamente.")
            TextButton(onClick=goSettings) { Text("Configurar compatibilidad",color=Blue) }
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
    val context=LocalContext.current
    var mode by rememberSaveable { mutableStateOf("Interno") }
    var pcapResult by remember { mutableStateOf("No se ha seleccionado ningún archivo.") }
    val pickPcap=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) {
            pcapResult=runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    run {
                        val bytes = ByteArray(64)
                        val read = input.read(bytes)
                        NativePcap.inspectHeader(bytes.copyOf(read.coerceAtLeast(0)))
                    }
                } ?: "No se pudo abrir el archivo"
            }.getOrElse { "Error al inspeccionar: ${it.javaClass.simpleName}" }
        }
    }
    Column(verticalArrangement=Arrangement.spacedBy(13.dp)) {
        H1("Centro de auditoría")
        Small("Selecciona el motor de diagnóstico para tus redes autorizadas.")
        Panel {
            H2("Método")
            listOf("Interno","USB OTG","Linux remoto").forEach { option ->
                Row(verticalAlignment=Alignment.CenterVertically, modifier=Modifier.fillMaxWidth()
                    .clickable{mode=option}) {
                    RadioButton(selected=mode==option,onClick={mode=option})
                    Text(option)
                }
            }
            HorizontalDivider(color=Color.DarkGray)
            when(mode) {
                "Interno" -> Small("Se comprobará si el controlador del teléfono anuncia monitor. Root no garantiza compatibilidad.")
                "USB OTG" -> Small("La app puede enumerar USB; cargar un driver Wi-Fi requiere soporte del kernel.")
                else -> Small("Agente Linux y SSH todavía pendientes de implementar. Sin credenciales guardadas.")
            }
        }
        Panel {
            H2("Herramientas disponibles")
            StatusRow(Icons.Filled.Usb,"Enumeración de dispositivos USB","Disponible",Green)
            StatusRow(Icons.Filled.Shield,"Diagnóstico Root","Disponible",Green)
            StatusRow(Icons.Filled.InsertDriveFile,"Reconocimiento PCAP/PCAPNG","Disponible",Green)
            StatusRow(Icons.Filled.Radar,"Captura Wi-Fi 802.11","Próximo hito",Amber)
            StatusRow(Icons.Filled.Terminal,"Control SSH remoto","Próximo hito",Amber)
        }
        Panel {
            H2("Inspección nativa C++")
            Small("Selecciona un archivo PCAP o PCAPNG. La app reconoce únicamente la cabecera; no extrae credenciales ni analiza todavía los paquetes.")
            OutlinedButton(onClick={pickPcap.launch(arrayOf("*/*"))}, modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.FolderOpen,null)
                Spacer(Modifier.width(8.dp))
                Text("Importar captura")
            }
            Small(pcapResult,color=Color.White)
        }
    }
}

@Composable private fun DevicesScreen() {
    val ctx=LocalContext.current
    var search by rememberSaveable { mutableStateOf("") }
    var usbList by remember { mutableStateOf(HardwareInspector.usbDevices(ctx)) }
    val detected = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
    Column(verticalArrangement=Arrangement.spacedBy(13.dp)) {
        H1("Equipos compatibles")
        Panel {
            H2("Mi teléfono: $detected")
            Small("Android ${android.os.Build.VERSION.RELEASE} · ${android.os.Build.DEVICE}")
            Small("La compatibilidad del hardware debe comprobarse en Inicio. No se infiere solo por el modelo.")
        }
        Small("Compatibilidad documentada para kernels/ROM específicos. No implica soporte de fábrica.")
        OutlinedTextField(value=search,onValueChange={search=it},label={Text("Buscar un dispositivo")},
            leadingIcon={Icon(Icons.Filled.Search,null)},singleLine=true,modifier=Modifier.fillMaxWidth())
        supported.filter { it.name.contains(search,true)||it.details.contains(search,true) }.forEach { device ->
            Panel {
                Text(device.name,fontWeight=FontWeight.SemiBold)
                Small(device.support,Green)
                Small(device.details)
                TextButton(onClick={
                    ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(device.url)))
                }) { Text("Ver referencia técnica",color=Blue) }
            }
        }
        Panel {
            H2("Otros Android: Xiaomi, Samsung, Honor…")
            Small("Si un modelo no aparece, se marca como SIN VERIFICAR. La app puede examinar su root e interfaces y recomendar adaptador externo o Linux remoto cuando corresponda.")
            Small("Redmi 14C: en nuestras pruebas el controlador MediaTek Gen4m rechazó crear una interfaz monitor.",Amber)
            Small("Incluso los teléfonos listados con monitor interno pueden necesitar un adaptador USB según firmware, kernel y modo de trabajo.")
        }
        Panel {
            H2("USB conectados")
            usbList.forEach { Small(it,color=Color.White) }
            if(usbList.isEmpty()) Small("No hay dispositivos USB enumerados.")
            OutlinedButton(onClick={usbList=HardwareInspector.usbDevices(ctx)}) {
                Icon(Icons.Filled.Refresh,null)
                Spacer(Modifier.width(8.dp))
                Text("Actualizar USB")
            }
            Small("VID/PID sirve para identificar dispositivos, pero no prueba disponibilidad de un driver.")
        }
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
                Small("Versión 0.1.0-alpha · Diagnóstico")
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
