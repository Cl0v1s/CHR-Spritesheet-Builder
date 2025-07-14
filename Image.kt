import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_RGB
import java.nio.ByteBuffer
import kotlin.math.round

/**
 * Merge multiple Buffered images into one
 */
fun merge(spritesheet: List<BufferedImage>, tileSize: Int): BufferedImage {
    val result = BufferedImage(tileSize, spritesheet.size * tileSize, TYPE_INT_RGB)
    val g = result.createGraphics()
    for(i in 0..spritesheet.size - 1) {
        val sub = spritesheet[i]
        g.drawImage(sub, 0, i * tileSize, null)
    }
    g.dispose()
    return result
}

/**
 * Create indexes for a given image according to a given palette
 * Transparent pixels are always given a value of 0
 * (This method modifies the image ! Use getIndexes if you want to retrieve indexes from an image without altering it)
 */
fun createIndexes(palette: Array<Int>, tileSize:Int, img: BufferedImage): Array<Array<Int?>> {
    val result = Array(img.width) { i -> arrayOfNulls<Int?>(img.height)}
    val tileWidth = img.width / tileSize
    val tileHeight = img.height / tileSize
    for(tileX in 0 until tileWidth) {
        for(tileY in 0 until tileHeight) {
            val map = mutableMapOf<Int, Int>()
            var colorIndex = 1
            for(i in 0 until tileSize) {
                for(u in 0 until tileSize) {
                    val x = tileX * tileSize + i
                    val y = tileY * tileSize + u
                    val color = img.getRGB(x, y)
                    val a = (color shr 24 and 0xff)
                    if(a < 255) {
                        result[x][y] = 0
                    } else {
                        val r = (color shr 16 and 0xff)
                        val g = (color shr 8 and 0xff)
                        val b = (color and 0xff)
                        val gray = round((maxOf(r,g,b) + minOf(r,g,b)).toDouble() / 2 / 10).toInt() * 10
                        if(!map.containsKey(gray)) {
                            map[gray] = colorIndex
                            colorIndex = ((colorIndex + 1) % (palette.size - 1)) + 1
                        }
                        result[x][y] = map[gray]
                    }
                }
            }
        }
    }
    return result
}

/**
 * For a given full color img, get the indexes according to a given palette
 */
fun getIndexes(palette: Array<Int>, img: BufferedImage): Array<Array<Int>> {
    val result = Array(img.width) { i -> arrayOfNulls<Int?>(img.height)}
    val map = HashMap<Int, Int>(palette.size)
    palette.forEachIndexed { index, i -> map[i] = index }
    for(i in 0 until img.width) {
        for(u in 0 until img.height) {
            val color = img.getRGB(i, u) and 0xffffff
            result[i][u] = map[color]
        }
    }
    return result as Array<Array<Int>>
}

/**
 * Create a full color img from an indexed image according to the given palette
 */
fun convertValues(palette: Array<Int>, img: Array<Array<Int?>>): BufferedImage {
    val width = img.size
    val height = img[0].size
    val result = BufferedImage(width, height, TYPE_INT_RGB)
    for(i in 0..width - 1) {
        for(u in 0..height -1) {
            val color = img[i][u]
            if(color != null) {
                result.setRGB(i, u, palette[color])
            } else {
                result.setRGB(i, u, palette[0])
            }
        }
    }
    return result
}

// thanks god this saved me https://bugzmanov.github.io/nes_ebook/chapter_6_3.html
/**
 * Convert an array of array of int into a CHR representation (NES version)
 */
fun convertCHR(
    pixels: Array<Array<Int>>,
): ByteArray {
    val tileWidth = pixels.size / 8
    val tileHeight = pixels[0].size / 8
    val result = ByteBuffer.allocate(tileWidth * tileHeight * 8 * 2)
    for(tileY in 0 until tileHeight) {
        for(tileX in 0 until tileWidth) {
            val plans1 = mutableListOf<Byte>()
            for(u in 0 until 8) {
                var plan0 = 0
                var plan1 = 0
                for(i in 0 until 8) {
                    val x = tileX * 8 + i
                    val y = tileY * 8 + u
                    var pixel = pixels[x][y]
                    if(pixel == -1) pixel = 0
                    pixel = pixel and 0b11
                    val p0 = pixel and 0b01
                    val p1 = (pixel and 0b10) shr 1
                    plan0 = plan0 or (p0 shl (7 - i))
                    plan1 = plan1 or (p1 shl (7 - i))
                }
                result.put(plan0.toByte())
                plans1.add(plan1.toByte())
            }
            for(plan1 in plans1) {
                result.put(plan1)
            }
        }
    }
    return result.array()
}

/**
 * Creates a palette from a given 4*1 pixel wide image
 */
fun buildPalette(paletteSource: BufferedImage): Array<Int> {
    val result = arrayOfNulls<Int?>(paletteSource.width)
    for(i in 0..paletteSource.width - 1) {
        result[i] = paletteSource.getRGB(i, 0) and 0xffffff; // we dont want the alpha channel
    }
    return result as Array<Int>
}