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
import androidx.compose.ui.graphics.Brush
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
import com.example.gymcontrol.ui.components.ClienteScaffold
import com.example.gymcontrol.ui.theme.GymColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val CardBorder = Brush.linearGradient(listOf(GymColors.Purple, GymColors.Gold))
private val InnerBackground = Color.Black.copy(alpha = 0.35f)

// El nombre debe tener al menos `min` letras (espacios y números no cuentan)
private fun nameHasMinLetters(s: String, min: Int = 3) = s.count { it.isLetter() } >= min

@Composable
fun ClientePerfilScreen(onNavigate: (String) -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("Ana López") }
    var phone by remember { mutableStateOf("3121111111") }
    var email by remember { mutableStateOf("ana@email.com") }
    var photo by remember { mutableStateOf<ImageBitmap?>(null) }
    val membership = "M-1001"

    var showEditDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
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
        logoRes = R.drawable.logo_axolotl
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
                    text = "Mi perfil",
                    color = GymColors.TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GymColors.Surface, RoundedCornerShape(14.dp))
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
                        color = GymColors.TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Membresía : $membership",
                        color = GymColors.TextSecondary,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Estado:",
                            color = GymColors.TextSecondary,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        StatusBadge(isActive = true)
                    }

                    Spacer(Modifier.height(18.dp))

                    ActionRow(
                        icon = Icons.Filled.Edit,
                        label = "Editar datos",
                        onClick = { showEditDialog = true }
                    )
                    Spacer(Modifier.height(10.dp))
                    ActionRow(
                        icon = Icons.Filled.Lock,
                        label = "Cambiar contraseña",
                        onClick = { showPasswordDialog = true }
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
                            text = "Datos Personales",
                            color = GymColors.TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(GymColors.Border)
                        )
                        Spacer(Modifier.height(12.dp))

                        InfoRow(icon = Icons.Filled.Person, label = "Nombre:", value = name)
                        Spacer(Modifier.height(14.dp))
                        InfoRow(icon = Icons.Filled.Phone, label = "Teléfono:", value = phone)
                        Spacer(Modifier.height(14.dp))
                        InfoRow(icon = Icons.Filled.Email, label = "Correo:", value = email)
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
                // TODO: guardar cambios en Supabase
                name = newName
                phone = newPhone
                email = newEmail
                showEditDialog = false
            }
        )
    }

    if (showPasswordDialog) {
        PasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onSave = { /* TODO: actualizar contraseña en Supabase */ showPasswordDialog = false }
        )
    }
}

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
                .background(GymColors.Purple.copy(alpha = 0.18f), CircleShape)
                .border(3.dp, CardBorder, CircleShape),
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
                    text = initials(name),
                    color = GymColors.TextPrimary,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .size(34.dp)
                .align(Alignment.TopEnd)
                .background(GymColors.Border, CircleShape)
                .clickable(onClick = onEditPhoto),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Cambiar foto",
                tint = GymColors.TextPrimary,
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
        Icon(icon, contentDescription = null, tint = GymColors.TextPrimary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            color = GymColors.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = GymColors.TextSecondary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = GymColors.TextSecondary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            color = GymColors.TextSecondary,
            fontSize = 16.sp,
            modifier = Modifier.width(88.dp)
        )
        Text(
            text = value,
            color = GymColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatusBadge(isActive: Boolean) {
    val color = if (isActive) GymColors.Green else GymColors.Red
    val label = if (isActive) "Activo" else "Inactivo"

    Box(
        modifier = Modifier
            .size(84.dp, 30.dp)
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .border(1.dp, color, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(GymColors.Surface, RoundedCornerShape(14.dp))
                .border(1.5.dp, CardBorder, RoundedCornerShape(14.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                color = GymColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
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
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = GymColors.Red),
            border = BorderStroke(1.dp, GymColors.Red)
        ) {
            Text("Cancelar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onConfirm,
            enabled = confirmEnabled,
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GymColors.Purple,
                contentColor = GymColors.TextPrimary
            )
        ) {
            Text(confirmText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
    GymDialog(title = "Foto de perfil", onDismiss = onDismiss) {
        ActionRow(icon = Icons.Filled.PhotoCamera, label = "Tomar foto", onClick = onCamera)
        ActionRow(icon = Icons.Filled.PhotoLibrary, label = "Elegir de la galería", onClick = onGallery)
        if (hasPhoto) {
            ActionRow(icon = Icons.Filled.Delete, label = "Quitar foto", onClick = onRemove)
        }
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = GymColors.Red),
            border = BorderStroke(1.dp, GymColors.Red)
        ) {
            Text("Cancelar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
private fun PasswordDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }

    GymDialog(title = "Cambiar contraseña", onDismiss = onDismiss) {
        GymTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = "Nueva contraseña",
            isPassword = true
        )

        DialogButtons(
            confirmText = "Actualizar",
            onConfirm = { onSave(newPassword) },
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
            focusedBorderColor = GymColors.Purple,
            unfocusedBorderColor = GymColors.Border,
            focusedTextColor = GymColors.TextPrimary,
            unfocusedTextColor = GymColors.TextPrimary,
            focusedLabelColor = GymColors.Purple,
            unfocusedLabelColor = GymColors.TextSecondary,
            cursorColor = GymColors.Gold,
            errorBorderColor = GymColors.Red,
            errorTextColor = GymColors.TextPrimary,
            errorLabelColor = GymColors.Red,
            errorCursorColor = GymColors.Red,
            errorSupportingTextColor = GymColors.Red
        )
    )
}