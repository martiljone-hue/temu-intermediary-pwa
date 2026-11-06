package com.example.ui.receipt

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.ProxyOrder
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReceiptExporter {

    private const val A4_WIDTH_PT = 595
    private const val A4_HEIGHT_PT = 842

    fun drawableResForImageKey(imageKey: String): Int {
        return when (imageKey.lowercase()) {
            "earbuds" -> R.drawable.img_prod_earbuds_1793928387586
            "lamp" -> R.drawable.img_prod_lamp_1793928387576
            else -> R.drawable.img_prod_blender_1793928386280
        }
    }

    /**
     * Generates a single-page A4 white-labeled PDF receipt (595x842 pt) with ZERO original supplier
     * branding (no Temu logos or original titles) and dynamic product photo card scaling (1, 2, or 3 cards).
     */
    fun exportAndShareWhiteLabelA4Pdf(
        context: Context,
        order: ProxyOrder,
        onError: (String) -> Unit
    ) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH_PT, A4_HEIGHT_PT, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            drawWhiteLabelReceiptOnCanvas(context, canvas, order)

            pdfDocument.finishPage(page)

            val receiptsDir = File(context.cacheDir, "receipts").apply { mkdirs() }
            val pdfFile = File(receiptsDir, "Mazagan_Receipt_${order.orderCode}.pdf")
            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "توصيل التسليم - مازاغان بروكسي الجديدة #${order.orderCode}"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, "مشاركة توصيل A4 PDF")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "تعذر إنشاء ملف PDF")
        }
    }

    private fun drawWhiteLabelReceiptOnCanvas(
        context: Context,
        canvas: Canvas,
        order: ProxyOrder
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FAF7F2") }
        canvas.drawRect(0f, 0f, A4_WIDTH_PT.toFloat(), A4_HEIGHT_PT.toFloat(), bgPaint)

        // Header Banner (White-label Mazagan Local Delivery — strictly NO Temu branding)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0F2922") }
        canvas.drawRoundRect(RectF(28f, 24f, 567f, 130f), 16f, 16f, headerPaint)

        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFB300")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val whiteTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val whiteSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D9EBE5")
            textSize = 12f
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("MAZAGAN EXPRESS • خدمة التوصيل المحلية بالجديدة", 545f, 58f, whiteTitlePaint)
        canvas.drawText("توصيل تسليم رسمي (White-Label Delivery Receipt)", 545f, 84f, goldPaint)
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(
            order.createdAt?.toDate() ?: Date()
        )
        canvas.drawText("رقم التوصيل: ${order.orderCode}   |   التاريخ: $dateStr", 545f, 110f, whiteSubPaint)

        // Customer Metadata Box
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2DACB")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val customerRect = RectF(28f, 146f, 567f, 250f)
        canvas.drawRoundRect(customerRect, 12f, 12f, cardPaint)
        canvas.drawRoundRect(customerRect, 12f, 12f, borderPaint)

        val darkBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#141D1A")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#33443F")
            textSize = 13f
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("معلومات الزبون والتسليم المحلي (El Jadida):", 545f, 172f, darkBoldPaint)
        canvas.drawText("الاسم الكامل: ${order.customerName}", 545f, 196f, bodyPaint)
        canvas.drawText("رقم الهاتف: ${order.customerPhone}", 545f, 218f, bodyPaint)
        canvas.drawText("عنوان التسليم: ${order.customerAddress}", 545f, 240f, bodyPaint)

        // Dynamic Image Scaling Section (1, 2, or 3 neat image cards on the same single A4 page)
        canvas.drawText("صور السلع المسلمة (توزيع تلقائي على صفحة A4 واحدة):", 545f, 278f, darkBoldPaint)

        val imageKeys = order.itemImageKeys.ifEmpty { listOf("blender") }.take(3)
        val cardCount = imageKeys.size
        val totalGalleryWidth = 539f // 567 - 28
        val gap = 14f
        val singleCardWidth = (totalGalleryWidth - gap * (cardCount - 1)) / cardCount
        val galleryTop = 290f
        val galleryHeight = if (cardCount == 1) 185f else if (cardCount == 2) 165f else 145f

        for (i in 0 until cardCount) {
            val left = 28f + i * (singleCardWidth + gap)
            val right = left + singleCardWidth
            val rect = RectF(left, galleryTop, right, galleryTop + galleryHeight)
            canvas.drawRoundRect(rect, 12f, 12f, cardPaint)
            canvas.drawRoundRect(rect, 12f, 12f, borderPaint)

            val resId = drawableResForImageKey(imageKeys[i])
            val bmp: Bitmap? = BitmapFactory.decodeResource(context.resources, resId)
            if (bmp != null) {
                val imgRect = RectF(left + 8f, galleryTop + 8f, right - 8f, galleryTop + galleryHeight - 30f)
                canvas.drawBitmap(bmp, null, imgRect, null)
            }
            val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F2922")
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val titleShort = order.itemTitlesDarija.getOrNull(i)?.take(28) ?: "منتج #${i + 1}"
            canvas.drawText(titleShort, (left + right) / 2f, galleryTop + galleryHeight - 10f, captionPaint)
        }

        // Line Items Table
        val tableTop = galleryTop + galleryHeight + 24f
        canvas.drawText("تفاصيل الطلبية والأثمنة بالدرهم المغربي:", 545f, tableTop, darkBoldPaint)

        var currentY = tableTop + 16f
        val titles = order.itemTitlesDarija
        val prices = order.itemPricesMad
        for (i in titles.indices) {
            val rowRect = RectF(28f, currentY, 567f, currentY + 34f)
            canvas.drawRoundRect(rowRect, 8f, 8f, cardPaint)
            canvas.drawRoundRect(rowRect, 8f, 8f, borderPaint)

            val itemTitle = titles[i].take(58)
            val itemPrice = prices.getOrNull(i) ?: 0L
            canvas.drawText("${i + 1}. $itemTitle", 548f, currentY + 22f, bodyPaint)

            val pricePaint = Paint(darkBoldPaint).apply { textAlign = Paint.Align.LEFT }
            canvas.drawText("$itemPrice MAD", 42f, currentY + 22f, pricePaint)
            currentY += 40f
        }

        // Pricing Transparency Footer Box
        val footerTop = 690f
        val footerRect = RectF(28f, footerTop, 567f, 795f)
        val footerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F3EFE6") }
        canvas.drawRoundRect(footerRect, 14f, 14f, footerBg)
        canvas.drawRoundRect(footerRect, 14f, 14f, borderPaint)

        canvas.drawText("مجموع السلع الأصلي (0% زيادة): ${order.subtotalMad} درهم", 545f, footerTop + 30f, bodyPaint)
        canvas.drawText("عمولة الخدمة والتوصيل المحلي فالجديدة: +${order.serviceFeeMad} درهم", 545f, footerTop + 54f, bodyPaint)

        val totalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E65100")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("المجموع النهائي المؤدى عند الاستلام: ${order.totalMad} درهم (MAD)", 545f, footerTop + 84f, totalPaint)

        val signPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#53635E")
            textSize = 11f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "شكراً لثقتكم في خدمات التوصيل المحلية بمدينة الجديدة • جميع الحقوق محفوظة",
            A4_WIDTH_PT / 2f,
            822f,
            signPaint
        )
    }
}
