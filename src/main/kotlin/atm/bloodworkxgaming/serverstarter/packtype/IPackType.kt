package atm.bloodworkxgaming.serverstarter.packtype

import atm.bloodworkxgaming.serverstarter.InternetManager
import atm.bloodworkxgaming.serverstarter.config.ConfigFile
import atm.bloodworkxgaming.serverstarter.packtype.curse.CurseIDPackType
import atm.bloodworkxgaming.serverstarter.packtype.curse.CursePackType
import atm.bloodworkxgaming.serverstarter.packtype.modrinth.ModrinthPackType
import atm.bloodworkxgaming.serverstarter.packtype.zip.ZipFilePackType

interface IPackType {
    companion object {
        private val packtype = mutableMapOf<String, (ConfigFile, InternetManager) -> IPackType>(
            Pair("curse", ::CursePackType),
            Pair("curseforge", ::CursePackType),
            Pair("curseid", ::CurseIDPackType),
            Pair("modrinth", ::ModrinthPackType),
            Pair("zip", ::ZipFilePackType),
            Pair("zipfile", ::ZipFilePackType)
        )

        fun createPackType(packTypeName: String, configFile: ConfigFile, internetManager: InternetManager): IPackType? {
            return packtype[packTypeName]?.invoke(configFile, internetManager)
        }
    }

    /**
     * Downloads and installs the pack
     */
    fun installPack()

    /**
     * Gets the mod-loader version, can be based on the version from the downloaded pack
     *
     * @return String representation of the version
     */
    fun getLoaderVersion(): String

    /**
     * Gets the minecraft version, can be based on the version from the downloaded pack
     *
     * @return String representation of the version
     */
    fun getMCVersion(): String
}
