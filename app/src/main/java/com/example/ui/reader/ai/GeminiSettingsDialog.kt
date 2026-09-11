package com.example.ui.reader.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.gemini.GeminiRepository
import com.example.data.gemini.GeminiSettingsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiSettingsSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        modifier = modifier.testTag("gemini_settings_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Gemini AI",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        )
                        Text(
                            text = "Yapay Zeka API Ayarları",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            // Body content
            GeminiAiSettingsSection()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Reusable Gemini AI Settings Section that can be placed in GeminiSettingsSheet
 * as well as inside ReaderSettingsSheet or Library settings.
 */
@Composable
fun GeminiAiSettingsSection(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val geminiRepository = remember { GeminiRepository(context) }

    var hasKey by remember { mutableStateOf(GeminiSettingsManager.hasApiKey(context)) }
    var maskedKey by remember { mutableStateOf(GeminiSettingsManager.getMaskedApiKey(context)) }
    var isEditing by remember { mutableStateOf(!hasKey) }
    var inputKey by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // Feedback & Test states
    var statusFeedback by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var testResultSuccess by remember { mutableStateOf<Boolean?>(null) }
    var testResultMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("gemini_ai_settings_section")
    ) {
        // Section Title & Description
        Text(
            text = "Gemini AI",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Kişisel Gemini API anahtarınızı buraya kaydederek kitap okurken yapay zeka özelliklerini kullanabilirsiniz. Anahtarınız cihazınızda güvenle yerel olarak saklanır.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // API Key state: Either Display Saved Key or Display Input Field
        if (hasKey && !isEditing) {
            // Already Saved Key Display
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "API anahtarı kayıtlı",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = maskedKey ?: "••••••••••••••••",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Buttons: "API Key'i değiştir" and "API Key'i sil"
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                isEditing = true
                                inputKey = ""
                                testResultSuccess = null
                                testResultMessage = null
                                statusFeedback = null
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("change_api_key_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("API Key'i değiştir", fontSize = 12.sp)
                        }

                        FilledTonalButton(
                            onClick = {
                                GeminiSettingsManager.deleteApiKey(context)
                                hasKey = GeminiSettingsManager.hasApiKey(context)
                                maskedKey = GeminiSettingsManager.getMaskedApiKey(context)
                                isEditing = true
                                testResultSuccess = null
                                testResultMessage = null
                                statusFeedback = "API anahtarı silindi."
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("delete_api_key_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("API Key'i sil", fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            // API Key Input Field (User can paste API Key)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "API Key",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = inputKey,
                    onValueChange = {
                        inputKey = it
                        testResultSuccess = null
                        testResultMessage = null
                    },
                    placeholder = { Text("Gemini API anahtarınızı yapıştırın…", fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_api_key_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    leadingIcon = {
                        Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) "Gizle" else "Göster"
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Action buttons: "Kaydet" and optional "Vazgeç"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (hasKey) {
                        OutlinedButton(
                            onClick = {
                                isEditing = false
                                inputKey = ""
                                testResultSuccess = null
                                testResultMessage = null
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Vazgeç")
                        }
                    }

                    Button(
                        onClick = {
                            val trimmed = inputKey.trim()
                            if (trimmed.isNotBlank()) {
                                GeminiSettingsManager.saveApiKey(context, trimmed)
                                hasKey = true
                                maskedKey = GeminiSettingsManager.getMaskedApiKey(context)
                                isEditing = false
                                inputKey = ""
                                statusFeedback = "API anahtarı başarıyla kaydedildi."
                                testResultSuccess = null
                                testResultMessage = null
                            }
                        },
                        enabled = inputKey.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("save_api_key_button")
                    ) {
                        Text("Kaydet", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Status Feedback (Saved / Deleted info)
        if (statusFeedback != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statusFeedback!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))

        // =========================================================================
        // "Gemini bağlantısını test et" BUTONU
        // =========================================================================
        OutlinedButton(
            onClick = {
                // API key boşsa API isteği gönderme kuralı
                val effectiveKey = if (isEditing && inputKey.isNotBlank()) {
                    inputKey.trim()
                } else {
                    GeminiSettingsManager.getEffectiveApiKey(context)
                }

                if (effectiveKey.isNullOrBlank()) {
                    testResultSuccess = false
                    testResultMessage = "API anahtarınızı ve internet bağlantınızı kontrol edin."
                    return@OutlinedButton
                }

                isTestingConnection = true
                testResultSuccess = null
                testResultMessage = null

                coroutineScope.launch {
                    val result = geminiRepository.testConnection(effectiveKey)
                    isTestingConnection = false
                    result.onSuccess {
                        testResultSuccess = true
                        testResultMessage = "Gemini bağlantısı başarılı."
                    }.onFailure {
                        testResultSuccess = false
                        testResultMessage = "Gemini bağlantısı kurulamadı. API anahtarınızı ve internet bağlantınızı kontrol edin."
                    }
                }
            },
            enabled = !isTestingConnection,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("test_gemini_connection_button")
        ) {
            if (isTestingConnection) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Bağlantı test ediliyor…", fontSize = 13.sp)
            } else {
                Icon(
                    imageVector = Icons.Default.NetworkCheck,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gemini bağlantısını test et",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp
                )
            }
        }

        // Test Sonucu Bildirimi (Başarılı / Başarısız)
        AnimatedVisibility(visible = testResultMessage != null) {
            if (testResultMessage != null) {
                val isSuccess = testResultSuccess == true
                val bgColor = if (isSuccess) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                val contentColor = if (isSuccess) Color(0xFF1B5E20) else MaterialTheme.colorScheme.error

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = bgColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_test_result_surface")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = testResultMessage!!,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            ),
                            color = contentColor,
                            modifier = Modifier.testTag("gemini_test_result_text")
                        )
                    }
                }
            }
        }
    }
}
