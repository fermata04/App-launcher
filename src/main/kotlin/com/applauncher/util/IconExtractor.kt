package com.applauncher.util

import java.awt.image.BufferedImage
import java.io.File
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.filechooser.FileSystemView
import java.awt.Graphics2D
import java.awt.RenderingHints

object IconExtractor {
    
    fun getIconForFile(file: File): BufferedImage? {
        return try {
            val fileSystemView = FileSystemView.getFileSystemView()
            val icon: Icon? = fileSystemView.getSystemIcon(file)
            
            if (icon != null) {
                iconToBufferedImage(icon)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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
    
    fun resizeImage(image: BufferedImage, targetWidth: Int, targetHeight: Int): BufferedImage {
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
