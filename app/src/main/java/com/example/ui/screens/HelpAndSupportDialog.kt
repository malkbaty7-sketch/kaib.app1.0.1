package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppThemeData
import com.example.data.model.SupportTicket
import com.example.ui.MainViewModel

data class HelpFaqItem(
    val id: String,
    val question: String,
    val answer: String,
    val category: String
)

@Composable
fun HelpAndSupportDialog(
    theme: AppThemeData,
    viewModel: MainViewModel,
    initialTab: String = "FAQ", // "FAQ" or "SUPPORT"
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(initialTab) }
    var searchQuery by remember { mutableStateOf("") }

    // Support contact form states
    var supportSenderName by remember { mutableStateOf("") }
    var supportSenderContact by remember { mutableStateOf("") }
    var supportMessage by remember { mutableStateOf("") }

    val allFaqs = remember {
        listOf(
            HelpFaqItem(
                id = "1",
                question = "كيف أقوم باستخلاص المواضيع من كتب PDF متعددة؟",
                answer = "انتقل إلى تبويب 'المصادر' وقم برفع كتب PDF أو ملفات نصية، ثم انتقل إلى تبويب 'الاستخلاص'، واكتب الموضوع المراد استخلاصه واضغط 'استخلاص'. سيقوم التطبيق بمسح جميع الكتب واستخراج الفقرات المتعلقة بها وحفظها في مستودع نصوص مخصص يمكنك نسخه أو تعديله في أي وقت.",
                category = "الاستخلاص"
            ),
            HelpFaqItem(
                id = "2",
                question = "ما هي أنواع الكتب والمستندات التي يمكن إنشاؤها في التطبيق؟",
                answer = "يدعم التطبيق 9 نماذج متخصصة: 1- كتاب إلكتروني، 2- بحث علمي، 3- بحث عملي، 4- عرض تقديمي باوربوينت، 5- قصص مصورة للأطفال، 6- مجلة، 7- رواية، 8- مستند نصي وورد، 9- بروشورات مطوية، مع إمكانية رفع نموذج مخصص لمحاكاته.",
                category = "التأليف"
            ),
            HelpFaqItem(
                id = "3",
                question = "كيف أتحكم في عدد الصفحات وترقيمها ولون الورقة؟",
                answer = "داخل المحرر، اضغط على زر 'تخصيص الورقة' لتغيير لون الخلفية إلى ألوان هادئة ومريحة للعينين (كريمي، سيبيا، نعناعي، رمادي ناعم)، واختيار أسلوب ترقيم الصفحات (عربي، لاتيني، روماني، فاخر). يمكنك إضافة وحذف صفحات عبر زر (+) وتبويبات الفصول.",
                category = "المحرر"
            ),
            HelpFaqItem(
                id = "4",
                question = "كيف أستخدم 'متصفح البحوث والكتب الإلكترونية'؟",
                answer = "يتيح لك المتصفح المدمج البحث المتخصص عن كتب PDF والبحوث العلمية فقط عبر الإنترنت من مستودعات أكاديمية مجانية وتنزيلها لذاكرة الهاتف أو جلبها مباشرة إلى التطبيق لاستخلاص الأفكار منها.",
                category = "المتصفح"
            ),
            HelpFaqItem(
                id = "5",
                question = "هل يعمل التطبيق دون اتصال بالإنترنت (Offline)؟",
                answer = "نعم، جميع ميزات قراءة الملفات، الاستخلاص الذكي، تحرير النصوص، التنسيق، وتصدير PDF و DOCX تعمل محلياً بنسبة 100% دون الحاجة إلى إنترنت وبأعلى كفاءة لا تستهلك البطارية.",
                category = "النظام"
            ),
            HelpFaqItem(
                id = "6",
                question = "كيف تعمل ميزة الرقابة الأبوية؟",
                answer = "من الإعدادات، يمكنك تفعيل الرقابة الأبوية وتعيين رمز PIN مكون من 4 أرقام لمنع الأطفال من الوصول للمحتوى غير المخصص لهم أو تغيير الإعدادات.",
                category = "الأمان"
            )
        )
    }

    val filteredFaqs = remember(searchQuery) {
        if (searchQuery.isBlank()) allFaqs
        else allFaqs.filter {
            it.question.contains(searchQuery, ignoreCase = true) ||
            it.answer.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .shadow(16.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(theme.primaryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color.White)
                        }
                        Text("مركز المساعدة والتعليمات (?)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = theme.textColor)
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Tabs: FAQ / Support
                TabRow(
                    selectedTabIndex = if (currentTab == "FAQ") 0 else 1,
                    containerColor = Color(0xFFF8FAFC)
                ) {
                    Tab(
                        selected = currentTab == "FAQ",
                        onClick = { currentTab = "FAQ" },
                        text = { Text("دليل الميزات والأسئلة", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = currentTab == "SUPPORT",
                        onClick = { currentTab = "SUPPORT" },
                        text = { Text("التواصل مع فريق الدعم", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                if (currentTab == "FAQ") {
                    // Search in FAQs
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("اطرح أي سؤال أو ابحث عن ميزة في التطبيق...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = theme.primaryColor) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredFaqs) { faq ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(faq.question, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = theme.textColor)
                                        Surface(color = theme.primaryColor.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                                            Text(faq.category, fontSize = 9.sp, color = theme.primaryColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                    Text(faq.answer, fontSize = 11.sp, color = Color.DarkGray, lineHeight = 18.sp)
                                }
                            }
                        }
                    }
                } else {
                    // Contact Support Form
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("مراسلة فريق الدعم والمشرفين", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = theme.textColor)
                        Text("يسعدنا تلقي استفساراتك واقتراحاتك، وسيتم الرد عليك في أقرب وقت ممكن.", fontSize = 11.sp, color = Color.Gray)

                        OutlinedTextField(
                            value = supportSenderName,
                            onValueChange = { supportSenderName = it },
                            label = { Text("الاسم الكامل") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = supportSenderContact,
                            onValueChange = { supportSenderContact = it },
                            label = { Text("البريد الإلكتروني أو رقم الهاتف للرد عليك") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = supportMessage,
                            onValueChange = { supportMessage = it },
                            label = { Text("نص الرسالة أو الاستفسار أو المشكلة") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )

                        Button(
                            onClick = {
                                if (supportSenderName.isNotBlank() && supportMessage.isNotBlank()) {
                                    viewModel.submitSupportTicket(
                                        SupportTicket(
                                            senderName = supportSenderName,
                                            senderContact = supportSenderContact,
                                            message = supportMessage
                                        )
                                    )
                                    Toast.makeText(context, "تم إرسال رسالتك إلى فريق الدعم بنجاح!", Toast.LENGTH_LONG).show()
                                    supportMessage = ""
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "الرجاء كتابة الاسم والرسالة أولاً", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إرسال لفريق الدعم")
                        }
                    }
                }
            }
        }
    }
}
