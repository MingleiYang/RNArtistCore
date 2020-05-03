package fr.unistra.rnartist.model

import fr.unistra.rnartist.model.io.parseBPSeq
import fr.unistra.rnartist.model.io.parseCT
import fr.unistra.rnartist.model.io.parseStockholm
import fr.unistra.rnartist.model.io.parseVienna
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.PrintWriter

fun main(args:Array<String>) {
    if (optionExists(args, "-h", "--help")) {
        printHelp()
    }
    else {
        RnartistConfig.loadConfig()
        val theme = Theme()
        if (optionExists(args, "-p", "--print")) {
            println("\n#### Current user-defined options to plot the 2D structures ####")
            for ((k,v) in theme.themeParams)
                println("- ${k}: ${v}")
            println("##################################################################\n")
        }
        else if (optionExists(args, "-f", "-id") && optionExists(args, "-o")) {

            getOptionValue(args, "-cA")?.let {
                if (!it.startsWith("#"))
                    theme.AColor = getAWTColor("#$it")!!
                else
                    theme.AColor = getAWTColor(it)!!
            }

            getOptionValue(args, "-cU")?.let {
                if (!it.startsWith("#"))
                    theme.UColor = getAWTColor("#$it")!!
                else
                    theme.UColor = getAWTColor(it)!!
            }

            getOptionValue(args, "-cG")?.let {
                if (!it.startsWith("#"))
                    theme.GColor = getAWTColor("#$it")!!
                else
                    theme.GColor = getAWTColor(it)!!
            }

            getOptionValue(args, "-cC")?.let {
                if (!it.startsWith("#"))
                    theme.CColor = getAWTColor("#$it")!!
                else
                    theme.CColor = getAWTColor(it)!!
            }

            getOptionValue(args, "-c2d")?.let {
                if (!it.startsWith("#"))
                    theme.SecondaryColor = getAWTColor("#$it")!!
                else
                    theme.SecondaryColor = getAWTColor(it)!!
            }

            getOptionValue(args, "-c3d")?.let {
                if (!it.startsWith("#"))
                    theme.TertiaryColor = getAWTColor("#$it")!!
                else
                    theme.TertiaryColor = getAWTColor(it)!!
            }

            getOptionValue(args, "--font")?.let {
                theme.fontName = it
            }

            getOptionValue(args, "-rb", "--residueBorder")?.let {
                theme.residueBorder = it.toInt()
            }

            getOptionValue(args, "-dxr", "--deltaXRes")?.let {
                theme.deltaXRes = it.toInt()
            }

            getOptionValue(args, "-dyr", "--deltaYRes")?.let {
                theme.deltaYRes = -it.toInt() //the inversion of sign is to have the y axis in the orientation people are used to see
            }

            getOptionValue(args,"-df", "--deltaFontSize")?.let {
                theme.deltaFontSize = it.toInt()
            }

            getOptionValue(args,"-w2d", "--width-2d")?.let {
                theme.secondaryInteractionWidth = it.toInt()
            }

            getOptionValue(args,"-w3d", "--width-3d")?.let {
                theme.tertiaryInteractionWidth = it.toInt()
            }

            getOptionValue(args,"-s3d", "--style-3d")?.let {
                theme.tertiaryInteractionStyle = if (it.equals("dashed")) DASHED else SOLID
            }

            getOptionValue(args,"-hw", "--halo-width")?.let {
                theme.haloWidth = it.toInt()
            }

            getOptionValue(args,"-o3d", "--opacity-3d")?.let {
                theme.tertiaryOpacity = it.toInt()
            }

            getOptionValue(args,"-f")?.let {
                val index = args.indexOf("-f") + 1
                val filePaths = mutableListOf<String>()
                for (i in index until args.size) {
                    if (args[i].startsWith("-"))
                        break
                    else {
                        val path = args[i].replaceFirst("~", System.getProperty("user.home"))
                        filePaths.add(File(path).getCanonicalPath())
                    }
                }

                filePaths.forEach { path ->
                    println("Processing ${path}")
                    when (path.split(".").last()) {
                        "bpseq" -> parseBPSeq(FileReader(File(path)))
                        "vienna" -> parseVienna(FileReader(File(path)))
                        "ct" -> parseCT(FileReader(File(path)))
                        else -> null
                    }?.let { ss ->
                        val drawing = SecondaryStructureDrawing(ss, theme = theme)

                        val outputPath = getOptionValue(args, "-o")?.let {
                            it.replaceFirst("~", System.getProperty("user.home"))
                        }

                        val tokens = path.split("/").last().split(".")
                        val writer = PrintWriter(File(File(outputPath).getAbsolutePath(), "${tokens.subList(0, tokens.size - 1).joinToString(separator = ".")}.svg"))
                        writer.write(drawing.asSVG(browserFix = optionExists(args,"--browser-fix")))
                        writer.close()
                    }
                }
            } ?: getOptionValue(args,"-id")?.let{ database_id ->
                val outputPath = getOptionValue(args, "-o")?.let {
                    it.replaceFirst("~", System.getProperty("user.home"))
                }
                when {
                    Regex("^RF.+").matches(database_id) -> {
                        for (ss in parseStockholm(Rfam().getEntry(database_id.trim()))) {
                            println("Processing ${ss.rna.name}")
                            var drawing = SecondaryStructureDrawing(secondaryStructure = ss)
                            var writer = FileWriter(File(File(outputPath).getAbsolutePath(),"${ss.rna.name.replace('/','_')}.svg"))
                            writer.write(drawing.asSVG(browserFix = optionExists(args,"--browser-fix")))
                            writer.close()
                        }
                    }
                }
            }
            if (optionExists(args,"-s", "--save")) {
                RnartistConfig.saveConfig(theme)
            }
        } else {
            printHelp()
        }
    }
}

fun optionExists(args:Array<String>, vararg optionName:String):Boolean {
    for (name in optionName) {
        for (arg in args)
            if (arg.startsWith(name))
                return true
    }
    return false
}

fun getOptionValue(args:Array<String>, vararg optionName:String):String? {
    for (name in optionName) {
        if (name.startsWith("--")) {
            for (arg in args) {
                val tokens = arg.split("=")
                if (name.equals(tokens.first()))
                    return tokens.last()
            }
        } else if (name in args){
            return args[args.indexOf(name) + 1]
        }
    }
    return null
}

fun printHelp() = println("""Usage: java -jar rnartistcore.jar [options]  [-f file_name] [-id database_id] -o directory
            
Description:
============
    RNArtistCore is a Java/Kotlin library and a commandline tool. As a tool, it exports an RNA secondary structure 
    in an SVG file. The secondary structure is computed from data stored in a local file or recovered from databases
    like Rfam using an ID. 
    The SVG plot can be configured through several options (lines width, font name,...). Using the option -s, these 
    user-defined values can be saved in a configuration file and become the default values for the next runs.
     
Mandatory Options:
==================
    -f file_name
        Either this option or -id is mandatory. The local file describing an RNA secondary structure. This option 
        accepts several file names. 
    
    -id database_entry_id
        Either this option or -f is mandatory. The database_entry_id has to conform to:
        RFXXXXX: an entry from the RFAM database (https://rfam.xfam.org/). A 2D structure is derived from the consensus 
                 one for each RNA member of the family and exported in the ouput directory as an SVG file
        
    -o dir_name
        The directory to output the SVG files. The directory has to exist.

Other Options:
==============
    
    --browser-fix 
        If you display your SVG files in a browser and observe some issues concerning the centering of residue characters, 
        try to add this option. If this doesn't fix the problem, you can improve the centering by yourself with the options 
        "dxr" and "dyr".
    
    -cA "HTML_color_code"
    -cU "HTML_color_code"
    -cG "HTML_color_code"
    -cC "HTML_color_code"
        These options define the color to use for each residue. The HTML code can be defined like "#99ccff", \#99ccff or 
        99ccff. You can find a list of HTML color codes here: https://www.w3schools.com/colors/colors_picker.asp
    
    -c2d "HTML_color_code"
    -c3d "HTML_color_code"
        These options define the color to use for the secondary (-c2d) or the tertiary (-c3d) interactions. The HTML code 
        can be defined like "#99ccff", \#99ccff or 99ccff. You can find a list of HTML color codes here: 
        https://www.w3schools.com/colors/colors_picker.asp
    
    -dxr number
    --deltaXRes=number
    -dyr number
    --deltaYRes=number
        These options translate the residue characters along the X- or Y-axis (for example, to move it by 5 pixels along 
        the X-axis, type "-dxr 5"). The X- and Y-axis are in the "classical" orientations (0,0 is the bottom left corner). 
        To push the characters on the left (X-axis) or to the bottom (Y-Axis), you need to use the two hypens syntax (like 
        "--deltaXRes=-5"). Once one of these two options user-defined, their value will be stored as the default one. To 
        erase them, make them equal to 0 (like "-dxr 0"). The number has to be a positive or negative integer.
        
    -df number
    --deltaFontSize=number
        Modifies the residue character size. To decrease it by 5 in size, you need to use the two hypens syntax (like 
        "--deltaFontSize=-5"). Once this option user-defined, its value will be stored as the default one. To erase it, 
        make it equal to 0 (like "-df 0"). The number has to be a positive or negative integer.  
        
    --font=font_name
        The name of the font to use. Check the fonts available for your system to make a choice.
        
    -h
    --help 
        Print this help message.
    
    -hw number
    --halo-width=number
        [NOT IMPLEMENTED, TO COME] Define the size of the halo around residues making tertiary interactions. The number 
        has to be an integer greater of equal to 0.
        
    -o3d number
    --opacity-3d=number
        [NOT IMPLEMENTED, TO COME] Define the % of opacity of the halo around residues making tertiary interactions. The 
        number has to be an integer between 0 and 100.
            
    -p
    --print
        Print the current user-defined options to plot the 2D structures. 
        
    -rb number
    --residueBorder=number
        Change the width for the border of the residues circles. The number has to be an integer greater of equal to 0.
        
    -s
    --save
        Save the options defined as default ones. Use option -p to print current default options.
        
    -s3d style
    --style-3d=style
        [NOT IMPLEMENTED, TO COME] Define the line style for the tertiary interactions. The value can be dashed or solid.
        
    -t theme_id
    --theme=theme_id
        [NOT IMPLEMENTED, TO COME] Use a theme shared by the community. Take allog at this page to see all theze themes shared.
    
    -w2d number
    --width-2d=number
    -w3d number
    --width-3d=number
    These options define the width for the secondary (-w2d) or the tertiary (-w3d) interactions lines. The number has to 
    be an integer greater of equal to 0.
        
Examples:
=========
    java -jar rnartistcore.jar -f ~/data/* -o ~/svg_files --font="Andale Mono" --browser-fix
    java -jar rnartistcore.jar -f ~/data/* -o ~/svg_files --font="DIN Condensed" -rb 2 -dxr 0 --deltaYRes=-5
    java -jar rnartistcore.jar -f ~/data/rna.bpseq -o ~/svg_files --font="Arial" -rb 0 --deltaFontSize=-10
    java -jar rnartistcore.jar -f ~/data/rna.bpseq -o ~/svg_files --font="Herculanum" -cU "#ffcc66" -s
    java -jar rnartistcore.jar -f ~/data/rna.bpseq -o ~/svg_files -w2d 10 -w3d 1 -s
    java -jar rnartistcore.jar -f ~/data/*.ct -o ~/svg_files -w2d 5 --font="Futura" --deltaFontSize=-2 -cA 0066ff -cG "#ff9900" -cU 009933 -cC \#cc00cc
        """.trimIndent())
