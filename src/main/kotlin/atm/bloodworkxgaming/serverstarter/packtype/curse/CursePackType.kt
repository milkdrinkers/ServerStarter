package atm.bloodworkxgaming.serverstarter.packtype.curse

import atm.bloodworkxgaming.serverstarter.InternetManager
import atm.bloodworkxgaming.serverstarter.ServerStarter.Companion.LOGGER
import atm.bloodworkxgaming.serverstarter.config.ConfigFile
import atm.bloodworkxgaming.serverstarter.packtype.AbstractZipbasedPackType
import atm.bloodworkxgaming.serverstarter.packtype.writeToFile
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.URISyntaxException
import java.nio.file.PathMatcher
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.system.exitProcess

open class CursePackType(private val configFile: ConfigFile, internetManager: InternetManager) :
    AbstractZipbasedPackType(configFile, internetManager) {
    private var loaderVersion: String = configFile.install.loaderVersion
    private var mcVersion: String = configFile.install.mcVersion
    private val oldFiles = File(basePath + "OLD_TO_DELETE/")

    override fun cleanUrl(url: String): String {
        if (url.contains("curseforge.com") && !url.endsWith("/download"))
            return "$url/download"

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
        // unzip start
        try {
            ZipInputStream(FileInputStream(file)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry

                loop@ while (entry != null) {
                    LOGGER.info("Entry in zip: $entry", true)
                    val name = entry.name

                    // special manifest treatment
                    if (name == "manifest.json")
                        zis.writeToFile(File(basePath + "manifest.json"))


                    // overrides
                    if (name.startsWith("overrides/")) {
                        val path = entry.name.substring(10)

                        when {
                            pathMatchers.any { it.matches(Paths.get(path)) } ->
                                LOGGER.info("Skipping $path as it is on the ignore list.", true)


                            !name.endsWith("/") -> {
                                val outfile = File(basePath + path)
                                LOGGER.info("Copying zip entry to = $outfile", true)


                                outfile.parentFile?.mkdirs()

                                zis.writeToFile(outfile)
                            }

                            name != "overrides/" -> {
                                val newFolder = File(basePath + path)
                                if (newFolder.exists())
                                    FileUtils.moveDirectory(newFolder, File(oldFiles, path))

                                LOGGER.info("Folder moved: " + newFolder.absolutePath, true)
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
        var manifest = true
        var file = File(basePath + "manifest.json")
        if (!file.exists()) {
            file = File(basePath + "minecraftinstance.json")
            manifest = false
        }
        if (!file.exists()) {
            LOGGER.error("No Manifest or minecraftinstance json found. Skipping mod downloads")
            return
        }

        InputStreamReader(FileInputStream(file), "utf-8").use { reader ->
            val json = JsonParser.parseReader(reader).asJsonObject
            LOGGER.info("manifest JSON Object: $json", true)
            if (manifest) {
                val mcObj = json.getAsJsonObject("minecraft")

                if (mcVersion.isEmpty()) {
                    mcVersion = mcObj.getAsJsonPrimitive("version").asString
                }

                // gets the forge version
                if (loaderVersion.isEmpty()) {
                    val loaders = mcObj.getAsJsonArray("modLoaders")
                    if (loaders.size() > 0) {
                        loaderVersion = loaders[0].asJsonObject.getAsJsonPrimitive("id").asString.substring(6)
                    }
                }

                // gets all the mods
                for (jsonElement in json.getAsJsonArray("files")) {
                    val obj = jsonElement.asJsonObject
                    mods.add(ModEntryRaw(
                        obj.getAsJsonPrimitive("projectID").asString,
                        obj.getAsJsonPrimitive("fileID").asString,
                        obj.getAsJsonPrimitive("downloadUrl")?.asString ?: ""))
                }
            } else {
                val mcObj = json.getAsJsonObject("baseModLoader")
                if (mcVersion.isEmpty()) {
                    mcVersion = mcObj.getAsJsonPrimitive("minecraftVersion").asString
                }
                if (loaderVersion.isEmpty()) {
                    loaderVersion = mcObj.getAsJsonPrimitive("forgeVersion").asString
                }

                for (jsonElement in json.getAsJsonArray("installedAddons")) {
                    val obj = jsonElement.asJsonObject
                    val projectID = obj.getAsJsonPrimitive("addonID").asString
                    val fileID = obj.getAsJsonObject("installedFile").getAsJsonPrimitive("id").asString
                    val downloadUrl = obj.getAsJsonObject("installedFile").getAsJsonPrimitive("downloadUrl").asString

                    mods.add(ModEntryRaw(projectID, fileID, downloadUrl))
                }
            }

        }

        downloadMods(mods)
    }

    data class GetFilesResponseHashes(
        val value: String,
        val algo: Int
    )

    data class GetFilesResponseMod(
        val modId: Int,
        val fileName: String,
        val displayName: String,
        val downloadUrl: String?,
        val hashes: List<GetFilesResponseHashes>
    )


    data class GetFilesResponse(
        val data: List<GetFilesResponseMod>
    )

    private fun requestModInformation(mods: List<ModEntryRaw>, ignoreSet: HashSet<String>): GetFilesResponse {
        LOGGER.info("Requesting Download links from curse api.")

        data class GetModFilesRequestBody(val fileIds: List<String>)

        val fileList = GetModFilesRequestBody(mods.map { it.fileID }.toList())
        println(fileList)

        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val bodyJson = mapper.writeValueAsString(fileList)
        LOGGER.info("Request Body: $bodyJson", true)


        val url = "https://api.curseforge.com/v1/mods/files"
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("x-api-key", configFile.install.curseForgeApiKey)
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        val res = internetManager.httpClient.newCall(request).execute()

        if (!res.isSuccessful)
            throw IOException("Request to $url was not successful. Error Code: ${res.code}")
        val body = res.body ?: throw IOException("Request to $url returned a null body.")

        val str = body.string()
        LOGGER.info("Response Json from fileid query: ${str.length}", true)
        LOGGER.info("Response Json from fileid query: $str", true)

        val jsonRes = mapper.readValue<GetFilesResponse>(str)
        LOGGER.info("Converted Response from manifest query: $jsonRes", true)


        val ignoredMods = jsonRes.data.filter { ignoreSet.contains(it.modId.toString()) }
        val ignoredModsString = ignoredMods.joinToString(separator = "\n") { "\t${it.displayName} (${it.modId})" }
        LOGGER.info("Ignoring the following mods:\n $ignoredModsString")

        return GetFilesResponse(jsonRes.data.filter { !ignoreSet.contains(it.modId.toString()) })
    }

    /**
     * Downloads the mods specified in the manifest
     * Gets the data from cursemeta
     *
     * @param mods List of the mods from the manifest
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

        if (configFile.install.curseForgeApiKey.isEmpty()) {
            LOGGER.error("No API Key provided. Please see https://github.com/Ocraftyone/ServerStarter-CFCorePatch for details on how to obtain a key")
            exitProcess(1)
        }

        val urls = requestModInformation(mods, ignoreSet)

        LOGGER.info("Mods to download: $urls", true)

        processMods(urls)
    }

    /**
     * Downloads all mods, with a second fallback if failed
     * This is done in parallel for better performance
     *
     * @param response object with information from curse api
     */
    private fun processMods(response: GetFilesResponse) {
        val mods = response.data

        // constructs the ignore list
        val ignorePatterns = ArrayList<Pattern>()
        for (ignoreFile in configFile.install.ignoreFiles) {
            if (ignoreFile.startsWith("mods/")) {
                ignorePatterns.add(Pattern.compile(ignoreFile.substring(ignoreFile.lastIndexOf('/'))))
            }
        }

        // downloads the mods
        val count = AtomicInteger(0)
        val totalCount = mods.size
        val fallbackList = ArrayList<GetFilesResponseMod>()

        mods.stream().parallel().forEach { s -> processSingleMod(s, count, totalCount, fallbackList, ignorePatterns) }

        val secondFail = ArrayList<GetFilesResponseMod>()
        fallbackList.forEach { s -> processSingleMod(s, count, totalCount, secondFail, ignorePatterns) }

        if (secondFail.isNotEmpty()) {
            LOGGER.warn("Failed to download (a) mod(s):")
            for (s in secondFail) {
                LOGGER.warn("\t" + s)
            }
        }

        mods.filter { it.downloadUrl == null }.forEach {
            println("Downloading ${it.displayName} is prohibited, please download it on your own.")
        }
    }

    /**
     * Downloads a single mod and saves to the /mods directory
     *
     * @param mod            URL of the mod
     * @param counter        current counter of how many mods have already been downloaded
     * @param totalCount     total count of mods that have to be downloaded
     * @param fallbackList   List to write to when it failed
     * @param ignorePatterns Patterns of mods which should be ignored
     */
    private fun processSingleMod(mod: GetFilesResponseMod, counter: AtomicInteger, totalCount: Int, fallbackList: MutableList<GetFilesResponseMod>, ignorePatterns: List<Pattern>) {
        if (mod.downloadUrl == null)
            return

        try {
            val fileName = mod.fileName // FilenameUtils.getName(mod.downloadUrl)
            for (ignorePattern in ignorePatterns) {
                if (ignorePattern.matcher(fileName).matches()) {
                    LOGGER.info("[" + counter.incrementAndGet() + "/" + totalCount + "] Skipped ignored mod: " + mod.displayName)
                }
            }

            internetManager.downloadToFile(mod.downloadUrl, File(basePath + "mods/" + fileName))
            LOGGER.info("[" + String.format("% 3d", counter.incrementAndGet()) + "/" + totalCount + "] Downloaded mod: " + mod.displayName)
        } catch (e: IOException) {
            LOGGER.error("Failed to download mod", e)
            fallbackList.add(mod)
        } catch (e: URISyntaxException) {
            LOGGER.error("Invalid url for $mod", e)
        }
    }
}

/**
 * Data class to keep projectID and fileID together
 */
data class ModEntryRaw(val projectID: String, val fileID: String, val downloadUrl: String)
