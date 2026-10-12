package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.ai.AiImageGeneratorService
import com.example.data.model.AppThemeData
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageGeneratorDialog(
    theme: AppThemeData,
    onDismiss: () -> Unit,
    onApplyToCover: (String) -> Unit,
    onInsertIntoPage: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var promptText by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("REALISTIC") }
    var selectedRatio by remember { mutableStateOf("1:1") }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedImageFile by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val styles = listOf(
        Triple("REALISTIC", "واقعي 🎬", "سينمائي ودرامي عالي الدقة"),
        Triple("CARTOON", "كارتون 🎨", "رسوم كوميكس وألوان حيوية"),
        Triple("CLASSIC", "كلاسيكي 🏛️", "لوحة زيتية وتراثية فاخرة"),
        Triple("SCI_FI", "خيال علمي 🚀", "مستقبلي ونيون وسبراني"),
        Triple("FANTASY", "فنتازيا 🐉", "أساطير وسحر وقلاع")
    )

    val ratios = listOf(
        "1:1" to "مربع (1:1)",
        "9:16" to "غلاف طولي (9:16)",
        "16:9" to "عريض (16:9)",
        "3:4" to "كتاب قياسي (3:4)"
    )

    val samplePrompts = listOf(
        "غلاف رواية ملحمية في صحراء عربية تحت سماء مرصعة بالنجوم",
        "محارب حكيم بعباءة مطرزة أمام بوابة قلعة أثرية",
        "مدينة عربية مستقبلية في عام 2150 بأبراج زجاجية معلقة",
        "مخطوطة سحرية قديمة تشع نوراً أزرق على طاولة خشبية"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(theme.primaryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                        }
                        Column {
                            Text(
                                text = "توليد الصور بالذكاء الاصطناعي 🎨",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.textColor
                            )
                            Text(
                                text = "محرك Google Imagen لتصميم الأغلفة ولوحات الكوميكس",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.3f))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Prompt Input
                    Text("1. وصف الصورة أو المشهد (Prompt):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("اكتب وصفاً تخيلياً دقيقاً لما ترغب في رسمه...", fontSize = 12.sp) },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Quick Prompt Chips
                    Text("أفكار سريعة ومقترحة:", fontSize = 11.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        samplePrompts.take(2).forEach { sample ->
                            SuggestionChip(
                                onClick = { promptText = sample },
                                label = { Text(sample.take(22) + "...", fontSize = 10.sp) }
                            )
                        }
                    }

                    // Style Selection
                    Text("2. النمط الفني (Art Style):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        styles.forEach { (styleKey, label, _) ->
                            val isChosen = selectedStyle == styleKey
                            FilterChip(
                                selected = isChosen,
                                onClick = { selectedStyle = styleKey },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = theme.primaryColor,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Aspect Ratio
                    Text("3. نسبة الأبعاد (Aspect Ratio):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ratios.forEach { (ratioKey, label) ->
                            val isChosen = selectedRatio == ratioKey
                            FilterChip(
                                selected = isChosen,
                                onClick = { selectedRatio = ratioKey },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = theme.primaryColor,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Generate Button
                    Button(
                        onClick = {
                            if (promptText.isNotBlank()) {
                                isGenerating = true
                                errorMessage = null
                                coroutineScope.launch {
                                    val res = AiImageGeneratorService.generateImage(
                                        context = context,
                                        prompt = promptText,
                                        style = selectedStyle,
                                        aspectRatio = selectedRatio
                                    )
                                    res.onSuccess {
                                        generatedImageFile = it
                                    }.onFailure {
                                        errorMessage = it.localizedMessage ?: "حدث خطأ أثناء التوليد"
                                    }
                                    isGenerating = false
                                }
                            }
                        },
                        enabled = promptText.isNotBlank() && !isGenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("جارٍ رسم وتوليد اللوحة الفنية...", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.Brush, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("توليد الرسمة الآن ✨", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    if (errorMessage != null) {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    // Shimmer Skeleton Loader (During AI Generation)
                    AnimatedVisibility(visible = isGenerating) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black.copy(alpha = 0.04f))
                                .border(1.5.dp, theme.primaryColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = theme.primaryColor)
                                Text("محرك Google Imagen يرسم اللوحة الآن...", fontSize = 12.sp, color = theme.primaryColor, fontWeight = FontWeight.Bold)
                            }
                            ShimmerPlaceholder(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }
                    }

                    // Preview Area
                    AnimatedVisibility(visible = generatedImageFile != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black.copy(alpha = 0.04f))
                                .border(1.5.dp, theme.primaryColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("معاينة النتيجة المولدة:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = theme.primaryColor)

                            AsyncImage(
                                model = generatedImageFile,
                                contentDescription = "الصورة المولدة",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Fit
                            )

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        generatedImageFile?.let {
                                            onApplyToCover(it.absolutePath)
                                            onDismiss()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تعيين كغلاف 📖", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        generatedImageFile?.let {
                                            onInsertIntoPage(it.absolutePath)
                                            onDismiss()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp), tint = theme.primaryColor)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إدراج بالصفحة 🖼️", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerPlaceholder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    val shimmerColors = listOf(
        Color(0xFFE2E8F0),
        Color(0xFFF8FAFC),
        Color(0xFFE2E8F0)
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 400f, translateAnim - 400f),
        end = Offset(translateAnim, translateAnim)
    )
    Box(modifier = modifier.background(brush))
}
