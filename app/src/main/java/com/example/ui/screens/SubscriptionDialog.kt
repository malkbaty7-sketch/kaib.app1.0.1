package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppThemeData
import com.example.ui.MainViewModel

@Composable
fun SubscriptionDialog(
    theme: AppThemeData,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedPlan by remember { mutableStateOf("YEARLY") } // "MONTHLY" or "YEARLY"
    val isVip by viewModel.isVipUser.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .shadow(16.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with VIP badge
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }

                Text("عضوية كاتب المميزة (Kateeb VIP)", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF1E293B))
                Text("إزالة كاملة للإعلانات، وميزات غير محدودة للتأليف والذكاء الاصطناعي والتصدير", fontSize = 11.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)

                // Feature checklist
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    VipFeatureItem("إزالة كافة الإعلانات البانر والتفاعلية نهائياً")
                    VipFeatureItem("استخلاص مواضيع غير محدود من عدد غير محدود من الكتب والـ PDFs")
                    VipFeatureItem("تصدير بجودة فائقة لـ PDF و Word و EPUB")
                    VipFeatureItem("الوصول الكامل إلى مكتبة النماذج التسعة والقوالب المتقدمة")
                }

                Divider(color = Color.LightGray.copy(alpha = 0.4f))

                // Plans Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Monthly Plan
                    PlanCard(
                        title = "اشتراك شهري",
                        price = "10 $",
                        period = "شهرياً",
                        isSelected = selectedPlan == "MONTHLY",
                        onSelect = { selectedPlan = "MONTHLY" },
                        modifier = Modifier.weight(1f)
                    )

                    // Yearly Plan (Best Value)
                    PlanCard(
                        title = "اشتراك سنوي",
                        price = "70 $",
                        period = "سنوياً (وفر 50$)",
                        badge = "الأفضل قيمة",
                        isSelected = selectedPlan == "YEARLY",
                        onSelect = { selectedPlan = "YEARLY" },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Action Button
                Button(
                    onClick = {
                        viewModel.setVipUser(true)
                        Toast.makeText(context, "تهانينا! تم تفعيل الاشتراك المميز بنجاح وإزالة كافة الإعلانات.", Toast.LENGTH_LONG).show()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Stars, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedPlan == "YEARLY") "ترقية الآن (70 دولار / سنة)" else "ترقية الآن (10 دولار / شهر)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(onClick = onDismiss) {
                    Text("المتابعة مع النسخة المجانية (مع إعلانات)", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun VipFeatureItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
        Text(text, fontSize = 11.sp, color = Color(0xFF334155))
    }
}

@Composable
fun PlanCard(
    title: String,
    price: String,
    period: String,
    badge: String? = null,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFFD97706) else Color.LightGray.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFFEF3C7).copy(alpha = 0.5f) else Color(0xFFF8FAFC)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (badge != null) {
                Surface(
                    color = Color(0xFFD97706),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(badge, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                }
            } else {
                Spacer(modifier = Modifier.height(14.dp))
            }

            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Text(price, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFFD97706))
            Text(period, fontSize = 10.sp, color = Color.Gray)
        }
    }
}
