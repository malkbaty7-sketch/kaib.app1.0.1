package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.availableEmotions
import com.example.data.model.defaultWritingTemplates

data class ProjectCreationType(
    val key: String,
    val name: String,
    val description: String,
    val defaultPages: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val paperColorHex: String,
    val previewLayout: String // "BOOK", "RESEARCH", "SLIDES", "COMIC", "MAGAZINE", "NOVEL", "DOC", "BROCHURE"
)

@Composable
fun NewProjectDialog(
    theme: com.example.data.model.AppThemeData,
    onDismiss: () -> Unit,
    onCreate: (title: String, type: String, templateId: String, emotions: List<String>) -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(1) } // 1: Type & Template preview, 2: Layout & Paper customization, 3: Details & Finalize

    val all9ProjectTypes = remember {
        listOf(
            ProjectCreationType("E_BOOK", "1- إنشاء كتاب إلكتروني", "تصميم ونشر الكتب الرقمية المتوافقة مع قارئات الكتب", 12, Icons.Default.MenuBook, "#FFFDD0", "BOOK"),
            ProjectCreationType("SCIENTIFIC_RESEARCH", "2- إنشاء بحث علمي", "هيكل أكاديمي محكم (مقدمة، منهجية، نتائج، مراجع)", 15, Icons.Default.Science, "#FFFFFF", "RESEARCH"),
            ProjectCreationType("PRACTICAL_RESEARCH", "3- إنشاء بحث عملي", "تقارير ودراسات ميدانية وتحليل بيانات واقعية", 10, Icons.Default.Assessment, "#F8F9FA", "RESEARCH"),
            ProjectCreationType("PRESENTATION_SLIDES", "4- عرض تقديمي باور بوينت", "شرائح أفقية مقسمة لتقديم الأفكار بصرياً وبإيجاز", 8, Icons.Default.Slideshow, "#FFFFF0", "SLIDES"),
            ProjectCreationType("CHILDREN_COMIC", "5- كتاب قصص مصورة للأطفال", "مربعات حوارية ورسومات ملونة ونصوص بسيطة معبرة", 6, Icons.Default.ColorLens, "#FFF5F5", "COMIC"),
            ProjectCreationType("MAGAZINE", "6- إنشاء مجلة", "تصميم أعمدة متعددة ومقالات وعناوين بارزة ملونة", 16, Icons.Default.ViewAgenda, "#F0FFF4", "MAGAZINE"),
            ProjectCreationType("NOVEL", "7- كتابة رواية", "سرد روائي وأقسام فصول وخط زمني وبناء شخصيات", 20, Icons.Default.AutoStories, "#FBF0D9", "NOVEL"),
            ProjectCreationType("WORD_DOC", "8- مستند نصي (وورد)", "تحرير وتنسيق رسمي للمستندات والخطابات والتقارير", 5, Icons.Default.Description, "#FFFFFF", "DOC"),
            ProjectCreationType("BROCHURE", "9- برشورات مطوية", "مطوية ثلاثية أو ثنائية الأوجه للمعلومات والتسويق", 4, Icons.Default.DashboardCustomize, "#F5F3FF", "BROCHURE")
        )
    }

    var selectedType by remember { mutableStateOf(all9ProjectTypes[0]) }
    var selectedTemplateId by remember { mutableStateOf("novel_action") }
    val selectedEmotions = remember { mutableStateListOf("warmth", "motivation") }
    var title by remember { mutableStateOf("") }
    var selectedFont by remember { mutableStateOf("Cairo") }

    // Page controls & visual customization
    var pageCount by remember { mutableIntStateOf(selectedType.defaultPages) }
    var paperColorHex by remember { mutableStateOf(selectedType.paperColorHex) }
    var numberingStyle by remember { mutableStateOf("عربي تقليدي (١، ٢، ٣)") }
    var hasCoverImage by remember { mutableStateOf(true) }
    var hasCustomCloneModel by remember { mutableStateOf(false) }
    var customCloneModelName by remember { mutableStateOf("") }

    val calmingPaperColors = listOf(
        Pair("كريمي دافئ", "#FFFDD0"),
        Pair("عاجي ملكي", "#FFFFF0"),
        Pair("سيبيا مريح", "#FBF0D9"),
        Pair("نعناعي هادئ", "#F0FFF4"),
        Pair("رمادي مهدئ", "#F8F9FA"),
        Pair("وردي ناعم", "#FFF5F5"),
        Pair("أبيض ناصع", "#FFFFFF")
    )

    val numberingStyles = listOf(
        "عربي تقليدي (١، ٢، ٣)",
        "أرقام قياسية (1, 2, 3)",
        "أرقام رومانية (I, II, III)",
        "ترقيم وصفي (صفحة X من Y)",
        "ترقيم أدبي فاخر (ـ ١ ـ)",
        "زخرفي في التذييل (✦ ١ ✦)"
    )

    val fontsList = listOf(
        "Cairo (الافتراضي)",
        "Tajawal",
        "Noto Naskh Arabic",
        "Almarai",
        "IBM Plex Sans Arabic",
        "Amiri",
        "Roboto"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "إنشاء كتاب أو مشروع جديد (الخيارات الـ 9)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textColor
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }

                    Text(
                        text = when (step) {
                            1 -> "الخطوة 1/3: اختيار نوع المستند من الـ 9 خيارات المحددة ومعاينة النموذج"
                            2 -> "الخطوة 2/3: إعدادات الصفحات، الترقيم، الألوان الهادئة، والغلاف"
                            else -> "الخطوة 3/3: عنوان المشروع، البصمة العاطفية، ونوع الخط"
                        },
                        fontSize = 11.sp,
                        color = theme.primaryColor,
                        fontWeight = FontWeight.Bold
                    )
                    Divider(color = Color.Black.copy(alpha = 0.08f), modifier = Modifier.padding(top = 4.dp))
                }

                // Step Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    when (step) {
                        1 -> {
                            // Step 1: Pick from the 9 specified types with wireframe preview
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("اختر نوع المشروع المطلوب إنشاؤه:", fontSize = 13.sp, fontWeight = FontWeight.Bold)

                                // List of 9 types
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    all9ProjectTypes.forEach { item ->
                                        val isChosen = selectedType.key == item.key
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedType = item
                                                    pageCount = item.defaultPages
                                                    paperColorHex = item.paperColorHex
                                                }
                                                .border(
                                                    width = if (isChosen) 2.dp else 1.dp,
                                                    color = if (isChosen) theme.primaryColor else Color.Black.copy(alpha = 0.06f),
                                                    shape = RoundedCornerShape(12.dp)
                                                ),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isChosen) theme.primaryColor.copy(alpha = 0.08f) else Color(0xFFFAFAFA)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isChosen) theme.primaryColor else Color.LightGray.copy(alpha = 0.4f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(item.icon, contentDescription = null, tint = if (isChosen) Color.White else Color.DarkGray, modifier = Modifier.size(20.dp))
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(item.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isChosen) theme.primaryColor else Color.DarkGray)
                                                    Text(item.description, fontSize = 10.sp, color = Color.Gray)
                                                }

                                                if (isChosen) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Wireframe / Layout Preview of Selected Paper
                                Text("معاينة شكل نموذج الورقة (${selectedType.name}):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                PaperVisualWireframe(
                                    layout = selectedType.previewLayout,
                                    bgColorHex = paperColorHex,
                                    theme = theme
                                )

                                // Option to upload custom template or model
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color(0xFF166534), modifier = Modifier.size(18.dp))
                                                Text("رفع نموذج أو صورة أولية لمحاكاتها للكتابة عليها", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                                            }
                                            Switch(
                                                checked = hasCustomCloneModel,
                                                onCheckedChange = {
                                                    hasCustomCloneModel = it
                                                    if (it) customCloneModelName = "نموذج_أولي_مستورد.pdf"
                                                },
                                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF166534))
                                            )
                                        }

                                        if (hasCustomCloneModel) {
                                            Text(
                                                text = "✓ تم تجهيز محاكاة النموذج المستورد: $customCloneModelName وسيقوم التطبيق بإنشاء نسخة متطابقة جاهزة للتحرير.",
                                                fontSize = 10.sp,
                                                color = Color(0xFF166534)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // Step 2: Page count control, numbering styles, calming colors, cover
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Page Count Controller (إضافة / حذف صفحات)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("التحكم في عدد صفحات الكتاب:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("يمكنك إضافة وحذف الصفحات في أي وقت", fontSize = 10.sp, color = Color.Gray)
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            IconButton(
                                                onClick = { if (pageCount > 1) pageCount-- },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "حذف صفحة", tint = Color.Red)
                                            }

                                            Text("$pageCount صفحة", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = theme.primaryColor)

                                            IconButton(
                                                onClick = { pageCount++ },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(Icons.Default.AddCircleOutline, contentDescription = "إضافة صفحة", tint = Color(0xFF16A34A))
                                            }
                                        }
                                    }
                                }

                                // Calming Paper Colors (ألوان هادئة ومريحة للعينين)
                                Text("تغيير لون الورقة لألوان هادئة ومريحة للعينين:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    calmingPaperColors.forEach { (name, hex) ->
                                        val isSelected = paperColorHex.equals(hex, ignoreCase = true)
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { paperColorHex = hex }
                                                .background(if (isSelected) theme.primaryColor.copy(alpha = 0.1f) else Color.Transparent)
                                                .padding(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                                    .border(
                                                        width = if (isSelected) 2.5.dp else 1.dp,
                                                        color = if (isSelected) theme.primaryColor else Color.Gray.copy(alpha = 0.3f),
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            Text(name, fontSize = 10.sp, color = if (isSelected) theme.primaryColor else Color.DarkGray)
                                        }
                                    }
                                }

                                // Professional Numbering Styles (طرق ترقيم احترافية مميزة متعددة)
                                Text("طرق ترقيم احترافية مميزة للاختيار بينها:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    numberingStyles.forEach { style ->
                                        val isChosen = numberingStyle == style
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { numberingStyle = style }
                                                .background(if (isChosen) theme.primaryColor.copy(alpha = 0.1f) else Color(0xFFF8FAFC))
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(style, fontSize = 11.sp, fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal, color = if (isChosen) theme.primaryColor else Color.DarkGray)
                                            if (isChosen) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                // Cover and Background
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("إضافة غلاف وخلفية غلاف للكتاب", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("إنشاء صفحة غلاف فنية تلقائية مع عنوان المؤلف", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Switch(
                                        checked = hasCoverImage,
                                        onCheckedChange = { hasCoverImage = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = theme.primaryColor)
                                    )
                                }
                            }
                        }

                        3 -> {
                            // Step 3: Title, Emotions, Font
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("عنوان المشروع أو الكتاب:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    label = { Text("أدخل عنوان الكتاب أو البحث") },
                                    placeholder = { Text("مثال: منهجية البحث العلمي المتقدم") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Text("اختر نمط الخط الطباعي:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    fontsList.forEach { fontName ->
                                        FilterChip(
                                            selected = selectedFont == fontName,
                                            onClick = { selectedFont = fontName },
                                            label = { Text(fontName, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Text("تحديد المشاعر والبصمة العاطفية الذكية:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier.height(160.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(availableEmotions.take(6)) { emo ->
                                        val isSelected = selectedEmotions.contains(emo.id)
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (isSelected) {
                                                        if (selectedEmotions.size > 1) selectedEmotions.remove(emo.id)
                                                    } else {
                                                        if (selectedEmotions.size < 5) selectedEmotions.add(emo.id)
                                                    }
                                                },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = if (isSelected) theme.primaryColor.copy(alpha = 0.15f) else Color(0xFFF9FAFB))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(emo.iconEmoji, fontSize = 16.sp)
                                                Text(emo.name, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Step Navigation Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 1) {
                        OutlinedButton(
                            onClick = { step-- },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("السابق")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (step < 3) {
                        Button(
                            onClick = { step++ },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("التالي")
                        }
                    } else {
                        Button(
                            onClick = {
                                if (title.isBlank()) {
                                    Toast.makeText(context, "يرجى كتابة عنوان المشروع أولاً", Toast.LENGTH_SHORT).show()
                                } else {
                                    onCreate(title, selectedType.key, selectedTemplateId, selectedEmotions.toList())
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("بدء الكتابة والتحرير", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaperVisualWireframe(
    layout: String,
    bgColorHex: String,
    theme: com.example.data.model.AppThemeData
) {
    val paperColor = try {
        Color(android.graphics.Color.parseColor(bgColorHex))
    } catch (e: Exception) {
        Color(0xFFFFFDD0)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = paperColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            when (layout) {
                "SLIDES" -> {
                    // Presentation slide layout
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Box(modifier = Modifier.size(60.dp, 8.dp).background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(2.dp)))
                            Box(modifier = Modifier.size(16.dp, 8.dp).background(theme.primaryColor, RoundedCornerShape(2.dp)))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f).height(50.dp).background(Color.Black.copy(alpha = 0.06f), RoundedCornerShape(6.dp)))
                            Box(modifier = Modifier.weight(1f).height(50.dp).background(Color.Black.copy(alpha = 0.06f), RoundedCornerShape(6.dp)))
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.LightGray.copy(alpha = 0.5f)))
                    }
                }
                "MAGAZINE" -> {
                    // Multi-column magazine layout
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.fillMaxWidth().height(36.dp).background(theme.primaryColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp)))
                            Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color.Gray.copy(alpha = 0.4f)))
                            Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color.Gray.copy(alpha = 0.4f)))
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color.Gray.copy(alpha = 0.4f)))
                            Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color.Gray.copy(alpha = 0.4f)))
                            Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color.Gray.copy(alpha = 0.4f)))
                            Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color.Gray.copy(alpha = 0.4f)))
                        }
                    }
                }
                "COMIC" -> {
                    // Comic panels layout
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().border(1.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.5f)))
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().border(1.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.5f)))
                        }
                        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().border(1.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.5f)))
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().border(1.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.5f)))
                        }
                    }
                }
                "BROCHURE" -> {
                    // 3-fold brochure
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.03f)))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.03f)))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.03f)))
                    }
                }
                else -> {
                    // Standard book / research / doc
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(100.dp, 8.dp).background(Color.DarkGray.copy(alpha = 0.6f), RoundedCornerShape(2.dp)))
                        Box(modifier = Modifier.fillMaxWidth().height(5.dp).background(Color.Gray.copy(alpha = 0.3f)))
                        Box(modifier = Modifier.fillMaxWidth().height(5.dp).background(Color.Gray.copy(alpha = 0.3f)))
                        Box(modifier = Modifier.fillMaxWidth().height(5.dp).background(Color.Gray.copy(alpha = 0.3f)))
                        Box(modifier = Modifier.fillMaxWidth().height(5.dp).background(Color.Gray.copy(alpha = 0.3f)))
                        Spacer(modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.align(Alignment.CenterHorizontally).size(20.dp, 6.dp).background(Color.Gray.copy(alpha = 0.4f)))
                    }
                }
            }
        }
    }
}
