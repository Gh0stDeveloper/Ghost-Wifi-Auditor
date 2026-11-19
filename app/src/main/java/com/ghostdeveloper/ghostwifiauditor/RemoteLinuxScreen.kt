package com.ghostdeveloper.ghostwifiauditor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val RemoteGreen: Color @Composable get() = LocalDesignColors.current.accent
private val RemoteBlue: Color @Composable get() = LocalDesignColors.current.blue
private val RemoteSecondary: Color @Composable get() = LocalDesignColors.current.secondary
private val RemoteAmber: Color @Composable get() = LocalDesignColors.current.warning
private val RemoteCardSurface: Color @Composable get() = LocalDesignColors.current.card

@Composable
private fun RemoteCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(RemoteCardSurface, RoundedCornerShape(15.dp))
        .padding(15.dp), verticalArrangement=Arrangement.spacedBy(10.dp), content=content)
}
@Composable private fun RemoteHint(text: String, color: Color = RemoteSecondary) {
    Text(text, color=color, fontSize=12.sp, lineHeight=17.sp)
}

@Composable
fun RemoteLinuxScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = LocalUiPreferences.current
    fun localize(es:String,en:String):String=if(preferences.language=="en") en else es
    // No passwords, passphrases or private-key contents are put in SavedState/SharedPreferences.
    var host by rememberSaveable { mutableStateOf("") }
    var port by rememberSaveable { mutableStateOf("22") }
    var username by rememberSaveable { mutableStateOf("") }
    var knownHostLine by rememberSaveable { mutableStateOf("") }
    var method by rememberSaveable { mutableStateOf("password") }
    var password by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    var privateKeyUri by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf<RemoteLinuxStatus?>(null) }
    var selectedIface by remember { mutableStateOf("") }
    var duration by remember { mutableIntStateOf(10) }
    var authorized by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf("Sin conexión verificada.") }
    var latest by remember { mutableStateOf<CaptureEntry?>(null) }

    val importKey=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        privateKeyUri=uri
        status=null
    }
    fun config(): RemoteLinuxConfig {
        return RemoteLinuxConfig(
            host=host.trim(), port=port.toIntOrNull() ?: -1,
            username=username.trim(), knownHostLine=knownHostLine.trim(),
            password=if(method=="password") password else "",
            keyUri=if(method=="key") privateKeyUri else null,
            keyPassphrase=if(method=="key") passphrase else ""
        )
    }
    Column(verticalArrangement=Arrangement.spacedBy(13.dp)) {
        TextButton(onClick=onBack) {
            Icon(Icons.Filled.ArrowBack,null)
            Spacer(Modifier.width(6.dp))
            Text(tr("Volver a Auditoría","Back to Audit"))
        }
        Text(tr("Linux remoto · SSH","Remote Linux · SSH"),fontSize=20.sp,fontWeight=FontWeight.Bold)
        RemoteHint(tr("Conecta un equipo Linux controlado por ti. No se ejecutan comandos libres ni se omite la verificación de host.",
            "Connect a Linux computer you control. No arbitrary shell commands or bypassed host verification."))
        if(preferences.offlineOnly) RemoteHint(tr("Modo sin conexión activo: desactívalo en Ajustes para utilizar SSH.",
            "Offline mode is active. Disable it in Settings to use SSH."),RemoteAmber)
        RemoteCard {
            Text(tr("Datos de conexión","Connection details"),fontWeight=FontWeight.Bold)
            OutlinedTextField(value=host,onValueChange={host=it.take(253);status=null},
                label={Text(tr("IP o nombre DNS del Linux","Linux hostname or IP"))}, singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(value=port,onValueChange={port=it.filter(Char::isDigit).take(5);status=null},
                label={Text(tr("Puerto SSH","SSH port"))}, singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),
                modifier=Modifier.fillMaxWidth())
            OutlinedTextField(value=username,onValueChange={username=it.take(32);status=null},
                label={Text(tr("Usuario Linux","Linux username"))},singleLine=true,modifier=Modifier.fillMaxWidth())
            Text(tr("Verificación de identidad del servidor","Server identity verification"),fontWeight=FontWeight.SemiBold)
            RemoteHint("Copia la línea de clave pública del host desde una sesión Linux confiable; verifica antes su fingerprint SHA256. Para otros puertos usa [host]:puerto en la primera columna.")
            OutlinedTextField(value=knownHostLine,onValueChange={knownHostLine=it.take(5000);status=null},
                label={Text(tr("Línea known_hosts verificada","Verified known_hosts line"))},minLines=3,maxLines=5,
                modifier=Modifier.fillMaxWidth())
            RemoteHint("Sin clave del servidor verificada, la conexión se rechaza. No uses una clave obtenida de una conexión SSH no verificada.",RemoteAmber)
        }
        RemoteCard {
            Text(tr("Autenticación","Authentication"),fontWeight=FontWeight.Bold)
            Row(verticalAlignment=Alignment.CenterVertically) {
                RadioButton(selected=method=="password",onClick={method="password";status=null})
                Text(tr("Contraseña","Password"))
                Spacer(Modifier.width(10.dp))
                RadioButton(selected=method=="key",onClick={method="key";status=null})
                Text(tr("Clave SSH","SSH key"))
            }
            if(method=="password") {
                OutlinedTextField(
                    value=password,onValueChange={password=it;status=null},
                    label={Text(tr("Contraseña SSH (solo esta sesión)","SSH password (this session only)"))},
                    singleLine=true,visualTransformation=PasswordVisualTransformation(),
                    modifier=Modifier.fillMaxWidth()
                )
            } else {
                OutlinedButton(onClick={importKey.launch(arrayOf("*/*"))},modifier=Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Key,null);Spacer(Modifier.width(8.dp))
                    Text(if(privateKeyUri!=null) "Clave seleccionada" else "Importar clave privada por archivo")
                }
                OutlinedTextField(value=passphrase,onValueChange={passphrase=it;status=null},
                    label={Text(tr("Frase de protección (si existe)","Key passphrase (if any)"))},
                    visualTransformation=PasswordVisualTransformation(),singleLine=true,
                    modifier=Modifier.fillMaxWidth())
            }
            RemoteHint("La clave privada se copia temporalmente para SSH y se elimina al terminar. Ni contraseña ni frase se guardan en preferencias.")
            Button(enabled=!busy && !preferences.offlineOnly,onClick={
                busy=true;status=null;latest=null
                scope.launch {
                    try {
                        status=RemoteLinuxClient.diagnose(context,config())
                        selectedIface=status?.interfaces?.firstOrNull { it.monitor }?.name ?: ""
                        info="Conexión SSH autenticada y agente Linux verificado"
                    } catch (ex:Exception) {
                        info="Fallo de conexión: ${ex.message ?: ex.javaClass.simpleName}"
                    } finally { busy=false }
                }
            },modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=RemoteGreen,contentColor=Color.Black)) {
                Text(if(busy) "Conectando…" else "Verificar conexión y listar interfaces")
            }
            RemoteHint(info,if(status==null) RemoteAmber else RemoteGreen)
        }
        status?.let { s ->
            RemoteCard {
                Text("Linux: ${s.hostname}",fontWeight=FontWeight.Bold)
                RemoteHint("Kernel: ${s.kernel} · Agente: ${s.agentVersion}")
                RemoteHint("tcpdump: ${if(s.captureAvailable) "Detectado" else "No disponible"}",
                    if(s.captureAvailable) RemoteGreen else RemoteAmber)
                if(s.interfaces.isEmpty()) RemoteHint("No se detectaron interfaces de iw.")
                s.interfaces.forEach { iface ->
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        RadioButton(
                            selected=selectedIface==iface.name,
                            onClick={selectedIface=iface.name},
                            enabled=iface.monitor
                        )
                        Column {
                            Text(iface.name,maxLines=1,overflow=TextOverflow.Ellipsis)
                            RemoteHint("Modo: ${iface.mode} ${if(iface.monitor) "(monitor detectado)" else "(no apto para captura M4)"}",
                                if(iface.monitor) RemoteGreen else RemoteSecondary)
                        }
                    }
                }
                if(s.interfaces.none { it.monitor }) {
                    RemoteHint("El equipo Linux debe tener una interfaz monitor real. Esta app no la crea ni modifica automáticamente.",RemoteAmber)
                }
            }
            RemoteCard {
                Text(tr("Captura pasiva autorizada","Authorized passive capture"),fontWeight=FontWeight.Bold)
                RemoteHint("Máximo 30 segundos, 500 paquetes y 32 MiB. Requiere permisos tcpdump en el Linux y una interfaz monitor ya preparada.")
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf(5,10,15,30).forEach { value ->
                        FilterChip(
                            selected=duration==value,onClick={duration=value},
                            label={Text("${value}s")}
                        )
                    }
                }
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(checked=authorized,onCheckedChange={authorized=it})
                    Text(tr("Confirmo que tengo autorización para capturar en esta red","I confirm I am authorized to capture on this network"),
                        fontSize=12.sp,lineHeight=16.sp,modifier=Modifier.weight(1f))
                }
                val canCapture=!busy && !preferences.offlineOnly && authorized && s.captureAvailable &&
                    s.interfaces.any { it.name==selectedIface && it.monitor }
                Button(enabled=canCapture,onClick={
                    busy=true
                    scope.launch {
                        try {
                            latest=RemoteLinuxClient.capture(context,config(),selectedIface,duration)
                            info="Captura remota transferida y analizada: ${latest?.packets} paquetes. Abre Auditoría para ver el historial."
                            authorized=false
                        } catch(ex:Exception) {
                            info="Error de captura: ${ex.message ?: ex.javaClass.simpleName}"
                        } finally {busy=false}
                    }
                },modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=RemoteGreen,contentColor=Color.Black)) {
                    Text(if(busy) "Procesando…" else "Capturar e importar al historial")
                }
                latest?.let { result ->
                    Text(tr("Captura importada","Capture imported"),color=RemoteGreen,fontWeight=FontWeight.SemiBold)
                    RemoteHint("${result.format} · ${result.packets} paquetes · ${result.bytes} bytes")
                }
                RemoteHint("Una conexión verificada aquí no es una sesión SSH permanente; cada operación valida de nuevo el servidor y se autentica.")
            }
        }
    }
}
