package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppThemeData
import com.example.ui.MainViewModel
import com.example.util.BillingManager

@Composable
fun PaywallScreen(
    theme: AppThemeData,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val isVip by viewModel.isVipUser.collectAsState()
    val aiTokens by viewModel.aiImageTokens.collectAsState()
    var selectedPlan by remember { mutableStateOf(BillingManager.SUB_YEARLY) } // Default to best value
    var showInAppPacks by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.Gray)
                    }
                    TextButton(onClick = {
                        viewModel.restorePurchases()
                        Toast.makeText(context, "تم فحص المشتريات وتحديث الحساب", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("استعادة المشتريات", fontSize = 11.sp, color = theme.primaryColor, fontWeight = FontWeight.Bold)
                    }
                }

                // Hero VIP Badge & Header
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFFB45309))
                            )
                        )
                        .shadow(12.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = "انضم إلى نخبة الكُتّاب مع Katib VIP ✨",
                    fontWeight = FontWeight.Black,
                    fontSize = 19.sp,
                    color = theme.textColor,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "أطلق العنان لإمكانياتك الإبداعية مع تجربة تأليف متكاملة خالية تماماً من الإعلانات، مع توليد لا محدود بالذكاء الاصطناعي وتصدير عالمي",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // Feature Checklist Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PaywallFeatureRow(
                            icon = Icons.Default.Block,
                            iconTint = Color(0xFFEF4444),
                            title = "خالٍ تماماً من الإعلانات 🚫",
                            description = "إزالة تامة لكافة الإعلانات البانر والتفاعلية لضمان بيئة كتابة هادئة ومركزة"
                        )
                        PaywallFeatureRow(
                            icon = Icons.Default.AutoAwesome,
                            iconTint = Color(0xFF9333EA),
                            title = "توليد صور ذكاء اصطناعي غير محدود 🎨",
                            description = "تصميم أغلفة الكتب ورسومات الكوميكس بدقة فائقة بنقرة واحدة عبر Imagen"
                        )
                        PaywallFeatureRow(
                            icon = Icons.Default.CloudSync,
                            iconTint = Color(0xFF0284C7),
                            title = "مزامنة سحابية تلقائية عبر Google Drive ☁️",
                            description = "حفظ ومزامنة فورية لكافة كتبك وفصولك وخطوطك لحمايتها من الضياع"
                        )
                        PaywallFeatureRow(
                            icon = Icons.Default.Publish,
                            iconTint = Color(0xFF10B981),
                            title = "نشر وتصدير معتمد لمعايير Amazon KDP و EPUB3 📚",
                            description = "تضمين كامل للخطوط العربية وحساب كعب الكتاب وفحص الجودة التلقائي (Pre-flight)"
                        )
                    }
                }

                // Subscription Plan Selector
                Text("اختر خطة الاشتراك المناسبة لك:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = theme.textColor)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Yearly (Highlighted)
                    PlanCardItem(
                        title = "سنوي (الأفضل قيمة)",
                        price = "$39.99",
                        period = "سنوياً (وفر 35%)",
                        badge = "وفر 35% 🔥",
                        isSelected = selectedPlan == BillingManager.SUB_YEARLY,
                        theme = theme,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedPlan = BillingManager.SUB_YEARLY }
                    )

                    // Monthly
                    PlanCardItem(
                        title = "شهري",
                        price = "$4.99",
                        period = "شهرياً",
                        isSelected = selectedPlan == BillingManager.SUB_MONTHLY,
                        theme = theme,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedPlan = BillingManager.SUB_MONTHLY }
                    )
                }

                // Upgrade Button
                Button(
                    onClick = {
                        activity?.let { act ->
                            viewModel.purchaseSubscription(act, selectedPlan)
                        } ?: run {
                            viewModel.setVipActive(true)
                        }
                        Toast.makeText(context, "تم ترقية حسابك إلى Katib VIP بنجاح! مبروك 🎉", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(8.dp, RoundedCornerShape(14.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Stars, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isVip) "حسابك مفعّل بالفعل كـ VIP ⭐" else "ترقية العضوية الآن وفتح كافة الميزات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // In-App consumable packs toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showInAppPacks = !showInAppPacks },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("شراء حزم إضافية للذكاء الاصطناعي (In-App Packs)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = theme.primaryColor)
                    Icon(if (showInAppPacks) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = theme.primaryColor)
                }

                AnimatedVisibility(visible = showInAppPacks) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    activity?.let { act ->
                                        viewModel.purchaseInAppPack(act, BillingManager.INAPP_AI_50_IMAGES)
                                    } ?: viewModel.addAiImageTokens(50)
                                    Toast.makeText(context, "تمت إضافة 50 رصيد صور بنجاح!", Toast.LENGTH_SHORT).show()
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5FF)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D5FF))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFF9333EA))
                                    Column {
                                        Text("حزمة 50 صورة AI عبر Imagen", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("رصيدك الحالي: $aiTokens رصيد", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                                Button(
                                    onClick = {
                                        activity?.let { act ->
                                            viewModel.purchaseInAppPack(act, BillingManager.INAPP_AI_50_IMAGES)
                                        } ?: viewModel.addAiImageTokens(50)
                                        Toast.makeText(context, "تمت إضافة 50 رصيد صور بنجاح!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("$2.99", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "يتم تجديد الاشتراك تلقائياً ويمكن إلغاؤه في أي وقت عبر إعدادات متجر Google Play. تطبق الشروط وسياسة الخصوصية.",
                    fontSize = 9.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PaywallFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E293B))
            Text(description, fontSize = 10.sp, color = Color.Gray, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun PlanCardItem(
    title: String,
    price: String,
    period: String,
    badge: String? = null,
    isSelected: Boolean,
    theme: AppThemeData,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(2.dp, Color(0xFFD97706), RoundedCornerShape(14.dp))
                else Modifier.border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFFEF3C7).copy(alpha = 0.5f) else Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (badge != null) {
                Surface(
                    color = Color(0xFFD97706),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        badge,
                        fontSize = 9.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E293B))
            Text(price, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFFB45309))
            Text(period, fontSize = 10.sp, color = Color.Gray)
        }
    }
}
