import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File
import java.io.IOException
import kotlin.math.floor
import kotlin.math.max


fun main() {
    var view: View? = null
    val model = Model(object: ModelListener {
        override fun onUpdate(model: Model) {
            if(model.getPalette() == null || model.getSource() == null) return
            val converted = convertValues(
                model.getPalette()!!,
                createIndexes(model.getPalette()!!, model.getTileSize(), model.getSource()!!)
            )
            view?.updateImage(converted)
            view?.updateSpritesheet(model.getSpritesheet(), model.getTileSize())
        }
    })
    view = View(object: ViewListener {
        override fun onImageClick(img: BufferedImage, x: Int, y: Int) {
            val sx = floor(x.toDouble() / model.getTileSize()).toInt()
            val sy = floor(y.toDouble() / model.getTileSize()).toInt()

            var sub = img.getSubimage(sx * model.getTileSize(), sy * model.getTileSize(), model.getTileSize(), model.getTileSize())

            var left = -1
            for(i in 0 until sub.width) {
                for(u in 0 until sub.height) {
                    val color = sub.getRGB(i, u) and 0xffffff
                    if(color != 0) {
                        left = i
                        break
                    }
                }
                if(left != -1) break;
            }

            var top = -1
            for(u in 0 until sub.height) {
                for(i in 0 until sub.width) {
                    val color = sub.getRGB(i, u) and 0xffffff
                    if(color != 0) {
                        top = u
                        break
                    }
                }
                if(top != -1) break;
            }

            var right = -1
            for(i in 0 until sub.width) {
                for(u in 0 until sub.height) {
                    val color = sub.getRGB(sub.width - 1 - i, u) and 0xffffff
                    if(color != 0) {
                        right = i
                        break
                    }
                }
                if(right != -1) break;
            }

            var bottom = -1
            for(u in 0 until sub.height) {
                for(i in 0 until sub.width) {
                    val color = sub.getRGB(i, sub.height - 1 - u) and 0xffffff
                    if(color != 0) {
                        bottom = u
                        break
                    }
                }
                if(bottom != -1) break;
            }

            sub = sub.getSubimage(max(0, left), max(0, top), max(0, model.getTileSize() - left - right), max(0, model.getTileSize() - top - bottom))

            var x = model.getTileSize() / 2 - sub.width / 2
            if(model.pullLeft && !model.pullRight) {
                x = 0
            } else if(!model.pullLeft && model.pullRight) {
                x = model.getTileSize() - sub.width
            }

            var y = model.getTileSize() / 2 - sub.height / 2
            if(model.pullTop && !model.pullBottom) {
                y = 0
            } else if(!model.pullTop && model.pullBottom) {
                y = model.getTileSize() - sub.height
            }

            val result = BufferedImage(model.getTileSize(), model.getTileSize(), BufferedImage.TYPE_INT_RGB)
            val g = result.createGraphics()
            g.drawImage(sub, x, y, null)
            g.dispose()

            model.setSpritesheet(model.getSpritesheet() + result)
        }

        override fun onChangeTileSize(t: Int) {
            model.setTileSize(t)
        }

        override fun onSave(filename: String) {
            if(model.getPalette() == null) return
            val image = merge(model.getSpritesheet(), model.getTileSize())
            val indexed = getIndexes(model.getPalette()!!, image)
            val chr = convertCHR(indexed as Array<Array<Int>>)
            val outputPng = File("$filename.png")
            val outputCHR = File("$filename.chr")
            ImageIO.write(image, "png", outputPng)
            outputCHR.writeBytes(chr)
        }

        override fun onPull(left: Boolean, top: Boolean, right: Boolean, bottom: Boolean) {
            model.updatePull(left, top, right, bottom)
        }

        override fun onLoadSource(path: String) {
            try {
                model.loadSource(path)
            } catch (ex: IOException) {
                println("${path} not found")
                ex.printStackTrace()
            }
        }

        override fun onLoadPalette(path: String) {
            try {
                model.loadPalette(path)
            } catch (ex: IOException) {
                println("${path} not found")
                ex.printStackTrace()
            }
        }
    })
}
