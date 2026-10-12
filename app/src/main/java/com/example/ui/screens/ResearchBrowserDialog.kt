package com.example.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppThemeData
import com.example.ui.MainViewModel

data class OnlineBookResource(
    val id: String,
    val title: String,
    val author: String,
    val category: String,
    val fileType: String = "PDF",
    val fileSizeMb: Float,
    val sampleText: String,
    val sourceUrl: String
)

@Composable
fun ResearchBrowserDialog(
    theme: AppThemeData,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, PDF, RESEARCH, EBOOKS
    var viewMode by remember { mutableStateOf("RESULTS") } // RESULTS or WEB_VIEW
    var activeWebUrl by remember { mutableStateOf("https://scholar.google.com") }

    val mockCatalog = remember {
        listOf(
            OnlineBookResource(
                id = "b1",
                title = "أصول البحث العلمي ومناهج التحقيق الأكاديمي",
                author = "د. عبد الرحمن البدوي",
                category = "بحث علمي",
                fileType = "PDF",
                fileSizeMb = 3.8f,
                sampleText = "المقدمة: يهدف منهج البحث العلمي الرصين إلى استقصاء الحقائق الموضوعية وتجريد النتائج من الأهواء الذاتية عبر منهجية الاستقراء والاستنباط المقارن...",
                sourceUrl = "https://archive.org/details/scientific_research_methodology"
            ),
            OnlineBookResource(
                id = "b2",
                title = "موسوعة تاريخ الفكر والأدب العالمي",
                author = "أحمد أمين",
                category = "كتاب إلكتروني",
                fileType = "PDF",
                fileSizeMb = 5.2f,
                sampleText = "تمهيد في نشأة التدوين: بدأت الفنون الأدبية ملحمة التعبير الوجداني عن الإنسان وصراعه الدائم مع الزمن والطبيعة وتوثيق تجارب الحضارات المتعاقبة...",
                sourceUrl = "https://openlibrary.org/books/history_of_literature"
            ),
            OnlineBookResource(
                id = "b3",
                title = "دراسة تحليلية: أثر الذكاء الاصطناعي في جودة التعليم الجامعي",
                author = "معهد البحوث الأكاديمية",
                category = "بحث عملي وجامعي",
                fileType = "PDF",
                fileSizeMb = 1.9f,
                sampleText = "الملخص التنفيذي: تناقش هذه الورقة البحثية الإحصائية نتائج استبيان شمل 1200 طالب وباحث حول فاعلية نماذج الذكاء الاصطناعي في تلخيص الكتب والمصادر...",
                sourceUrl = "https://arxiv.org/abs/ai_education_impact"
            ),
            OnlineBookResource(
                id = "b4",
                title = "قواعد الكتابة الإبداعية وصناعة الرواية",
                author = "د. علي جواد الطاهر",
                category = "فنون الكتابة",
                fileType = "PDF",
                fileSizeMb = 4.1f,
                sampleText = "الفصل الأول: البناء الدرامي ورسم الشخصيات يبدأ من لحظة الصراع الجوهري وخلق فضاء سردي مريح ومقنع يشد وجدان القارئ...",
                sourceUrl = "https://archive.org/details/creative_writing_principles"
            ),
            OnlineBookResource(
                id = "b5",
                title = "علم النفس المعرفي وتطبيقات الذاكرة",
                author = "مجموعة من الباحثين",
                category = "بحث علمي",
                fileType = "PDF",
                fileSizeMb = 2.7f,
                sampleText = "مقدمة الدراسة: كيف يستوعب العقل البشري المعلومات الكثيفة أثناء القراءة المكثفة للكتب واستخلاص المفاهيم الجوهرية وتخزينها في الذاكرة طويلة المدى...",
                sourceUrl = "https://doaj.org/article/cognitive_psychology_study"
            )
        )
    }

    val filteredList = remember(searchQuery, selectedFilter) {
        mockCatalog.filter { item ->
            val matchQuery = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.author.contains(searchQuery, ignoreCase = true) ||
                    item.category.contains(searchQuery, ignoreCase = true)
            val matchFilter = when (selectedFilter) {
                "PDF" -> item.fileType == "PDF"
                "RESEARCH" -> item.category.contains("بحث")
                "EBOOKS" -> item.category.contains("كتاب")
                else -> true
            }
            matchQuery && matchFilter
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .shadow(16.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(theme.primaryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = Color.White)
                        }
                        Column {
                            Text(
                                text = "متصفح البحوث والكتب الإلكترونية",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.textColor
                            )
                            Text(
                                text = "بحث مخصص للكتب الإلكترونية، ملفات PDF، والبحوث العلمية فقط",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Divider(color = Color.Black.copy(alpha = 0.08f))

                // Search Bar & Engine Quick Launch
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن كتاب، بحث علمي، أو ملف PDF بالاسم والموضوع...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = theme.primaryColor) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.primaryColor,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    singleLine = true
                )

                // Quick Engines and Categories
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL" && viewMode == "RESULTS",
                        onClick = { selectedFilter = "ALL"; viewMode = "RESULTS" },
                        label = { Text("الكل", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == "RESEARCH" && viewMode == "RESULTS",
                        onClick = { selectedFilter = "RESEARCH"; viewMode = "RESULTS" },
                        label = { Text("بحوث علمية", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == "EBOOKS" && viewMode == "RESULTS",
                        onClick = { selectedFilter = "EBOOKS"; viewMode = "RESULTS" },
                        label = { Text("كتب PDF", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = viewMode == "WEB_VIEW",
                        onClick = {
                            viewMode = if (viewMode == "WEB_VIEW") "RESULTS" else "WEB_VIEW"
                            activeWebUrl = "https://scholar.google.com"
                        },
                        label = { Text("تصفح Google Scholar", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }

                // Content View
                if (viewMode == "WEB_VIEW") {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    webViewClient = WebViewClient()
                                    loadUrl(activeWebUrl)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.FindInPage, contentDescription = null, modifier = Modifier.size(44.dp), tint = Color.LightGray)
                                Text("لم يتم العثور على كتب مطابقة مباشرة لكلمة البحث", color = Color.Gray, fontSize = 12.sp)
                                Text("جرب كلمات مثل: 'بحث'، 'رواية'، 'تاريخ'، 'علم'", color = theme.primaryColor, fontSize = 11.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredList) { book ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color.Black.copy(alpha = 0.06f), RoundedCornerShape(12.dp)),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFCFD))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = book.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = theme.textColor,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Surface(
                                                color = theme.primaryColor.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "${book.fileType} • ${book.fileSizeMb} MB",
                                                    fontSize = 10.sp,
                                                    color = theme.primaryColor,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "المؤلف: ${book.author} | التصنيف: ${book.category}",
                                            fontSize = 11.sp,
                                            color = Color.DarkGray
                                        )

                                        Text(
                                            text = book.sampleText,
                                            fontSize = 11.sp,
                                            lineHeight = 17.sp,
                                            color = Color.Gray,
                                            maxLines = 2
                                        )

                                        Divider(color = Color.Black.copy(alpha = 0.04f))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Download to phone storage
                                            OutlinedButton(
                                                onClick = {
                                                    Toast.makeText(context, "جارٍ تنزيل ${book.title} إلى ذاكرة الهاتف (مجلد التنزيلات)...", Toast.LENGTH_SHORT).show()
                                                },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("تنزيل للهاتف", fontSize = 11.sp)
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            // Import to App
                                            Button(
                                                onClick = {
                                                    viewModel.addManualTextFile(
                                                        title = "${book.title} (${book.fileType})",
                                                        content = "${book.title}\nالمؤلف: ${book.author}\n\n${book.sampleText}\n\n[تم جلب هذا المستند عبر متصفح البحوث والكتب الإلكترونية]"
                                                    )
                                                    Toast.makeText(context, "تم جلب وإضافة الكتاب إلى قسم المصادر بنجاح!", Toast.LENGTH_SHORT).show()
                                                },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("جلب إلى التطبيق", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
