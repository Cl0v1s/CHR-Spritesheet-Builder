import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

interface ModelListener {
    fun onUpdate(model: Model);
}

class Model(val listener: ModelListener) {
    private var tileSize = DEFAULT_TILESIZE
    var pullTop = true
    var pullLeft = true
    var pullRight = true
    var pullBottom = true
    private var spritesheet = listOf<BufferedImage>()

    private var source: BufferedImage? = null
    private var palette: Array<Int>? = null

    fun loadSource(path: String) {
        source = ImageIO.read(File(path))
        listener.onUpdate(this)
    }

    fun loadPalette(path: String) {
        val p = ImageIO.read(File(path))
        palette = buildPalette(p)
        listener.onUpdate(this)
    }

    fun getSource(): BufferedImage? {
        return source
    }

    fun getPalette(): Array<Int>? {
        return palette
    }

    fun setSpritesheet(list: List<BufferedImage>) {
        spritesheet = list
        listener.onUpdate(this)
    }

    fun getSpritesheet(): List<BufferedImage> {
        return spritesheet
    }

    fun setTileSize(size: Int) {
        tileSize = size
        spritesheet = listOf<BufferedImage>()
        listener.onUpdate(this)
    }

    fun getTileSize(): Int {
        return tileSize
    }

    fun updatePull(left: Boolean, top: Boolean, right: Boolean, bottom: Boolean) {
        pullLeft = left
        pullTop = top
        pullBottom = bottom
        pullRight = right
    }
}