/**
 * This script installs the GNCnn extension in QuPath.
 */
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

import qupath.lib.common.Version
import qupath.lib.gui.BuildInfo
import qupath.lib.gui.prefs.PathPrefs

def userPath = PathPrefs.userPathProperty().get()

if (userPath == null) {
    println('User path not set, setting it to the default value...')
    // The default user path is the user's home directory + '/QuPath' + 'vX.Y', where X.Y is the version number
    def version = BuildInfo.getInstance().getVersion();
    userPath = System.getProperty('user.home') + '/QuPath/v' + version.getMajor() + '.' + version.getMinor()
    new File(userPath).mkdirs()
    PathPrefs.userPathProperty().set(userPath)
    println('User path set to ' + userPath)
}

def extensionsDir = new File(userPath + '/extensions')

if (!extensionsDir.exists()) {
    println('Extension directory not found, creating it...')
    extensionsDir.mkdirs()
}

// Copy the JAR file to the extension directory
def jarFile = new File(this.args[0])

if (!jarFile.exists()) {
    throw new FileNotFoundException('GNCnn.jar not found')
} else {
    Files.copy(jarFile.toPath(), Paths.get(extensionsDir.getAbsolutePath() + '/GNCnn.jar'), StandardCopyOption.REPLACE_EXISTING)
}

println('GNCnn extension installed')