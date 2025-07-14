import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.Insets
import java.awt.event.ItemListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

interface ViewListener {
    fun onImageClick(img: BufferedImage, x: Int, y: Int)
    fun onChangeTileSize(t: Int)
    fun onSave(filename: String)
    fun onPull(left: Boolean, top: Boolean, right: Boolean, bottom: Boolean)

    fun onLoadSource(path: String);

    fun onLoadPalette(path: String)
}

class View(val listener: ViewListener) {
    val spritesheetPanel: JPanel = JPanel()
    val imageWrapper: JPanel = JPanel()

    val window = JFrame("CHR Spritesheet Builder")

    init {

        window.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

        val frame = JPanel()
        frame.layout = BoxLayout(frame, BoxLayout.PAGE_AXIS)

        val form = this.buildForm(DEFAULT_TILESIZE)

        frame.add(imageWrapper)
        frame.add(spritesheetPanel)
        frame.add(form)

        window.contentPane.add(frame)
        window.pack()
        window.isVisible = true
    }

    private fun buildForm(tileSize: Int): JPanel {
        val form = JPanel(GridBagLayout())
        form.border = EmptyBorder(15, 15, 15, 15)
        val gbc = GridBagConstraints()
        gbc.insets = Insets(5, 5, 5, 5)
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.WEST

        // === Source File Selector ===
        gbc.gridx = 0
        gbc.gridy = 0
        form.add(JLabel("Source File:"), gbc)

        val sourceField = JTextField(20)
        sourceField.isEditable = false
        gbc.gridx = 1
        gbc.gridy = 0
        form.add(sourceField, gbc)

        val sourceBrowseButton = JButton("Browse...")
        sourceBrowseButton.addActionListener {
            val chooser = JFileChooser()
            chooser.fileSelectionMode = JFileChooser.FILES_ONLY
            val result = chooser.showOpenDialog(form)
            if (result == JFileChooser.APPROVE_OPTION) {
                sourceField.text = chooser.selectedFile.absolutePath
                listener.onLoadSource(chooser.selectedFile.absolutePath)
            }
        }
        gbc.gridx = 2
        gbc.gridy = 0
        form.add(sourceBrowseButton, gbc)

        // === Palette File Selector ===
        gbc.gridx = 0
        gbc.gridy = 1
        form.add(JLabel("Palette File:"), gbc)

        val paletteField = JTextField(20)
        paletteField.isEditable = false
        gbc.gridx = 1
        gbc.gridy = 1
        form.add(paletteField, gbc)

        val paletteBrowseButton = JButton("Browse...")
        paletteBrowseButton.addActionListener {
            val chooser = JFileChooser()
            chooser.fileSelectionMode = JFileChooser.FILES_ONLY
            val result = chooser.showOpenDialog(form)
            if (result == JFileChooser.APPROVE_OPTION) {
                paletteField.text = chooser.selectedFile.absolutePath
                listener.onLoadPalette(chooser.selectedFile.absolutePath)
            }
        }
        gbc.gridx = 2
        gbc.gridy = 1
        form.add(paletteBrowseButton, gbc)

        // === Folder Selector ===
        gbc.gridx = 0
        gbc.gridy = 2
        form.add(JLabel("Output Folder:"), gbc)

        val folderField = JTextField(20)
        folderField.isEditable = false
        gbc.gridx = 1
        gbc.gridy = 2
        form.add(folderField, gbc)

        val folderBrowseButton = JButton("Browse...")
        folderBrowseButton.addActionListener {
            val chooser = JFileChooser()
            chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            val result = chooser.showOpenDialog(form)
            if (result == JFileChooser.APPROVE_OPTION) {
                folderField.text = chooser.selectedFile.absolutePath
            }
        }
        gbc.gridx = 2
        gbc.gridy = 2
        form.add(folderBrowseButton, gbc)

        // === Filename Field ===
        gbc.gridx = 0
        gbc.gridy = 3
        form.add(JLabel("Filename:"), gbc)

        val filenameField = JTextField(20)
        gbc.gridx = 1
        gbc.gridy = 3
        gbc.gridwidth = 2
        form.add(filenameField, gbc)
        gbc.gridwidth = 1

        // === Tilesize ===
        gbc.gridx = 0
        gbc.gridy = 4
        form.add(JLabel("Tilesize:"), gbc)

        val tilesizeField = JTextField(10)
        tilesizeField.text = tileSize.toString()
        gbc.gridx = 1
        gbc.gridy = 4
        gbc.gridwidth = 2
        form.add(tilesizeField, gbc)
        gbc.gridwidth = 1

        tilesizeField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = update()
            override fun removeUpdate(e: DocumentEvent?) = update()
            override fun changedUpdate(e: DocumentEvent?) {}
            private fun update() {
                try {
                    listener.onChangeTileSize(tilesizeField.text.toInt())
                } catch (e: NumberFormatException) {
                    println(e)
                }
            }
        })

        // === Crop Checkboxes ===
        val cropPanel = JPanel(GridLayout(2, 2, 5, 5))
        val cbLeft = JCheckBox("Left")
        val cbTop = JCheckBox("Top")
        val cbRight = JCheckBox("Right")
        val cbBottom = JCheckBox("Bottom")
        cropPanel.border = EmptyBorder(5, 0, 5, 0)
        cropPanel.add(cbLeft)
        cropPanel.add(cbTop)
        cropPanel.add(cbRight)
        cropPanel.add(cbBottom)

        gbc.gridx = 0
        gbc.gridy = 5
        gbc.gridwidth = 3
        form.add(cropPanel, gbc)

        val updateCrop = {
            listener.onPull(
                cbLeft.isSelected,
                cbTop.isSelected,
                cbRight.isSelected,
                cbBottom.isSelected
            )
        }

        val l = ItemListener { updateCrop() }
        cbLeft.addItemListener(l)
        cbTop.addItemListener(l)
        cbRight.addItemListener(l)
        cbBottom.addItemListener(l)

        // === Save Button ===
        val saveButton = JButton("Save")
        saveButton.addActionListener {
            val folder = folderField.text.trim()
            val filename = filenameField.text.trim()
            val fullPath = if (folder.isNotEmpty()) {
                File(folder, filename).absolutePath
            } else {
                filename
            }
            listener.onSave(fullPath)
        }
        gbc.gridx = 0
        gbc.gridy = 6
        gbc.gridwidth = 3
        gbc.anchor = GridBagConstraints.EAST
        form.add(saveButton, gbc)

        return form
    }

    fun updateImage(img: BufferedImage) {
        imageWrapper.removeAll()
        val dim = Dimension(img.width, img.height)
        imageWrapper.preferredSize = dim
        imageWrapper.minimumSize = dim
        imageWrapper.maximumSize = dim
        val image = object : JComponent() {
            override fun getPreferredSize(): Dimension {
                return Dimension(img.width, img.height)
            }
            override fun paintComponent(g: Graphics?) {
                super.paintComponent(g)
                g?.drawImage(img, 0, 0, null)
            }
        }

        image.addMouseListener(object: MouseListener {
            override fun mouseClicked(event: MouseEvent?) {
                val x = (event!!.x)
                val y = (event.y)
                listener.onImageClick(img, x, y)
            }

            override fun mousePressed(p0: MouseEvent?) {
            }

            override fun mouseReleased(p0: MouseEvent?) {
            }

            override fun mouseEntered(p0: MouseEvent?) {
            }

            override fun mouseExited(p0: MouseEvent?) {
            }
        })

        imageWrapper.add(image)
        imageWrapper.revalidate()
        imageWrapper.repaint()
        window.pack()
    }

    fun updateSpritesheet(spritesheet: List<BufferedImage>, tileSize: Int) {
        spritesheetPanel.removeAll()
        spritesheetPanel.layout = null
        val dim = Dimension(spritesheet.size * tileSize, tileSize)
        spritesheetPanel.preferredSize = dim
        spritesheetPanel.minimumSize = dim
        spritesheetPanel.maximumSize = dim
        spritesheetPanel.border = LineBorder(Color.RED)
        val panel = object : JPanel() {
            override fun getPreferredSize(): Dimension {
                return dim
            }
            override fun paintComponent(g: Graphics?) {
                super.paintComponent(g)
                for(i in 0..spritesheet.size - 1) {
                    val buffer = spritesheet[i]
                    g?.drawImage(buffer, i * tileSize, 0, null)
                }
            }
        }
        panel.setBounds(0, 0, dim.width, dim.height)
        spritesheetPanel.add(panel)
        spritesheetPanel.revalidate()
        spritesheetPanel.repaint()
    }
}

