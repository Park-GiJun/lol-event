package net.gijun.collector.ui

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

/**
 * LoL 스타일 앱 아이콘 생성 유틸.
 * 금색 원형 프레임 + 어두운 배경 + 교차 검 모양.
 */
object AppIcon {

    private val GOLD = Color(0xC8, 0x9B, 0x3C)
    private val GOLD_LIGHT = Color(0xD4, 0xAF, 0x5A)
    private val DARK = Color(0x0A, 0x14, 0x28)
    private val DARK_MID = Color(0x0D, 0x1B, 0x2E)

    fun createBufferedImage(size: Int): BufferedImage {
        val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

        val pad = (size * 0.04).toInt()
        val d = size - pad * 2

        // 외부 금색 원
        g.color = GOLD
        g.fillOval(pad, pad, d, d)

        // 내부 어두운 원
        val innerPad = (size * 0.12).toInt()
        val innerD = size - innerPad * 2
        g.color = DARK
        g.fillOval(innerPad, innerPad, innerD, innerD)

        // 중앙 그라디언트 원
        val centerPad = (size * 0.2).toInt()
        val centerD = size - centerPad * 2
        g.color = DARK_MID
        g.fillOval(centerPad, centerPad, centerD, centerD)

        // 교차 검 (X 형태)
        val cx = size / 2.0
        val cy = size / 2.0
        val armLen = size * 0.22
        val strokeW = maxOf(2f, size * 0.06f)

        g.color = GOLD_LIGHT
        g.stroke = BasicStroke(strokeW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

        // 왼상 → 우하
        g.drawLine(
            (cx - armLen).toInt(), (cy - armLen).toInt(),
            (cx + armLen).toInt(), (cy + armLen).toInt(),
        )
        // 우상 → 좌하
        g.drawLine(
            (cx + armLen).toInt(), (cy - armLen).toInt(),
            (cx - armLen).toInt(), (cy + armLen).toInt(),
        )

        // 중심 보석
        val gemR = (size * 0.08).toInt()
        g.color = GOLD
        g.fillOval((cx - gemR).toInt(), (cy - gemR).toInt(), gemR * 2, gemR * 2)

        // 외곽 링 하이라이트
        g.color = Color(0xD4, 0xAF, 0x5A, 80)
        g.stroke = BasicStroke(maxOf(1f, size * 0.02f))
        g.drawOval(pad + 1, pad + 1, d - 2, d - 2)

        g.dispose()
        return img
    }

    fun createAwtImage(size: Int): java.awt.Image = createBufferedImage(size)

    fun createBitmapPainter(size: Int = 32): BitmapPainter {
        val img = createBufferedImage(size)
        val bmp = org.jetbrains.skia.Bitmap().also { b ->
            b.allocPixels(org.jetbrains.skia.ImageInfo.makeN32Premul(size, size))
            for (y in 0 until size) for (x in 0 until size) {
                b.erase(img.getRGB(x, y), org.jetbrains.skia.IRect.makeXYWH(x, y, 1, 1))
            }
        }
        return BitmapPainter(org.jetbrains.skia.Image.makeFromBitmap(bmp).toComposeImageBitmap())
    }

    /**
     * ICO 파일 생성 (16x16, 32x32, 48x48, 256x256).
     * Compose Desktop nativeDistributions에서 사용.
     */
    fun writeIcoFile(file: File) {
        val sizes = listOf(16, 32, 48, 256)
        val images = sizes.map { createBufferedImage(it) }
        val pngDataList = images.map { img ->
            ByteArrayOutputStream().also { ImageIO.write(img, "png", it) }.toByteArray()
        }

        file.outputStream().use { out ->
            // ICO header: reserved(2) + type(2) + count(2)
            out.write(byteArrayOf(0, 0, 1, 0, sizes.size.toByte(), 0))

            // 디렉토리 엔트리 (각 16바이트)
            var dataOffset = 6 + sizes.size * 16
            for (i in sizes.indices) {
                val s = sizes[i]
                val w = if (s >= 256) 0 else s
                val h = if (s >= 256) 0 else s
                val dataSize = pngDataList[i].size

                out.write(byteArrayOf(
                    w.toByte(), h.toByte(),     // width, height
                    0, 0,                        // color palette, reserved
                    1, 0,                        // color planes
                    32, 0,                       // bits per pixel
                ))
                // data size (4 bytes LE)
                out.write(intToLE(dataSize))
                // data offset (4 bytes LE)
                out.write(intToLE(dataOffset))

                dataOffset += dataSize
            }

            // PNG 데이터
            pngDataList.forEach { out.write(it) }
        }
    }

    private fun intToLE(v: Int): ByteArray = byteArrayOf(
        (v and 0xFF).toByte(),
        ((v shr 8) and 0xFF).toByte(),
        ((v shr 16) and 0xFF).toByte(),
        ((v shr 24) and 0xFF).toByte(),
    )
}
