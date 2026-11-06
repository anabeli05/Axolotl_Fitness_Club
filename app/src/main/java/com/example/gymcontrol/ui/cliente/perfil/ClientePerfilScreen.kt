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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
            Toast.makeText(context, context.getString(R.string.perfil_load_error), Toast.LENGTH_SHORT).show()
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
                else Toast.makeText(context, context.getString(R.string.perfil_image_load_error), Toast.LENGTH_SHORT).show()
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
        else Toast.makeText(context, context.getString(R.string.perfil_camera_permission_required), Toast.LENGTH_SHORT).show()
    }

    ClienteScaffoldClaro(
        currentSection = "Perfil",
        onNavigate = onNavigate,
        logoRes = R.drawable.logo_final
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
                    stringResource(R.string.perfil_title),
                    color = ClienteTema.Texto,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
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
                    Text("${stringResource(R.string.label_membership)} : $membership", color = ClienteTema.Texto, fontSize = 15.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.perfil_status_label), color = ClienteTema.Texto, fontSize = 15.sp)
                        Spacer(Modifier.width(8.dp))
                        StatusBadge(isActive = cliente?.let { it.activa && it.estado == "ACTIVO" } ?: true)
                    }
                    cliente?.let {
                        Spacer(Modifier.height(4.dp))
                        val planName = translatedPlanName(it.plan)
                        val planPrefix = stringResource(R.string.label_plan_prefix, planName)
                        val expiresLabel = stringResource(R.string.label_expires)
                        Text(
                            "$planPrefix · $expiresLabel: ${ClientesRepository.fechaApp(it.fechaFin)}",
                            color = ClienteTema.TextoSec,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    ActionRow(Icons.Filled.Edit, stringResource(R.string.perfil_action_edit_data)) { showEditDialog = true }
                    Spacer(Modifier.height(8.dp))
                    ActionRow(Icons.Filled.Lock, stringResource(R.string.perfil_action_change_password)) { showPasswordDialog = true }

                    Spacer(Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ClienteTema.Navy, RoundedCornerShape(10.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            stringResource(R.string.perfil_personal_data_title),
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
                        InfoRow(Icons.Filled.Person, stringResource(R.string.perfil_label_name), name)
                        Spacer(Modifier.height(14.dp))
                        InfoRow(Icons.Filled.Phone, stringResource(R.string.perfil_label_phone), phone)
                        Spacer(Modifier.height(14.dp))
                        InfoRow(Icons.Filled.Email, stringResource(R.string.perfil_label_email), email)
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
            name = name,
            initialPhone = phone,
            email = email,
            onDismiss = { showEditDialog = false },
            onSave = { newPhone ->
                val c = cliente
                if (c == null) {
                    Toast.makeText(context, context.getString(R.string.perfil_not_loaded_yet), Toast.LENGTH_SHORT).show()
                    return@EditDataDialog
                }
                scope.launch {
                    try {
                        // Solo el teléfono es editable; nombre y correo se mandan como están
                        ClientesRepository.editar(c.id, c.nombre, newPhone.trim(), c.gmail, c.estado)
                        phone = newPhone.trim()
                        cliente = c.copy(telefono = phone)
                        showEditDialog = false
                        Toast.makeText(context, context.getString(R.string.perfil_data_updated), Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        val msg = e.message?.substringBefore("\nCode:") ?: ""
                        Toast.makeText(context, "${context.getString(R.string.error_change_failed)}: $msg", Toast.LENGTH_LONG).show()
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
                        Toast.makeText(context, context.getString(R.string.perfil_password_updated), Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        val msg = e.message?.substringBefore("\nCode:") ?: ""
                        Toast.makeText(context, "${context.getString(R.string.error_change_failed)}: $msg", Toast.LENGTH_LONG).show()
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
                    contentDescription = stringResource(R.string.perfil_photo_desc),
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
                contentDescription = stringResource(R.string.perfil_change_photo_desc),
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
            if (isActive) stringResource(R.string.status_active) else stringResource(R.string.status_inactive),
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
            Text(
                title,
                color = ClienteTema.Texto,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
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
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f).height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.Rojo),
            border = BorderStroke(1.dp, ClienteTema.Rojo)
        ) { Text(stringResource(R.string.cancel), fontSize = 16.sp, fontWeight = FontWeight.Bold) }
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
    GymDialog(title = stringResource(R.string.perfil_photo_dialog_title), onDismiss = onDismiss) {
        ActionRow(Icons.Filled.PhotoCamera, stringResource(R.string.perfil_photo_take), onCamera)
        ActionRow(Icons.Filled.PhotoLibrary, stringResource(R.string.perfil_photo_gallery), onGallery)
        if (hasPhoto) ActionRow(Icons.Filled.Delete, stringResource(R.string.perfil_photo_remove), onRemove)
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.Rojo),
            border = BorderStroke(1.dp, ClienteTema.Rojo)
        ) { Text(stringResource(R.string.cancel), fontSize = 16.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun EditDataDialog(
    name: String,
    initialPhone: String,
    email: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var phone by remember { mutableStateOf(initialPhone) }

    var phoneWarning by remember { mutableStateOf(false) }
    var phoneTick by remember { mutableStateOf(0) }

    LaunchedEffect(phoneTick) {
        if (phoneTick > 0) { phoneWarning = true; delay(2500); phoneWarning = false }
    }

    val phoneIncomplete = phone.isNotEmpty() && phone.length < 10
    val isValid = phone.length == 10

    GymDialog(title = stringResource(R.string.perfil_edit_dialog_title), onDismiss = onDismiss) {
        // Nombre y correo: solo lectura
        GymTextField(
            value = name,
            onValueChange = {},
            label = stringResource(R.string.perfil_field_name),
            enabled = false
        )
        GymTextField(
            value = email,
            onValueChange = {},
            label = stringResource(R.string.perfil_field_email),
            enabled = false
        )

        // Único campo editable
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

        DialogButtons(
            confirmText = stringResource(R.string.save),
            confirmEnabled = isValid,
            onConfirm = { onSave(phone) },
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

    GymDialog(title = stringResource(R.string.change_password), onDismiss = onDismiss) {
        GymTextField(
            value = currentPassword,
            onValueChange = { currentPassword = it },
            label = stringResource(R.string.current_password),
            isPassword = true
        )
        GymTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = stringResource(R.string.new_password),
            isPassword = true,
            isError = tooShort,
            errorText = stringResource(R.string.perfil_password_min_length)
        )
        GymTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = stringResource(R.string.perfil_confirm_password),
            isPassword = true,
            isError = mismatch,
            errorText = stringResource(R.string.passwords_dont_match)
        )
        DialogButtons(
            confirmText = stringResource(R.string.perfil_confirm_update),
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
    errorText: String? = null,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
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
            errorSupportingTextColor = ClienteTema.Rojo,
            // Campos de solo lectura (nombre y correo)
            disabledTextColor = ClienteTema.TextoSec.copy(alpha = 0.6f),
            disabledBorderColor = ClienteTema.Borde.copy(alpha = 0.4f),
            disabledLabelColor = ClienteTema.TextoSec.copy(alpha = 0.5f)
        )
    )
}

@Composable
private fun translatedPlanName(plan: String?): String {
    return when (plan?.lowercase()?.trim()) {
        "trimestral", "quarterly" -> stringResource(R.string.plan_quarterly)
        "mensual", "monthly" -> stringResource(R.string.plan_monthly)
        "visita", "día", "day", "day pass" -> stringResource(R.string.plan_day)
        else -> plan ?: "—"
    }
}