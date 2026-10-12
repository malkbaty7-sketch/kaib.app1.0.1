package com.example.util

import androidx.compose.ui.unit.LayoutDirection

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flag: String,
    val layoutDirection: LayoutDirection
) {
    ARABIC("ar", "العربية", "العربية", "🇸🇦", LayoutDirection.Rtl),
    ENGLISH("en", "English", "English", "🇺🇸", LayoutDirection.Ltr),
    SPANISH("es", "Spanish", "Español", "🇪🇸", LayoutDirection.Ltr),
    FRENCH("fr", "French", "Français", "🇫🇷", LayoutDirection.Ltr)
}

object AppStrings {
    fun get(key: String, language: AppLanguage): String {
        return when (language) {
            AppLanguage.ARABIC -> arabicStrings[key] ?: englishStrings[key] ?: key
            AppLanguage.ENGLISH -> englishStrings[key] ?: key
            AppLanguage.SPANISH -> spanishStrings[key] ?: englishStrings[key] ?: key
            AppLanguage.FRENCH -> frenchStrings[key] ?: englishStrings[key] ?: key
        }
    }

    private val arabicStrings = mapOf(
        "app_name" to "كاتب",
        "tab_files" to "المصادر",
        "tab_sources" to "المصادر",
        "tab_extraction" to "الاستخلاص",
        "tab_projects" to "المشاريع",
        "tab_books" to "الكتب المحفوظة",
        "tab_themes" to "الثيمات 3D",
        "tab_settings" to "الإعدادات",
        "settings_title" to "الإعدادات العامة",
        "language_selection" to "اختيار لغة التطبيق",
        "browser" to "متصفح البحوث والكتب الإلكترونية",
        "help" to "التعليمات والأسئلة (?)",
        "admin" to "لوحة الإدارة ومحرر الأكواد",
        "support" to "التواصل مع فريق الدعم",
        "login" to "تسجيل الدخول",
        "profile" to "الملف الشخصي",
        "subscribe" to "ترقية العضوية (Katib VIP)",
        "parental_control" to "الرقابة الأبوية",
        "new_project" to "إنشاء كتاب أو مشروع جديد",
        "copy" to "نسخ",
        "cut" to "قص",
        "paste" to "لصق",
        "delete" to "حذف",
        "share" to "مشاركة المشروع",
        "grammar_check" to "تنقيح وتصحيح النصوص",
        "save" to "حفظ",
        "offline_ready" to "يعمل دون إنترنت وموفر للبطارية",
        "analytics_title" to "إحصائيات وتحليلات الإنتاجية",
        "words_today" to "كلمات اليوم",
        "streak_days" to "أيام الالتزام المتتالية",
        "target_goal" to "الهدف اليومي",
        "typing_speed" to "سرعة الكتابة (كلمة/دقيقة)",
        "peak_hour" to "ساعة الذروة الإبداعية",
        "preflight_check" to "فحص التوافق والنشر العالمي (Pre-flight)",
        "paywall_title" to "انضم إلى نخبة الكُتّاب مع Katib VIP",
        "paywall_subtitle" to "ذكاء اصطناعي غير محدود، مزامنة سحابية، وتصدير لمعايير Amazon KDP بدون إعلانات",
        "monthly_plan" to "اشتراك شهري",
        "yearly_plan" to "اشتراك سنوي (وفر 35%)",
        "buy_ai_pack" to "حزمة توليد 50 صورة بالذكاء الاصطناعي",
        "restore_purchases" to "استعادة المشتريات",
        "corkboard_title" to "لوحة التخطيط والتنظيم المرئي"
    )

    private val englishStrings = mapOf(
        "app_name" to "Kateeb",
        "tab_files" to "Sources",
        "tab_sources" to "Sources",
        "tab_extraction" to "Extraction",
        "tab_projects" to "Projects",
        "tab_books" to "Saved Books",
        "tab_themes" to "3D Themes",
        "tab_settings" to "Settings",
        "settings_title" to "General Settings",
        "language_selection" to "Application Language",
        "browser" to "Research & PDF Browser",
        "help" to "Help & FAQs (?)",
        "admin" to "Admin Portal & Code Editor",
        "support" to "Contact Support",
        "login" to "Login / Register",
        "profile" to "My Profile",
        "subscribe" to "Go Premium (Katib VIP)",
        "parental_control" to "Parental Controls",
        "new_project" to "Create Book or Project",
        "copy" to "Copy",
        "cut" to "Cut",
        "paste" to "Paste",
        "delete" to "Delete",
        "share" to "Share Project",
        "grammar_check" to "Proofread & Refine",
        "save" to "Save",
        "offline_ready" to "Offline Ready & Battery Saver",
        "analytics_title" to "Writing & Productivity Analytics",
        "words_today" to "Words Today",
        "streak_days" to "Writing Streak Days",
        "target_goal" to "Daily Target Goal",
        "typing_speed" to "Typing Speed (WPM)",
        "peak_hour" to "Peak Creative Hour",
        "preflight_check" to "Global Pre-flight Compliance Check",
        "paywall_title" to "Join Elite Authors with Katib VIP",
        "paywall_subtitle" to "Unlimited AI, cloud sync, Amazon KDP export, and 100% ad-free",
        "monthly_plan" to "Monthly Subscription",
        "yearly_plan" to "Annual Plan (Save 35%)",
        "buy_ai_pack" to "Pack of 50 AI Imagen Illustrations",
        "restore_purchases" to "Restore Purchases",
        "corkboard_title" to "Digital Corkboard & Story Mapper"
    )

    private val spanishStrings = mapOf(
        "app_name" to "Kateeb",
        "tab_files" to "Fuentes",
        "tab_sources" to "Fuentes",
        "tab_extraction" to "Extracción",
        "tab_projects" to "Proyectos",
        "tab_books" to "Libros Guardados",
        "tab_themes" to "Temas 3D",
        "tab_settings" to "Ajustes",
        "settings_title" to "Ajustes Generales",
        "language_selection" to "Idioma de la Aplicación",
        "browser" to "Navegador de Investigación",
        "help" to "Ayuda y Preguntas (?)",
        "admin" to "Panel de Administración",
        "support" to "Soporte Técnico",
        "login" to "Iniciar Sesión",
        "profile" to "Mi Perfil",
        "subscribe" to "Hazte VIP (Sin Anuncios)",
        "parental_control" to "Control Parental",
        "new_project" to "Nuevo Libro o Proyecto",
        "copy" to "Copiar",
        "cut" to "Cortar",
        "paste" to "Pegar",
        "delete" to "Eliminar",
        "share" to "Compartir Proyecto",
        "grammar_check" to "Revisión y Estilo",
        "save" to "Guardar",
        "offline_ready" to "Listo sin conexión y ahorro de batería",
        "analytics_title" to "Estadísticas y Productividad de Escritura",
        "words_today" to "Palabras Hoy",
        "streak_days" to "Días Consecutivos de Racha",
        "target_goal" to "Meta Diaria",
        "typing_speed" to "Velocidad (Palabras/Min)",
        "peak_hour" to "Hora de Mayor Inspiración",
        "preflight_check" to "Validación Pre-publicación KDP",
        "paywall_title" to "Únete a los Autores Pro con Katib VIP",
        "paywall_subtitle" to "IA ilimitada, sincronización en la nube y exportación KDP sin publicidad",
        "monthly_plan" to "Plan Mensual",
        "yearly_plan" to "Plan Anual (Ahorra 35%)",
        "buy_ai_pack" to "Paquete de 50 Imágenes con IA",
        "restore_purchases" to "Restaurar Compras",
        "corkboard_title" to "Pizarra Digital y Mapeo de Historias"
    )

    private val frenchStrings = mapOf(
        "app_name" to "Kateeb",
        "tab_files" to "Sources",
        "tab_sources" to "Sources",
        "tab_extraction" to "Extraction",
        "tab_projects" to "Projets",
        "tab_books" to "Livres Enregistrés",
        "tab_themes" to "Thèmes 3D",
        "tab_settings" to "Paramètres",
        "settings_title" to "Paramètres Généraux",
        "language_selection" to "Langue de l'Application",
        "browser" to "Navigateur de Recherche",
        "help" to "Aide et FAQ (?)",
        "admin" to "Portail d'Administration",
        "support" to "Contacter le Support",
        "login" to "Connexion / Inscription",
        "profile" to "Mon Profil",
        "subscribe" to "Passer à Katib VIP",
        "parental_control" to "Contrôle Parental",
        "new_project" to "Créer un Livre ou Projet",
        "copy" to "Copier",
        "cut" to "Couper",
        "paste" to "Coller",
        "delete" to "Supprimer",
        "share" to "Partager le Projet",
        "grammar_check" to "Correction et Style",
        "save" to "Enregistrer",
        "offline_ready" to "Prêt hors-ligne et économie de batterie",
        "analytics_title" to "Analyses et Productivité d'Écriture",
        "words_today" to "Mots Aujourd'hui",
        "streak_days" to "Série de Jours Consécutifs",
        "target_goal" to "Objectif Quotidien",
        "typing_speed" to "Vitesse d'Écriture (Mots/Min)",
        "peak_hour" to "Heure de Pic Créatif",
        "preflight_check" to "Validation Pré-publication KDP",
        "paywall_title" to "Rejoignez l'Élite des Auteurs avec Katib VIP",
        "paywall_subtitle" to "IA illimitée, sauvegarde cloud et export certifié Amazon KDP sans pub",
        "monthly_plan" to "Abonnement Mensuel",
        "yearly_plan" to "Plan Annuel (Économisez 35%)",
        "buy_ai_pack" to "Pack de 50 Illustrations par IA",
        "restore_purchases" to "Restaurer les Achats",
        "corkboard_title" to "Tableau Numérique et Cartographie d'Intrigue"
    )
}

object AppLocalization {
    fun string(language: AppLanguage, key: String): String {
        return AppStrings.get(key, language)
    }
}
