package com.example.gymcontrol.ui.instructor.perfil

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.gymcontrol.R
import com.example.gymcontrol.data.remote.InstructorRepository
import com.example.gymcontrol.ui.components.ClienteScaffold
import com.example.gymcontrol.ui.components.SesionActual
import com.example.gymcontrol.ui.components.ScaffoldTab
import com.example.gymcontrol.ui.components.ClienteTema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val CardBorder = Brush.linearGradient(listOf(ClienteTema.Borde, ClienteTema.AzulSel))
private val InnerBackground = Color.Black.copy(alpha = 0.35f)

private val INSTRUCTOR_TABS = listOf(
    ScaffoldTab("Mis clientes", Icons.Filled.Group),
    ScaffoldTab("Perfil", Icons.Filled.Person)
)

// El nombre debe tener al menos `min` letras (espacios y números no cuentan)
private fun nameHasMinLetters(s: String, min: Int = 3) = s.count { it.isLetter() } >= min

@Composable
fun InstructorPerfilScreen(onNavigate: (String) -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(SesionActual.nombre ?: "") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(SesionActual.correo ?: "") }
    var photo by remember { mutableStateOf<ImageBitmap?>(null) }

    var activo by remember { mutableStateOf(true) }
    var capacity by remember { mutableStateOf(0) }
    var activeClients by remember { mutableStateOf(0) }

    // Datos reales del instructor que inició sesión
    LaunchedEffect(Unit) {
        val correo = SesionActual.correo ?: return@LaunchedEffect
        try {
            val p = InstructorRepository.perfil(correo)
            if (p != null) {
                name = p.nombre
                phone = p.telefono
                email = p.gmail
                activo = p.estado == "ACTIVO"
                capacity = p.maxClientes
                activeClients = p.activos
            }
        } catch (e: Exception) {
            val msg = e.message?.substringBefore("\nCode:") ?: ""
            Toast.makeText(context, "No se pudo cargar el perfil: $msg", Toast.LENGTH_LONG).show()
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var showPhotoDialog by remember { mutableStateOf(false) }

    // Galería (selector de fotos del sistema, no necesita permisos)
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bitmap = withContext(Dispatchers.IO) { loadBitmap(context, uri) }
                if (bitmap != null) photo = bitmap.asImageBitmap()
                else Toast.makeText(context, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Cámara (toma la foto en el momento)
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) photo = bitmap.asImageBitmap()
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch(null)
        else Toast.makeText(context, "Se necesita permiso de cámara para tomar la foto", Toast.LENGTH_SHORT).show()
    }

    ClienteScaffold(
        currentSection = "Perfil",
        onNavigate = onNavigate,
        logoRes = R.drawable.logo_final,
        tabs = INSTRUCTOR_TABS
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.perfil_title),
                    color = ClienteTema.Texto,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ClienteTema.Beige, RoundedCornerShape(14.dp))
                        .border(1.5.dp, CardBorder, RoundedCornerShape(14.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ProfileAvatar(
                        name = name,
                        photo = photo,
                        onEditPhoto = { showPhotoDialog = true }
                    )

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = name,
                        color = ClienteTema.Texto,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.perfil_status_label),
                            color = ClienteTema.TextoSec,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        if (activo) {
                            Chip(text = stringResource(R.string.status_active), color = ClienteTema.Verde, width = 84.dp, height = 30.dp)
                        } else {
                            Chip(text = stringResource(R.string.status_inactive), color = ClienteTema.Rojo, width = 96.dp, height = 30.dp)
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    ActionRow(
                        icon = Icons.Filled.Edit,
                        label = stringResource(R.string.perfil_action_edit_data),
                        onClick = { showEditDialog = true }
                    )

                    Spacer(Modifier.height(14.dp))

                    // Datos personales
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(InnerBackground, RoundedCornerShape(10.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.perfil_personal_data_title),
                            color = ClienteTema.Texto,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(ClienteTema.Borde)
                        )
                        Spacer(Modifier.height(12.dp))

                        InfoRow(icon = Icons.Filled.Person, label = stringResource(R.string.perfil_label_name), value = name)
                        Spacer(Modifier.height(14.dp))
                        InfoRow(icon = Icons.Filled.Phone, label = stringResource(R.string.perfil_label_phone), value = formatPhone(phone))
                        Spacer(Modifier.height(14.dp))
                        InfoRow(icon = Icons.Filled.Email, label = stringResource(R.string.perfil_label_email), value = email)
                        Spacer(Modifier.height(14.dp))

                        // Capacidad máxima de clientes
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Group,
                                contentDescription = null,
                                tint = ClienteTema.TextoSec,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.instructor_max_capacity_label),
                                color = ClienteTema.TextoSec,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = capacity.toString(),
                                color = ClienteTema.Texto,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(10.dp))
                            if (activeClients >= capacity) {
                                Chip(text = stringResource(R.string.instructor_capacity_full), color = ClienteTema.Rojo, width = 64.dp, height = 28.dp)
                            } else {
                                Chip(text = stringResource(R.string.instructor_capacity_available), color = ClienteTema.Verde, width = 90.dp, height = 28.dp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPhotoDialog) {
        PhotoDialog(
            hasPhoto = photo != null,
            onDismiss = { showPhotoDialog = false },
            onCamera = {
                showPhotoDialog = false
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) cameraLauncher.launch(null)
                else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onGallery = {
                showPhotoDialog = false
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onRemove = {
                showPhotoDialog = false
                photo = null
            }
        )
    }

    if (showEditDialog) {
        EditDataDialog(
            initialName = name,
            initialPhone = phone,
            initialEmail = email,
            onDismiss = { showEditDialog = false },
            onSave = { newName, newPhone, newEmail ->
                scope.launch {
                    try {
                        InstructorRepository.editarPerfil(email, newName.trim(), newPhone.trim(), newEmail.trim())
                        name = newName.trim()
                        phone = newPhone.trim()
                        email = newEmail.trim()
                        SesionActual.fijarCorreo(email)
                        SesionActual.fijarNombre(name)
                        showEditDialog = false
                        Toast.makeText(context, "Datos actualizados", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        val msg = e.message?.substringBefore("\nCode:") ?: ""
                        Toast.makeText(context, "No se pudo guardar: $msg", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

// "2487693720" -> "+52 248 769 3720"
private fun formatPhone(phone: String): String =
    if (phone.length == 10) {
        "+52 ${phone.substring(0, 3)} ${phone.substring(3, 6)} ${phone.substring(6)}"
    } else phone

// Lee la imagen reduciendo su tamaño y corrigiendo la rotación
private fun loadBitmap(context: Context, uri: Uri, maxSize: Int = 600): Bitmap? = try {
    val resolver = context.contentResolver

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

    var sample = 1
    while (bounds.outWidth / sample > maxSize * 2 || bounds.outHeight / sample > maxSize * 2) {
        sample *= 2
    }

    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

    val orientation = resolver.openInputStream(uri)?.use {
        ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } ?: ExifInterface.ORIENTATION_NORMAL

    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }

    if (bitmap != null && degrees != 0f) {
        Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height,
            Matrix().apply { postRotate(degrees) }, true
        )
    } else bitmap
} catch (e: Exception) {
    null
}

// Foto (o iniciales si no hay foto) con el lápiz para cambiarla
@Composable
private fun ProfileAvatar(name: String, photo: ImageBitmap?, onEditPhoto: () -> Unit) {
    Box(modifier = Modifier.size(124.dp)) {
        Box(
            modifier = Modifier
                .size(116.dp)
                .align(Alignment.BottomStart)
                .clip(CircleShape)
                .background(ClienteTema.AzulSel.copy(alpha = 0.18f), CircleShape)
                .border(3.dp, CardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (photo != null) {
                Image(
                    bitmap = photo,
                    contentDescription = stringResource(R.string.perfil_photo_desc),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = initials(name),
                    color = ClienteTema.Texto,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .size(34.dp)
                .align(Alignment.TopEnd)
                .background(ClienteTema.Borde, CircleShape)
                .clickable(onClick = onEditPhoto),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = stringResource(R.string.perfil_change_photo_desc),
                tint = ClienteTema.Texto,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun initials(name: String): String =
    name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }

@Composable
private fun ActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(InnerBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = ClienteTema.Texto, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            color = ClienteTema.Texto,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = ClienteTema.TextoSec,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = ClienteTema.TextoSec, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            color = ClienteTema.TextoSec,
            fontSize = 16.sp,
            modifier = Modifier.width(88.dp)
        )
        Text(
            text = value,
            color = ClienteTema.Texto,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

// Chip de estado con fondo tintado y borde del mismo color
@Composable
private fun Chip(
    text: String,
    color: Color,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .size(width, height)
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .border(1.dp, color, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ---------- Diálogos con el mismo estilo de las tarjetas (borde degradado) ----------

@Composable
private fun GymDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ClienteTema.Beige, RoundedCornerShape(14.dp))
                .border(1.5.dp, CardBorder, RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = title,
                    color = ClienteTema.Texto,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                )
                content()
            }

            // X para cerrar
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.common_close),
                    tint = ClienteTema.TextoSec,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun PhotoDialog(
    hasPhoto: Boolean,
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onRemove: () -> Unit
) {
    GymDialog(title = stringResource(R.string.perfil_photo_dialog_title), onDismiss = onDismiss) {
        ActionRow(icon = Icons.Filled.PhotoCamera, label = stringResource(R.string.perfil_photo_take), onClick = onCamera)
        ActionRow(icon = Icons.Filled.PhotoLibrary, label = stringResource(R.string.perfil_photo_gallery), onClick = onGallery)
        if (hasPhoto) {
            ActionRow(icon = Icons.Filled.Delete, label = stringResource(R.string.perfil_photo_remove), onClick = onRemove)
        }
    }
}

@Composable
private fun EditDataDialog(
    initialName: String,
    initialPhone: String,
    initialEmail: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var email by remember { mutableStateOf(initialEmail) }

    // Avisos que aparecen unos segundos cuando se intenta escribir un carácter no permitido
    var nameWarning by remember { mutableStateOf(false) }
    var nameTick by remember { mutableStateOf(0) }
    var phoneWarning by remember { mutableStateOf(false) }
    var phoneTick by remember { mutableStateOf(0) }

    LaunchedEffect(nameTick) {
        if (nameTick > 0) {
            nameWarning = true
            delay(2500)
            nameWarning = false
        }
    }
    LaunchedEffect(phoneTick) {
        if (phoneTick > 0) {
            phoneWarning = true
            delay(2500)
            phoneWarning = false
        }
    }

    // Reglas: nombre solo letras y espacios (mínimo 3 letras); teléfono exactamente 10 dígitos
    val nameValid = nameHasMinLetters(name)
    val nameShort = name.isNotEmpty() && !nameValid
    val phoneIncomplete = phone.isNotEmpty() && phone.length < 10
    val isValid = nameValid && phone.length == 10

    GymDialog(title = stringResource(R.string.perfil_edit_dialog_title), onDismiss = onDismiss) {
        GymTextField(
            value = name,
            onValueChange = { new ->
                when {
                    !new.all { it.isLetter() || it == ' ' } -> nameTick++
                    new.length <= 40 -> name = new
                }
            },
            label = stringResource(R.string.perfil_field_name),
            isError = nameWarning || nameShort,
            errorText = if (nameWarning) stringResource(R.string.perfil_error_letters_only) else stringResource(R.string.perfil_error_min_letters)
        )
        GymTextField(
            value = phone,
            onValueChange = { new ->
                when {
                    !new.all { it in '0'..'9' } -> phoneTick++
                    new.length <= 10 -> phone = new
                }
            },
            label = stringResource(R.string.perfil_field_phone),
            keyboardType = KeyboardType.Number,
            isError = phoneWarning || phoneIncomplete,
            errorText = if (phoneWarning) stringResource(R.string.perfil_error_phone_digits)
            else "Deben ser 10 dígitos (${phone.length}/10)"
        )
        GymTextField(
            value = email,
            onValueChange = { email = it },
            label = stringResource(R.string.perfil_field_email),
            keyboardType = KeyboardType.Email
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.Rojo),
                border = BorderStroke(1.dp, ClienteTema.Rojo)
            ) {
                Text(stringResource(R.string.cancel), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { onSave(name.trim(), phone, email) },
                enabled = isValid,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClienteTema.Azul,
                    contentColor = ClienteTema.Texto
                )
            ) {
                Text(stringResource(R.string.save), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GymTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = if (isError && errorText != null) {
            { Text(errorText) }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ClienteTema.AzulSel,
            unfocusedBorderColor = ClienteTema.Borde,
            focusedTextColor = ClienteTema.Texto,
            unfocusedTextColor = ClienteTema.Texto,
            focusedLabelColor = ClienteTema.AzulSel,
            unfocusedLabelColor = ClienteTema.TextoSec,
            cursorColor = ClienteTema.AzulSel,
            errorBorderColor = ClienteTema.Rojo,
            errorTextColor = ClienteTema.Texto,
            errorLabelColor = ClienteTema.Rojo,
            errorCursorColor = ClienteTema.Rojo,
            errorSupportingTextColor = ClienteTema.Rojo
        )
    )
}