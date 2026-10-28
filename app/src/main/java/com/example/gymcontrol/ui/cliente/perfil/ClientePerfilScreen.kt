package com.example.gymcontrol.ui.cliente.perfil

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.gymcontrol.R
import com.example.gymcontrol.data.remote.AuthRepository
import com.example.gymcontrol.data.remote.ClienteRow
import com.example.gymcontrol.data.remote.ClientesRepository
import com.example.gymcontrol.ui.components.SesionActual
import com.example.gymcontrol.ui.components.ClienteScaffoldClaro
import com.example.gymcontrol.ui.components.ClienteTema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun nameHasMinLetters(s: String, min: Int = 3) = s.count { it.isLetter() } >= min

@Composable
fun ClientePerfilScreen(onNavigate: (String) -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Datos reales del cliente en sesión (se cargan de clientes_lista)
    var cliente by remember { mutableStateOf<ClienteRow?>(null) }
    var name by remember { mutableStateOf(SesionActual.nombre ?: "") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(SesionActual.correo ?: "") }
    var photo by remember { mutableStateOf<ImageBitmap?>(null) }
    val membership = cliente?.numeroMembresia ?: ""

    LaunchedEffect(Unit) {
        val correo = SesionActual.correo ?: return@LaunchedEffect
        try {
            ClientesRepository.porCorreo(correo)?.let {
                cliente = it
                name = it.nombre
                phone = it.telefono
                email = it.gmail
            }
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudieron cargar tus datos", Toast.LENGTH_SHORT).show()
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showPhotoDialog by remember { mutableStateOf(false) }

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

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> if (bitmap != null) photo = bitmap.asImageBitmap() }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch(null)
        else Toast.makeText(context, "Se necesita permiso de cámara para tomar la foto", Toast.LENGTH_SHORT).show()
    }

    ClienteScaffoldClaro(
        currentSection = "Perfil",
        onNavigate = onNavigate,
        logoRes = R.drawable.logo_pi2
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
                    "Mi perfil",
                    color = ClienteTema.Texto,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                val forma = RoundedCornerShape(20.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ClienteTema.Beige, forma)
                        .border(2.dp, ClienteTema.Borde, forma)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ProfileAvatar(name = name, photo = photo, onEditPhoto = { showPhotoDialog = true })

                    Spacer(Modifier.height(12.dp))
                    Text(name, color = ClienteTema.Texto, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text("Membresía : $membership", color = ClienteTema.Texto, fontSize = 15.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Estado:", color = ClienteTema.Texto, fontSize = 15.sp)
                        Spacer(Modifier.width(8.dp))
                        StatusBadge(isActive = cliente?.let { it.activa && it.estado == "ACTIVO" } ?: true)
                    }
                    cliente?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Plan: ${it.plan ?: "—"} · Vence: ${ClientesRepository.fechaApp(it.fechaFin)}",
                            color = ClienteTema.TextoSec,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    ActionRow(Icons.Filled.Edit, "Editar datos") { showEditDialog = true }
                    Spacer(Modifier.height(8.dp))
                    ActionRow(Icons.Filled.Lock, "Cambiar contraseña") { showPasswordDialog = true }

                    Spacer(Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ClienteTema.Navy, RoundedCornerShape(10.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            "Datos Personales",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.5f))
                        )
                        Spacer(Modifier.height(12.dp))
                        InfoRow(Icons.Filled.Person, "Nombre:", name)
                        Spacer(Modifier.height(14.dp))
                        InfoRow(Icons.Filled.Phone, "Teléfono:", phone)
                        Spacer(Modifier.height(14.dp))
                        InfoRow(Icons.Filled.Email, "Correo:", email)
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
                val c = cliente
                if (c == null) {
                    Toast.makeText(context, "Aún no se cargan tus datos", Toast.LENGTH_SHORT).show()
                    return@EditDataDialog
                }
                scope.launch {
                    try {
                        ClientesRepository.editar(c.id, newName.trim(), newPhone.trim(), newEmail.trim(), c.estado)
                        name = newName.trim()
                        phone = newPhone.trim()
                        email = newEmail.trim()
                        cliente = c.copy(nombre = name, telefono = phone, gmail = email)
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

    if (showPasswordDialog) {
        PasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onSave = { actual, nueva ->
                scope.launch {
                    try {
                        AuthRepository.cambiarContrasena(email, actual, nueva)
                        showPasswordDialog = false
                        Toast.makeText(context, "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        val msg = e.message?.substringBefore("\nCode:") ?: ""
                        Toast.makeText(context, "No se pudo cambiar: $msg", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

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

@Composable
private fun ProfileAvatar(name: String, photo: ImageBitmap?, onEditPhoto: () -> Unit) {
    Box(modifier = Modifier.size(124.dp)) {
        Box(
            modifier = Modifier
                .size(116.dp)
                .align(Alignment.BottomStart)
                .clip(CircleShape)
                .background(Color(0xFFB9B5A6), CircleShape)
                .border(3.dp, ClienteTema.Azul, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (photo != null) {
                Image(
                    bitmap = photo,
                    contentDescription = "Foto de perfil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    initials(name),
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
                .background(Color(0xFF8A8A8A), CircleShape)
                .clickable(onClick = onEditPhoto),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = "Cambiar foto",
                tint = Color.White,
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
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ClienteTema.Navy)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp, modifier = Modifier.width(88.dp))
        Text(
            value,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatusBadge(isActive: Boolean) {
    val color = if (isActive) ClienteTema.Verde else ClienteTema.Rojo
    Box(
        modifier = Modifier
            .size(64.dp, 24.dp)
            .background(color, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (isActive) "Activo" else "Inactivo",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ---------- Diálogos ----------

@Composable
private fun GymDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ClienteTema.Beige, RoundedCornerShape(16.dp))
                .border(1.5.dp, ClienteTema.Borde, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, color = ClienteTema.Texto, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun DialogButtons(
    confirmText: String,
    confirmEnabled: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f).height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.Rojo),
            border = BorderStroke(1.dp, ClienteTema.Rojo)
        ) { Text("Cancelar", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        Button(
            onClick = onConfirm,
            enabled = confirmEnabled,
            modifier = Modifier.weight(1f).height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ClienteTema.Azul,
                contentColor = Color.White
            )
        ) { Text(confirmText, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
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
    GymDialog(title = "Foto de perfil", onDismiss = onDismiss) {
        ActionRow(Icons.Filled.PhotoCamera, "Tomar foto", onCamera)
        ActionRow(Icons.Filled.PhotoLibrary, "Elegir de la galería", onGallery)
        if (hasPhoto) ActionRow(Icons.Filled.Delete, "Quitar foto", onRemove)
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.Rojo),
            border = BorderStroke(1.dp, ClienteTema.Rojo)
        ) { Text("Cancelar", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
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

    var nameWarning by remember { mutableStateOf(false) }
    var nameTick by remember { mutableStateOf(0) }
    var phoneWarning by remember { mutableStateOf(false) }
    var phoneTick by remember { mutableStateOf(0) }

    LaunchedEffect(nameTick) {
        if (nameTick > 0) { nameWarning = true; delay(2500); nameWarning = false }
    }
    LaunchedEffect(phoneTick) {
        if (phoneTick > 0) { phoneWarning = true; delay(2500); phoneWarning = false }
    }

    val nameValid = nameHasMinLetters(name)
    val nameShort = name.isNotEmpty() && !nameValid
    val phoneIncomplete = phone.isNotEmpty() && phone.length < 10
    val isValid = nameValid && phone.length == 10

    GymDialog(title = "Editar datos", onDismiss = onDismiss) {
        GymTextField(
            value = name,
            onValueChange = { new ->
                when {
                    !new.all { it.isLetter() || it == ' ' } -> nameTick++
                    new.length <= 40 -> name = new
                }
            },
            label = "Nombre",
            isError = nameWarning || nameShort,
            errorText = if (nameWarning) "Solo se permiten letras" else "Mínimo 3 letras"
        )
        GymTextField(
            value = phone,
            onValueChange = { new ->
                when {
                    !new.all { it in '0'..'9' } -> phoneTick++
                    new.length <= 10 -> phone = new
                }
            },
            label = "Teléfono",
            keyboardType = KeyboardType.Number,
            isError = phoneWarning || phoneIncomplete,
            errorText = if (phoneWarning) "Solo se permiten números"
            else "Deben ser 10 dígitos (${phone.length}/10)"
        )
        GymTextField(
            value = email,
            onValueChange = { email = it },
            label = "Correo",
            keyboardType = KeyboardType.Email
        )

        DialogButtons(
            confirmText = "Guardar",
            confirmEnabled = isValid,
            onConfirm = { onSave(name.trim(), phone, email) },
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun PasswordDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Mínimo 8 caracteres y que la confirmación coincida
    val tooShort = newPassword.isNotEmpty() && newPassword.length < 8
    val mismatch = confirmPassword.isNotEmpty() && confirmPassword != newPassword
    val canSave = currentPassword.isNotEmpty() && newPassword.length >= 8 && confirmPassword == newPassword

    GymDialog(title = "Cambiar contraseña", onDismiss = onDismiss) {
        GymTextField(
            value = currentPassword,
            onValueChange = { currentPassword = it },
            label = "Contraseña actual",
            isPassword = true
        )
        GymTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = "Nueva contraseña",
            isPassword = true,
            isError = tooShort,
            errorText = "Mínimo 8 caracteres"
        )
        GymTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Confirmar contraseña",
            isPassword = true,
            isError = mismatch,
            errorText = "Las contraseñas no coinciden"
        )
        DialogButtons(
            confirmText = "Actualizar",
            confirmEnabled = canSave,
            onConfirm = { onSave(currentPassword, newPassword) },
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun GymTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
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
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ClienteTema.Azul,
            unfocusedBorderColor = ClienteTema.Borde,
            focusedTextColor = ClienteTema.Texto,
            unfocusedTextColor = ClienteTema.Texto,
            focusedLabelColor = ClienteTema.Azul,
            unfocusedLabelColor = ClienteTema.TextoSec,
            cursorColor = ClienteTema.Azul,
            errorBorderColor = ClienteTema.Rojo,
            errorTextColor = ClienteTema.Texto,
            errorLabelColor = ClienteTema.Rojo,
            errorCursorColor = ClienteTema.Rojo,
            errorSupportingTextColor = ClienteTema.Rojo
        )
    )
}