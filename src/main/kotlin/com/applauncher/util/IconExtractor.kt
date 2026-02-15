package com.applauncher.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.swing.Icon
import javax.swing.filechooser.FileSystemView

object IconExtractor {

    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()
    private val loadingSemaphore = Semaphore(4)
    private val fileSystemView: FileSystemView by lazy { FileSystemView.getFileSystemView() }

    suspend fun getIconBitmap(path: String, size: Int = 32): ImageBitmap? {
        iconCache[path]?.let { return it }

        return loadingSemaphore.withPermit {
            // Double-check after acquiring permit
            iconCache[path]?.let { return@withPermit it }

            withContext(Dispatchers.IO) {
                try {
                    val file = File(path)
                    if (!file.exists()) return@withContext null

                    val icon: Icon = fileSystemView.getSystemIcon(file) ?: return@withContext null
                    val buffered = iconToBufferedImage(icon)
                    val resized = resizeImage(buffered, size, size)
                    val bitmap = resized.toComposeImageBitmap()
                    iconCache[path] = bitmap
                    bitmap
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    fun clearCache() {
        iconCache.clear()
    }

    fun evict(path: String) {
        iconCache.remove(path)
    }

    private fun iconToBufferedImage(icon: Icon): BufferedImage {
        val width = icon.iconWidth
        val height = icon.iconHeight
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2d: Graphics2D = image.createGraphics()

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

        icon.paintIcon(null, g2d, 0, 0)
        g2d.dispose()

        return image
    }

    private fun resizeImage(image: BufferedImage, targetWidth: Int, targetHeight: Int): BufferedImage {
        val resized = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB)
        val g2d = resized.createGraphics()

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        g2d.drawImage(image, 0, 0, targetWidth, targetHeight, null)
        g2d.dispose()

        return resized
    }
}
