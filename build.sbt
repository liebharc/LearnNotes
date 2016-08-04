androidBuild

javacOptions in Compile ++= "-source" :: "1.7" :: "-target" :: "1.7" :: Nil

platformTarget in Android := "android-21"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test"
libraryDependencies += "de.sciss" % "abc4j" % "0.6.0" // L-GPL, https://github.com/Sciss/abc4j
