package accesswidener

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import xyz.xenondevs.commons.gson.getAllStrings
import xyz.xenondevs.commons.gson.getArray
import xyz.xenondevs.commons.gson.getObject
import xyz.xenondevs.commons.gson.getString
import java.io.Reader

data class DevBundleInfo(
    val mappedServerCoordinates: String,
    val dependencies: List<String>,
) {
    
    companion object {
        
        fun fromJson(reader: Reader): DevBundleInfo {
            val json = JsonParser.parseReader(reader) as JsonObject
            val buildData = json.getObject("buildData")
            
            val dependencies = ArrayList<String>()
            dependencies.addAll(buildData.getArray("compileDependencies").getAllStrings())
            dependencies.add(json.getString("apiCoordinates"))
            
            return DevBundleInfo(json.getString("mappedServerCoordinates"), dependencies)
        }
        
    }
    
}