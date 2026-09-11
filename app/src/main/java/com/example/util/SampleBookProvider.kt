package com.example.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import com.example.data.local.entity.BookEntity
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object SampleBookProvider {
    private const val TAG = "SampleBookProvider"

    /**
     * Generates a sample PDF guide and a sample EPUB book in the app's internal files
     * directory so the user can immediately experience the reader.
     */
    fun createSampleBooks(context: Context): List<BookEntity> {
        val books = mutableListOf<BookEntity>()

        try {
            val sampleDir = File(context.filesDir, "sample_books").apply { mkdirs() }

            // 1. Sample PDF: Reader Kullanıcı Rehberi
            val pdfFile = File(sampleDir, "Reader_Kullanici_Rehberi.pdf")
            if (!pdfFile.exists() || pdfFile.length() == 0L) {
                generateSamplePdf(pdfFile)
            }

            if (pdfFile.exists()) {
                val pdfUri = Uri.fromFile(pdfFile)
                val bookId = pdfUri.toString()
                val meta = PdfHelper.extractMetaAndCover(context, pdfUri, bookId)

                books.add(
                    BookEntity(
                        id = bookId,
                        uriString = pdfUri.toString(),
                        title = "Reader Kullanıcı Rehberi",
                        author = "Reader Ekibi",
                        format = "PDF",
                        fileSize = pdfFile.length(),
                        coverPath = meta.coverPath,
                        currentPage = 0,
                        totalPages = meta.totalPages.coerceAtLeast(3),
                        lastReadProgress = 0f,
                        lastReadTimestamp = 0L
                    )
                )
            }

            // 2. Sample EPUB: Küçük Prens (Seçme Bölümler)
            val epubFile = File(sampleDir, "Kucuk_Prens_Secmeler.epub")
            if (!epubFile.exists() || epubFile.length() == 0L) {
                generateSampleEpub(epubFile)
            }

            if (epubFile.exists()) {
                val epubUri = Uri.fromFile(epubFile)
                val bookId = epubUri.toString()
                val meta = EpubHelper.extractMetaAndCover(context, epubUri, bookId)

                books.add(
                    BookEntity(
                        id = bookId,
                        uriString = epubUri.toString(),
                        title = meta.title ?: "Küçük Prens (Seçmeler)",
                        author = meta.author ?: "Antoine de Saint-Exupéry",
                        format = "EPUB",
                        fileSize = epubFile.length(),
                        coverPath = meta.coverPath,
                        currentPage = 0,
                        totalPages = meta.totalChapters.coerceAtLeast(3),
                        lastReadProgress = 0f,
                        lastReadTimestamp = 0L
                    )
                )
            }

            // 3. Sample PDF: Modern E-Kitap Okuma Sanatı
            val pdfFile2 = File(sampleDir, "the_art_of_reading_books.pdf")
            if (!pdfFile2.exists() || pdfFile2.length() == 0L) {
                generateSecondSamplePdf(pdfFile2)
            }

            if (pdfFile2.exists()) {
                val pdfUri2 = Uri.fromFile(pdfFile2)
                val bookId2 = pdfUri2.toString()
                val meta2 = PdfHelper.extractMetaAndCover(context, pdfUri2, bookId2)

                books.add(
                    BookEntity(
                        id = bookId2,
                        uriString = pdfUri2.toString(),
                        title = "The Art of Reading Books",
                        author = "Mortimer J. Adler",
                        format = "PDF",
                        fileSize = pdfFile2.length(),
                        coverPath = meta2.coverPath,
                        currentPage = 0,
                        totalPages = meta2.totalPages.coerceAtLeast(4),
                        lastReadProgress = 0f,
                        lastReadTimestamp = 0L
                    )
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error generating sample books", e)
        }

        return books
    }

    private fun generateSamplePdf(targetFile: File) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 points

        // Page 1: Cover & Intro
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        canvas.drawColor(Color.parseColor("#FDFDFD"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // Background card accent
        paint.color = Color.parseColor("#2C3E50")
        canvas.drawRect(Rect(0, 0, 595, 140), paint)

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText("READER UYGULAMASI", 50f, 75f, paint)

        paint.textSize = 14f
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#BDC3C7")
        canvas.drawText("Modern PDF & EPUB Kitap Okuyucu", 50f, 105f, paint)

        // Body
        paint.color = Color.parseColor("#2C3E50")
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("Hoş Geldiniz!", 50f, 200f, paint)

        paint.color = Color.parseColor("#4A4A4A")
        paint.textSize = 13f
        paint.isFakeBoldText = false
        val linesPage1 = listOf(
            "Reader, Android cihazınızdaki PDF ve EPUB kitapları için",
            "özenle hazırlanmış yerel ve sade bir okuma platformudur.",
            "",
            "Temel Özellikler:",
            "• Depolama Alanı Taraması: Cihazınızdaki PDF ve EPUB dosyalarını kolayca bulun.",
            "• Akıllı Kapak Önizlemesi: İlk sayfadan veya EPUB paketinden otomatik kapak oluşturur.",
            "• Kaldığınız Yerden Devam: Son okuma konumunuz otomatik olarak kaydedilir.",
            "• Tablet ve Telefon Uyumu: Ekran boyutuna göre otomatik genişleyen responsive grid.",
            "• Gece & Gündüz Modu: Gözünüzü yormayan yüksek kontrastlı modern tema."
        )
        var y = 235f
        for (line in linesPage1) {
            canvas.drawText(line, 50f, y, paint)
            y += 24f
        }

        // Footer
        paint.color = Color.GRAY
        paint.textSize = 11f
        canvas.drawText("Sayfa 1 / 3", 500f, 800f, paint)
        document.finishPage(page)

        // Page 2: PDF Features
        val pageInfo2 = PdfDocument.PageInfo.Builder(595, 842, 2).create()
        page = document.startPage(pageInfo2)
        canvas = page.canvas
        canvas.drawColor(Color.parseColor("#FDFDFD"))

        paint.color = Color.parseColor("#2C3E50")
        paint.textSize = 22f
        paint.isFakeBoldText = true
        canvas.drawText("Bölüm 1: Dosya ve Klasör Yönetimi", 50f, 80f, paint)

        paint.color = Color.parseColor("#555555")
        paint.textSize = 13f
        paint.isFakeBoldText = false
        val linesPage2 = listOf(
            "Uygulamamız Android'in modern Depolama Erişim Çerçevesi (SAF)",
            "altyapısını kullanır. Bu sayede cihazınızdan gereksiz geniş depolama",
            "izinleri istemeden yalnızca seçtiğiniz klasör veya dosyalarla çalışır.",
            "",
            "1. 'Klasör Tara' butonuna dokunarak e-kitaplarınızı tuttuğunuz klasörü seçin.",
            "2. 'Yenile' butonu ile yeni eklediğiniz kitapları kütüphaneye dahil edin.",
            "3. Kitap kartına dokunarak doğrudan okuma ekranını açın.",
            "",
            "Sanayi Devrimi Avrupa toplumunu değiştirdi.",
            "Kapital birikimi modern ekonominin temel taşlarından biridir.",
            "",
            "Gelecek Aşamalar:",
            "• Metin seçme, vurgulama (highlight) ve not alma",
            "• Tablet kalemi ile serbest el yazısı notları",
            "• Gemini AI destekli özet ve akıllı okuma asistanı"
        )
        y = 125f
        for (line in linesPage2) {
            canvas.drawText(line, 50f, y, paint)
            y += 24f
        }
        canvas.drawText("Sayfa 2 / 3", 500f, 800f, paint)
        document.finishPage(page)

        // Page 3: Tips & Notes
        val pageInfo3 = PdfDocument.PageInfo.Builder(595, 842, 3).create()
        page = document.startPage(pageInfo3)
        canvas = page.canvas
        canvas.drawColor(Color.parseColor("#FDFDFD"))

        paint.color = Color.parseColor("#2C3E50")
        paint.textSize = 22f
        paint.isFakeBoldText = true
        canvas.drawText("Bölüm 2: İpuçları ve Kısayollar", 50f, 80f, paint)

        paint.color = Color.parseColor("#555555")
        paint.textSize = 13f
        paint.isFakeBoldText = false
        val linesPage3 = listOf(
            "• Okuma sırasında alt kısımdaki hızlı sayfa çubuğunu kullanarak",
            "  istediğiniz sayfaya anında geçiş yapabilirsiniz.",
            "• Çift dokunarak veya kıstırma (pinch-to-zoom) ile sayfayı yakınlaştırabilirsiniz.",
            "• Kitaptan çıktığınızda okuduğunuz sayfa belleğe alınır.",
            "",
            "İyi okumalar dileriz!",
            "Reader Ekibi - 2026"
        )
        y = 125f
        for (line in linesPage3) {
            canvas.drawText(line, 50f, y, paint)
            y += 24f
        }
        canvas.drawText("Sayfa 3 / 3", 500f, 800f, paint)
        document.finishPage(page)

        FileOutputStream(targetFile).use { out ->
            document.writeTo(out)
        }
        document.close()
    }

    private fun generateSecondSamplePdf(targetFile: File) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()

        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawColor(Color.parseColor("#F8F9FA"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.parseColor("#8E44AD")
        canvas.drawRect(Rect(0, 0, 595, 160), paint)

        paint.color = Color.WHITE
        paint.textSize = 26f
        paint.isFakeBoldText = true
        canvas.drawText("THE ART OF READING BOOKS", 50f, 85f, paint)

        paint.textSize = 14f
        paint.color = Color.parseColor("#E8DAEF")
        canvas.drawText("Mortimer J. Adler & Charles Van Doren", 50f, 120f, paint)

        paint.color = Color.parseColor("#2C3E50")
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("Chapter 1: The Dimensions of Reading", 50f, 220f, paint)

        paint.color = Color.parseColor("#444444")
        paint.textSize = 13f
        paint.isFakeBoldText = false
        val lines = listOf(
            "Reading is a multi-layered activity. In its simplest form,",
            "it is the recognition of symbols and words. In its highest form,",
            "it is an active conversation between reader and author.",
            "",
            "Modern digital readers allow us to carry entire libraries in our pockets.",
            "With this application, you can navigate documents with zero friction,",
            "enjoy clean typography, and maintain your reading momentum effortlessly."
        )
        var y = 260f
        for (line in lines) {
            canvas.drawText(line, 50f, y, paint)
            y += 24f
        }

        paint.color = Color.GRAY
        paint.textSize = 11f
        canvas.drawText("Sayfa 1 / 1", 500f, 800f, paint)
        document.finishPage(page)

        FileOutputStream(targetFile).use { out ->
            document.writeTo(out)
        }
        document.close()
    }

    private fun generateSampleEpub(targetFile: File) {
        FileOutputStream(targetFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                // mimetype
                val mimeEntry = ZipEntry("mimetype")
                zos.putNextEntry(mimeEntry)
                zos.write("application/epub+zip".toByteArray())
                zos.closeEntry()

                // META-INF/container.xml
                zos.putNextEntry(ZipEntry("META-INF/container.xml"))
                val containerXml = """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>"""
                zos.write(containerXml.toByteArray())
                zos.closeEntry()

                // OEBPS/content.opf
                zos.putNextEntry(ZipEntry("OEBPS/content.opf"))
                val opf = """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="pub-id">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:identifier id="pub-id">urn:uuid:reader-sample-kucuk-prens</dc:identifier>
    <dc:title>Küçük Prens (Seçmeler)</dc:title>
    <dc:creator>Antoine de Saint-Exupéry</dc:creator>
    <dc:language>tr</dc:language>
  </metadata>
  <manifest>
    <item id="ch1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>
    <item id="ch2" href="chapter2.xhtml" media-type="application/xhtml+xml"/>
    <item id="ch3" href="chapter3.xhtml" media-type="application/xhtml+xml"/>
    <item id="ch4" href="chapter4.xhtml" media-type="application/xhtml+xml"/>
  </manifest>
  <spine>
    <itemref idref="ch1"/>
    <itemref idref="ch2"/>
    <itemref idref="ch3"/>
    <itemref idref="ch4"/>
  </spine>
</package>"""
                zos.write(opf.toByteArray())
                zos.closeEntry()

                // OEBPS/chapter1.xhtml
                zos.putNextEntry(ZipEntry("OEBPS/chapter1.xhtml"))
                val ch1 = """<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head><title>Bölüm 1 - Giriş</title></head>
<body>
  <h1>Bölüm 1: Çöldeki Karşılaşma</h1>
  <p>Altı yaşındayken, balta girmemiş ormanlar üzerine yazılmış bir kitapta muhteşem bir resim görmüştüm.</p>
  <p>Bir boa yılanı, bir avı bütün olarak yutuyordu. Kitapta şöyle deniyordu: <em>"Boa yılanları avlarını çiğnemeden bütün olarak yutarlar. Sonra da altı ay boyunca uyuyarak sindirirler."</em></p>
  <p>Büyükler hiçbir şeyi tek başlarına anlayamazlar; çocukların da onlara her şeyi tekrar tekrar açıklamaktan canları çıkar.</p>
</body>
</html>"""
                zos.write(ch1.toByteArray())
                zos.closeEntry()

                // OEBPS/chapter2.xhtml
                zos.putNextEntry(ZipEntry("OEBPS/chapter2.xhtml"))
                val ch2 = """<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head><title>Bölüm 2 - Küçük Prens ve Koyun</title></head>
<body>
  <h1>Bölüm 2: Lütfen Bana Bir Koyun Çiz!</h1>
  <p>Uçağımın motorunda bir arıza çıkmış ve Sahra Çölü'ne inmek zorunda kalmıştım. Yanımda ne bir tamirci ne de yolcu vardı.</p>
  <p>Gün doğarken tuhaf, incecik bir sesle uyandım: <strong>"Lütfen... bana bir koyun çiz!"</strong></p>
  <p>Şaşkınlıkla ayağa fırladım. Karşımda beni ciddiyetle inceleyen küçücük, olağanüstü bir çocuk duruyordu.</p>
</body>
</html>"""
                zos.write(ch2.toByteArray())
                zos.closeEntry()

                // OEBPS/chapter3.xhtml
                zos.putNextEntry(ZipEntry("OEBPS/chapter3.xhtml"))
                val ch3 = """<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head><title>Bölüm 3 - Gül ve Tilki</title></head>
<body>
  <h1>Bölüm 3: En Önemli Şey Gözle Görülmez</h1>
  <p>Tilki şöyle dedi: <em>"İşte sana bir sır, çok basit bir sır: İnsan ancak yüreğiyle baktığı zaman gerçeği görebilir. En önemli şey gözle görülmez."</em></p>
  <p>"Gülünü bu kadar değerli kılan, onun için harcadığın zamandır," diye ekledi tilki.</p>
  <p>Küçük Prens unutmamak için tekrarladı: "Gülüm için harcadığım zaman..."</p>
</body>
</html>"""
                zos.write(ch3.toByteArray())
                zos.closeEntry()

                // OEBPS/chapter4.xhtml
                zos.putNextEntry(ZipEntry("OEBPS/chapter4.xhtml"))
                val ch4 = """<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head><title>Bölüm 4 - Tarih ve Düşünce</title></head>
<body>
  <h1>Bölüm 4: Toplum ve Ekonomi Üzerine</h1>
  <p>Sanayi Devrimi Avrupa toplumunu değiştirdi.</p>
  <p>Kapital birikimi modern ekonominin temel taşlarından biridir.</p>
  <p>Tarih boyunca fikirler ve üretim biçimleri insanlığın geleceğini şekillendirmiştir.</p>
</body>
</html>"""
                zos.write(ch4.toByteArray())
                zos.closeEntry()
            }
        }
    }
}
