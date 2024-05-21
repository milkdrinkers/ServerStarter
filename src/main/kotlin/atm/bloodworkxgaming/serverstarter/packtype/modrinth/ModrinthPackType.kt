package atm.bloodworkxgaming.serverstarter.packtype.modrinth

import atm.bloodworkxgaming.serverstarter.InternetManager
import atm.bloodworkxgaming.serverstarter.ServerStarter.Companion.LOGGER
import atm.bloodworkxgaming.serverstarter.config.ConfigFile
import atm.bloodworkxgaming.serverstarter.packtype.AbstractZipbasedPackType
import atm.bloodworkxgaming.serverstarter.packtype.writeToFile
import com.google.gson.JsonParser
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.URISyntaxException
import java.nio.file.PathMatcher
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

open class ModrinthPackType(private val configFile: ConfigFile, internetManager: InternetManager) : AbstractZipbasedPackType(configFile, internetManager) {
    private var loaderVersion: String = configFile.install.loaderVersion
    private var mcVersion: String = configFile.install.mcVersion
    private val oldFiles = File(basePath + "OLD_TO_DELETE/")

    override fun cleanUrl(url: String): String {
        return url
    }

    override fun getLoaderVersion(): String {
        return loaderVersion
    }

    override fun getMCVersion(): String {
        return mcVersion
    }

    @Throws(IOException::class)
    override fun handleZip(file: File, pathMatchers: List<PathMatcher>) {
        // delete old installer folder
        FileUtils.deleteDirectory(oldFiles)

        // start with deleting the mods folder as it is not guaranteed to have override mods
        val modsFolder = File(basePath + "mods/")

        if (modsFolder.exists())
            FileUtils.moveDirectory(modsFolder, File(oldFiles, "mods"))
        LOGGER.info("Moved the mods folder")

        LOGGER.info("Starting to unzip files.")
        //unzip start
        try {
            ZipInputStream(FileInputStream(file)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry

                loop@ while (entry != null) {
                    LOGGER.info("Entry in mrpack: $entry", true)
                    val name = entry.name

                    // special manifest treatment
                    if (name == "modrinth.index.json")
                        zis.writeToFile(File(basePath + "modrinth.index.json"))

                    //overrides
                    if (name.startsWith("overrides/")) {
                        val path = entry.name.substring(10)

                        when {
                            pathMatchers.any { it.matches(Paths.get(path)) } ->
                                LOGGER.info("Skipping $path as it is on the ignore list.", true)


                            !name.endsWith("/") -> {
                                val outfile = File(basePath + path)
                                LOGGER.info("Copying entry to = $outfile", true)


                                outfile.parentFile?.mkdirs()

                                zis.writeToFile(outfile)
                            }

                            name != "overrides/" -> {
                                val newFolder = File(basePath + path)
                                if (newFolder.exists())
                                    FileUtils.moveDirectory(newFolder, File(oldFiles, path))

                                LOGGER.info("Folder moved:" + newFolder.absolutePath, true)
                            }
                        }
                    }

                    entry = zis.nextEntry
                }

                zis.closeEntry()
            }
        } catch (e: IOException) {
            LOGGER.error("Could not unzip files", e)
            throw e
        }

        LOGGER.info("Done unzipping the files.")
    }

    @Throws(IOException::class)
    override fun postProcessing() {
        val mods = ArrayList<ModEntryRaw>()
        ArrayList<String>()
        val file = File(basePath + "modrinth.index.json")

        if (!file.exists()) {
            LOGGER.error("No modrinth Manifest json found. Skipping mod downloads")
            return
        }

        InputStreamReader(FileInputStream(file), "utf-8").use { reader ->
            val json = JsonParser.parseReader(reader).asJsonObject
            LOGGER.info("manifest JSON Object: $json", true)
            val mcObj = json.getAsJsonObject("dependencies")

            if (mcVersion.isEmpty()) {
                mcVersion = mcObj.getAsJsonPrimitive("minecraft").asString
            }

            // gets the modrinth version
            if (loaderVersion.isEmpty()) {
                loaderVersion = mcObj.getAsJsonPrimitive(mcObj.keySet().last()).asString
            }

            // gets all the mods
            for (jsonElement in json.getAsJsonArray("files")) {
                val obj = jsonElement.asJsonObject
                // don't download things unsupported on the server
                if (obj.getAsJsonObject("env")?.getAsJsonPrimitive("server")?.asString.equals("unsupported")) {
                    continue
                } else {
                    val downloadUrl = obj.getAsJsonArray("downloads").get(0).asString
                    val projectID = extractProjectId(downloadUrl)
                    val fileID = extractFileId(downloadUrl)

                    mods.add(
                        ModEntryRaw(projectID, fileID, downloadUrl)
                    )
                }
            }
        }

        downloadMods(mods)
    }

    private fun extractProjectId(url: String): String {
        val projectIdRegex = "https://cdn.modrinth.com/data/([A-z0-9]*)/.*".toRegex()
        return projectIdRegex.replace(url, "$1")
    }

    private fun extractFileId(url: String): String {
        val fileIdRegex = "https://cdn.modrinth.com/data/[A-z0-9]*/versions/([A-z0-9]*)/.*".toRegex()
        return fileIdRegex.replace(url, "$1")
    }

    /**
     * Downloads the mods specified in the modrinth.index.json
     *
     * @param mods List of the mods from the json file
     */
    private fun downloadMods(mods: List<ModEntryRaw>) {
        val ignoreSet = HashSet<String>()
        val ignoreListTemp = configFile.install.getFormatSpecificSettingOrDefault<List<Any>>("ignoreProject", null)

        if (ignoreListTemp != null)
            for (o in ignoreListTemp) {
                if (o is String)
                    ignoreSet.add(o)

                if (o is Int)
                    ignoreSet.add(o.toString())
            }

        val ignoredMods = mods.filter { ignoreSet.contains(it.projectID) }
        val ignoredModsString = ignoredMods.joinToString(separator = "\n") { "\t${FilenameUtils.getName(it.downloadUrl)} (${it.projectID})" }
        LOGGER.info("Ignoring the following mods:\n $ignoredModsString")

        val modsToDownload = mods.filter { !ignoreSet.contains(it.projectID) }
        LOGGER.info("Mods to download: $modsToDownload", true)

        processMods(modsToDownload)
    }

    private fun processMods(mods: List<ModEntryRaw>) {
        // constructs the ignore list
        val ignorePatterns = java.util.ArrayList<Pattern>()
        for (ignoreFile in configFile.install.ignoreFiles) {
            if (ignoreFile.startsWith("mods/")) {
                ignorePatterns.add(Pattern.compile(ignoreFile.substring(ignoreFile.lastIndexOf('/'))))
            }
        }

        // downloads the mods
        val count = AtomicInteger(0)
        val totalCount = mods.size
        val fallbackList = ArrayList<ModEntryRaw>()

        mods.stream().parallel().forEach { s -> processSingleMod(s, count, totalCount, fallbackList, ignorePatterns) }

        val secondFail = ArrayList<ModEntryRaw>()
        fallbackList.forEach { s -> processSingleMod(s, count, totalCount, secondFail, ignorePatterns) }

        if (secondFail.isNotEmpty()) {
            LOGGER.warn("Failed to download (a) mod(s):")
            for (s in secondFail) {
                LOGGER.warn("\t" + s)
            }
        }
    }

    private fun processSingleMod(mod: ModEntryRaw, counter: AtomicInteger, totalCount: Int, fallbackList: MutableList<ModEntryRaw>, ignorePatterns: List<Pattern>) {
        try {
            val fileName = FilenameUtils.getName(mod.downloadUrl)
            for (ignorePattern in ignorePatterns) {
                if (ignorePattern.matcher(fileName).matches()) {
                    LOGGER.info("[" + counter.incrementAndGet() + "/" + totalCount + "] Skipped ignored mod: " + fileName)
                }
            }

            internetManager.downloadToFile(mod.downloadUrl, File(basePath + "mods/" + fileName))
            LOGGER.info("[" + String.format("% 3d", counter.incrementAndGet()) + "/" + totalCount + "] Downloaded mod: " + fileName)
        } catch (e: IOException) {
            LOGGER.error("Failed to download mod", e)
            fallbackList.add(mod)
        } catch (e: URISyntaxException) {
            LOGGER.error("Invalid url for $mod", e)
        }
    }
}

/**
 * Data class to keep projectID and downloadUrl together
 */
data class ModEntryRaw(val projectID: String, val fileID: String, val downloadUrl: String)